-- VeterApp - etapa 2: clientes, estado de mascota e historial de propietario.
-- Ejecutar en el SQL Editor de Supabase o con `supabase db push`.

create table if not exists public.clients (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  name text not null,
  phone text,
  document text,
  email text,
  address text,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text
);

create index if not exists clients_clinic_updated_idx
  on public.clients (clinic_id, updated_at desc);
create index if not exists clients_clinic_name_idx
  on public.clients (clinic_id, name);
create index if not exists clients_pending_idx
  on public.clients (clinic_id, updated_at desc)
  where deleted_at is null;

alter table public.pets add column if not exists client_id uuid;
alter table public.pets add column if not exists status text not null default 'active';

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'pets_client_id_fkey'
  ) then
    alter table public.pets
      add constraint pets_client_id_fkey
      foreign key (client_id) references public.clients (id) on delete set null;
  end if;
end;
$$;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'pets_status_check'
  ) then
    alter table public.pets
      add constraint pets_status_check
      check (status in ('active', 'inactive', 'deceased'));
  end if;
end;
$$;

create index if not exists pets_client_idx on public.pets (client_id);

drop trigger if exists clients_touch_updated_at on public.clients;
create trigger clients_touch_updated_at
before update on public.clients
for each row execute function public.touch_updated_at();

alter table public.clients enable row level security;

drop policy if exists "clients_select_members" on public.clients;
create policy "clients_select_members"
on public.clients for select
to authenticated
using (public.is_clinic_member(clinic_id));

revoke insert, update, delete on public.clients from anon, authenticated;
grant select on public.clients to authenticated;

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
  v_client_id uuid;
  v_status text;
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

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select p.*
      from public.pets p
      where p.id = v_pet_id and p.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_pet ->> 'name', '') = ''
     or coalesce(p_pet ->> 'species', '') = ''
     or coalesce(p_pet ->> 'owner_name', '') = '' then
    raise exception 'name, species and owner_name are required';
  end if;

  v_status := coalesce(nullif(p_pet ->> 'status', ''), 'active');
  if v_status not in ('active', 'inactive', 'deceased') then
    raise exception 'invalid pet status';
  end if;

  if nullif(p_pet ->> 'client_id', '') is not null then
    v_client_id := (p_pet ->> 'client_id')::uuid;
    if not exists (
      select 1
      from public.clients c
      where c.id = v_client_id
        and c.clinic_id = p_clinic_id
        and c.deleted_at is null
    ) then
      raise exception 'client not found in clinic';
    end if;
  else
    v_client_id := null;
  end if;

  select p.clinic_id into v_existing_clinic
  from public.pets p
  where p.id = v_pet_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'pet belongs to another clinic';
  end if;

  insert into public.pets (
    id, clinic_id, client_id, name, species, breed, owner_name, notes, foto_url,
    status, deleted_at, origin_device_id
  )
  values (
    v_pet_id,
    p_clinic_id,
    v_client_id,
    p_pet ->> 'name',
    p_pet ->> 'species',
    nullif(p_pet ->> 'breed', ''),
    p_pet ->> 'owner_name',
    nullif(p_pet ->> 'notes', ''),
    nullif(p_pet ->> 'foto_url', ''),
    v_status,
    nullif(p_pet ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_pet ->> 'origin_device_id')
  )
  on conflict (id) do update set
    client_id = excluded.client_id,
    name = excluded.name,
    species = excluded.species,
    breed = excluded.breed,
    owner_name = excluded.owner_name,
    notes = excluded.notes,
    foto_url = excluded.foto_url,
    status = excluded.status,
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

create or replace function public.apply_client_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_client jsonb
)
returns setof public.clients
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_client_id uuid;
  v_existing_clinic uuid;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  if not public.is_clinic_member(p_clinic_id) then
    raise exception 'not a clinic member';
  end if;

  if p_client is null or coalesce(p_client ->> 'id', '') = '' then
    raise exception 'invalid client payload';
  end if;

  v_client_id := (p_client ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select c.*
      from public.clients c
      where c.id = v_client_id and c.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_client ->> 'name', '') = '' then
    raise exception 'name is required';
  end if;

  if nullif(p_client ->> 'deleted_at', '') is not null
     and exists (
       select 1
       from public.pets p
       where p.client_id = v_client_id
         and p.deleted_at is null
     ) then
    raise exception 'client has pets';
  end if;

  select c.clinic_id into v_existing_clinic
  from public.clients c
  where c.id = v_client_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'client belongs to another clinic';
  end if;

  insert into public.clients (
    id, clinic_id, name, phone, document, email, address, notes,
    deleted_at, origin_device_id
  )
  values (
    v_client_id,
    p_clinic_id,
    p_client ->> 'name',
    nullif(p_client ->> 'phone', ''),
    nullif(p_client ->> 'document', ''),
    nullif(p_client ->> 'email', ''),
    nullif(p_client ->> 'address', ''),
    nullif(p_client ->> 'notes', ''),
    nullif(p_client ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_client ->> 'origin_device_id')
  )
  on conflict (id) do update set
    name = excluded.name,
    phone = excluded.phone,
    document = excluded.document,
    email = excluded.email,
    address = excluded.address,
    notes = excluded.notes,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where clients.clinic_id = p_clinic_id;

  if not found then
    raise exception 'client could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'client')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id,
    v_user,
    'client',
    v_client_id,
    'upsert',
    jsonb_build_object('operation_id', p_operation_id, 'device_id', p_device_id)
  );

  return query
    select c.*
    from public.clients c
    where c.id = v_client_id and c.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_pet_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_pet_operation(uuid, uuid, text, jsonb) to authenticated;

revoke all on function public.apply_client_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_client_operation(uuid, uuid, text, jsonb) to authenticated;
