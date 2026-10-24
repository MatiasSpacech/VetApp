-- VeterApp - etapa 0
-- Ejecutar en el SQL Editor de Supabase (región São Paulo) o con `supabase db push`.

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.clinics (
  id uuid primary key default gen_random_uuid(),
  name text not null default 'Mi veterinaria',
  owner_id uuid not null references auth.users (id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.clinic_members (
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  role text not null default 'owner' check (role in ('owner', 'staff')),
  created_at timestamptz not null default now(),
  primary key (clinic_id, user_id)
);

create table if not exists public.pets (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  name text not null,
  species text not null,
  breed text,
  owner_name text not null,
  notes text,
  foto_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text
);

create table if not exists public.sync_operations (
  operation_id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  device_id text,
  entity text not null,
  applied_at timestamptz not null default now(),
  result jsonb
);

create table if not exists public.audit_log (
  id bigint generated always as identity primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  actor_id uuid,
  entity text not null,
  entity_id uuid,
  action text not null,
  payload jsonb,
  created_at timestamptz not null default now()
);

create index if not exists pets_clinic_updated_idx
  on public.pets (clinic_id, updated_at desc);
create index if not exists pets_pending_idx
  on public.pets (clinic_id, updated_at desc)
  where deleted_at is null;
create index if not exists clinic_members_user_idx
  on public.clinic_members (user_id);

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
  new.updated_at := now();
  new.revision := coalesce(old.revision, 0) + 1;
  return new;
end;
$$;

drop trigger if exists pets_touch_updated_at on public.pets;
create trigger pets_touch_updated_at
before update on public.pets
for each row execute function public.touch_updated_at();

create or replace function public.is_clinic_member(p_clinic_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select exists (
    select 1
    from public.clinic_members
    where clinic_id = p_clinic_id
      and user_id = auth.uid()
  );
$$;

revoke all on function public.is_clinic_member(uuid) from public;
grant execute on function public.is_clinic_member(uuid) to authenticated;

alter table public.clinics enable row level security;
alter table public.clinic_members enable row level security;
alter table public.pets enable row level security;
alter table public.sync_operations enable row level security;
alter table public.audit_log enable row level security;

drop policy if exists "clinics_select_members" on public.clinics;
create policy "clinics_select_members"
on public.clinics for select
to authenticated
using (public.is_clinic_member(id));

drop policy if exists "members_select_own" on public.clinic_members;
create policy "members_select_own"
on public.clinic_members for select
to authenticated
using (user_id = auth.uid() or public.is_clinic_member(clinic_id));

drop policy if exists "pets_select_members" on public.pets;
create policy "pets_select_members"
on public.pets for select
to authenticated
using (public.is_clinic_member(clinic_id));

drop policy if exists "audit_select_members" on public.audit_log;
create policy "audit_select_members"
on public.audit_log for select
to authenticated
using (public.is_clinic_member(clinic_id));

revoke insert, update, delete on public.clinics from anon, authenticated;
revoke insert, update, delete on public.clinic_members from anon, authenticated;
revoke insert, update, delete on public.pets from anon, authenticated;

grant select on public.clinics to authenticated;
grant select on public.clinic_members to authenticated;
grant select on public.pets to authenticated;
grant select on public.audit_log to authenticated;

create or replace function public.bootstrap_clinic()
returns table (clinic_id uuid)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_clinic uuid;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  select cm.clinic_id
    into v_clinic
  from public.clinic_members cm
  where cm.user_id = v_user
  limit 1;

  if v_clinic is null then
    insert into public.clinics (name, owner_id)
    values ('Mi veterinaria', v_user)
    returning id into v_clinic;

    insert into public.clinic_members (clinic_id, user_id, role)
    values (v_clinic, v_user, 'owner');
  end if;

  return query select v_clinic;
end;
$$;

revoke all on function public.bootstrap_clinic() from public;
grant execute on function public.bootstrap_clinic() to authenticated;

drop function if exists public.apply_pet_operation(uuid, uuid, text, jsonb);

create or replace function public.apply_pet_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_pet jsonb
)
returns setof public.pets
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_pet_id uuid;
  v_existing_clinic uuid;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  if not public.is_clinic_member(p_clinic_id) then
    raise exception 'not a clinic member';
  end if;

  if p_pet is null or coalesce(p_pet ->> 'id', '') = '' then
    raise exception 'invalid pet payload';
  end if;

  v_pet_id := (p_pet ->> 'id')::uuid;

  if coalesce(p_pet ->> 'name', '') = ''
     or coalesce(p_pet ->> 'species', '') = ''
     or coalesce(p_pet ->> 'owner_name', '') = '' then
    raise exception 'name, species and owner_name are required';
  end if;

  -- Idempotencia: un reintento de la misma operación no duplica ni cambia datos.
  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select p.*
      from public.pets p
      where p.id = v_pet_id and p.clinic_id = p_clinic_id;
    return;
  end if;

  select p.clinic_id into v_existing_clinic
  from public.pets p
  where p.id = v_pet_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'pet belongs to another clinic';
  end if;

  insert into public.pets (
    id, clinic_id, name, species, breed, owner_name, notes, foto_url,
    deleted_at, origin_device_id
  )
  values (
    v_pet_id,
    p_clinic_id,
    p_pet ->> 'name',
    p_pet ->> 'species',
    nullif(p_pet ->> 'breed', ''),
    p_pet ->> 'owner_name',
    nullif(p_pet ->> 'notes', ''),
    nullif(p_pet ->> 'foto_url', ''),
    nullif(p_pet ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_pet ->> 'origin_device_id')
  )
  on conflict (id) do update set
    name = excluded.name,
    species = excluded.species,
    breed = excluded.breed,
    owner_name = excluded.owner_name,
    notes = excluded.notes,
    foto_url = excluded.foto_url,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where pets.clinic_id = p_clinic_id;

  if not found then
    raise exception 'pet could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'pet')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id,
    v_user,
    'pet',
    v_pet_id,
    'upsert',
    jsonb_build_object('operation_id', p_operation_id, 'device_id', p_device_id)
  );

  return query
    select p.*
    from public.pets p
    where p.id = v_pet_id and p.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_pet_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_pet_operation(uuid, uuid, text, jsonb) to authenticated;

-- Bucket público para fotos. La lectura es pública; la escritura exige pertenencia.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('mascotas', 'mascotas', true, 1048576, array['image/webp', 'image/jpeg'])
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "mascotas_insert_members" on storage.objects;
create policy "mascotas_insert_members"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'mascotas'
  and array_length(storage.foldername(name), 1) >= 1
  and public.is_clinic_member(((storage.foldername(name))[1])::uuid)
);

drop policy if exists "mascotas_update_members" on storage.objects;
create policy "mascotas_update_members"
on storage.objects for update
to authenticated
using (
  bucket_id = 'mascotas'
  and array_length(storage.foldername(name), 1) >= 1
  and public.is_clinic_member(((storage.foldername(name))[1])::uuid)
)
with check (
  bucket_id = 'mascotas'
  and array_length(storage.foldername(name), 1) >= 1
  and public.is_clinic_member(((storage.foldername(name))[1])::uuid)
);

drop policy if exists "mascotas_delete_members" on storage.objects;
create policy "mascotas_delete_members"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'mascotas'
  and array_length(storage.foldername(name), 1) >= 1
  and public.is_clinic_member(((storage.foldername(name))[1])::uuid)
);
