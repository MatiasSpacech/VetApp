-- VeterApp - etapa 4.5: completitud clínica.
-- Agrega campos de mascota, evolución de peso y sincronización de pesos.

alter table public.pets add column if not exists sex text;
alter table public.pets add column if not exists birth_date date;
alter table public.pets add column if not exists approx_age_months integer;
alter table public.pets add column if not exists color text;
alter table public.pets add column if not exists markings text;
alter table public.pets add column if not exists microchip text;
alter table public.pets add column if not exists reproductive_status text;
alter table public.pets add column if not exists allergies text;
alter table public.pets add column if not exists medical_notes text;

alter table public.pets drop constraint if exists pets_sex_check;
alter table public.pets
  add constraint pets_sex_check
  check (sex is null or sex in ('male', 'female', 'unknown'));

alter table public.pets drop constraint if exists pets_reproductive_check;
alter table public.pets
  add constraint pets_reproductive_check
  check (reproductive_status is null or reproductive_status in ('intact', 'neutered', 'unknown'));

create table if not exists public.pet_weights (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  pet_id uuid not null references public.pets (id) on delete cascade,
  measured_at timestamptz not null,
  weight_grams integer not null,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text,
  constraint pet_weights_weight_check check (weight_grams > 0)
);

create index if not exists pet_weights_clinic_pet_idx
  on public.pet_weights (clinic_id, pet_id, measured_at);

drop trigger if exists pet_weights_touch_updated_at on public.pet_weights;
create trigger pet_weights_touch_updated_at
before update on public.pet_weights
for each row execute function public.touch_updated_at();

alter table public.pet_weights enable row level security;

drop policy if exists "pet_weights_select_members" on public.pet_weights;
create policy "pet_weights_select_members"
on public.pet_weights for select
to authenticated
using (public.is_clinic_member(clinic_id));

revoke insert, update, delete on public.pet_weights from anon, authenticated;
grant select on public.pet_weights to authenticated;

create or replace function public.apply_pet_weight_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_weight jsonb
)
returns setof public.pet_weights
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_pet_id uuid;
  v_existing_clinic uuid;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  if not public.is_clinic_member(p_clinic_id) then
    raise exception 'not a clinic member';
  end if;

  if p_weight is null or coalesce(p_weight ->> 'id', '') = '' then
    raise exception 'invalid weight payload';
  end if;

  v_id := (p_weight ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select w.* from public.pet_weights w where w.id = v_id and w.clinic_id = p_clinic_id;
    return;
  end if;

  v_pet_id := (p_weight ->> 'pet_id')::uuid;
  if not exists (
    select 1 from public.pets p
    where p.id = v_pet_id and p.clinic_id = p_clinic_id
  ) then
    raise exception 'pet not found in clinic';
  end if;

  if coalesce(p_weight ->> 'weight_grams', '') = ''
     or (p_weight ->> 'weight_grams')::integer <= 0 then
    raise exception 'weight_grams must be positive';
  end if;

  select w.clinic_id into v_existing_clinic
  from public.pet_weights w where w.id = v_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'weight belongs to another clinic';
  end if;

  insert into public.pet_weights (
    id, clinic_id, pet_id, measured_at, weight_grams, notes,
    deleted_at, origin_device_id
  )
  values (
    v_id,
    p_clinic_id,
    v_pet_id,
    (p_weight ->> 'measured_at')::timestamptz,
    (p_weight ->> 'weight_grams')::integer,
    nullif(p_weight ->> 'notes', ''),
    nullif(p_weight ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_weight ->> 'origin_device_id')
  )
  on conflict (id) do update set
    pet_id = excluded.pet_id,
    measured_at = excluded.measured_at,
    weight_grams = excluded.weight_grams,
    notes = excluded.notes,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where pet_weights.clinic_id = p_clinic_id;

  if not found then
    raise exception 'weight could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'pet_weight')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'pet_weight', v_id, 'upsert',
    jsonb_build_object('operation_id', p_operation_id, 'device_id', p_device_id)
  );

  return query select w.* from public.pet_weights w where w.id = v_id and w.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_pet_weight_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_pet_weight_operation(uuid, uuid, text, jsonb) to authenticated;

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
      select p.* from public.pets p
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
      select 1 from public.clients c
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
  from public.pets p where p.id = v_pet_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'pet belongs to another clinic';
  end if;

  insert into public.pets (
    id, clinic_id, client_id, name, species, breed, owner_name, notes, foto_url,
    status, sex, birth_date, approx_age_months, color, markings, microchip,
    reproductive_status, allergies, medical_notes, deleted_at, origin_device_id
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
    nullif(p_pet ->> 'sex', ''),
    nullif(p_pet ->> 'birth_date', '')::date,
    nullif(p_pet ->> 'approx_age_months', '')::integer,
    nullif(p_pet ->> 'color', ''),
    nullif(p_pet ->> 'markings', ''),
    nullif(p_pet ->> 'microchip', ''),
    nullif(p_pet ->> 'reproductive_status', ''),
    nullif(p_pet ->> 'allergies', ''),
    nullif(p_pet ->> 'medical_notes', ''),
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
    sex = excluded.sex,
    birth_date = excluded.birth_date,
    approx_age_months = excluded.approx_age_months,
    color = excluded.color,
    markings = excluded.markings,
    microchip = excluded.microchip,
    reproductive_status = excluded.reproductive_status,
    allergies = excluded.allergies,
    medical_notes = excluded.medical_notes,
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
    p_clinic_id, v_user, 'pet', v_pet_id, 'upsert',
    jsonb_build_object('operation_id', p_operation_id, 'device_id', p_device_id)
  );

  return query
    select p.* from public.pets p
    where p.id = v_pet_id and p.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_pet_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_pet_operation(uuid, uuid, text, jsonb) to authenticated;
