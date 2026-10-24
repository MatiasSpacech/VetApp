-- VeterApp - etapa 3: historia clinica, vacunaciones y recordatorios.
-- Ejecutar en el SQL Editor de Supabase o con `supabase db push`.

create table if not exists public.consultations (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  pet_id uuid not null references public.pets (id) on delete cascade,
  consulted_at timestamptz not null,
  reason text not null,
  exam text,
  diagnosis text,
  treatment text,
  medication text,
  instructions text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text
);

create index if not exists consultations_clinic_updated_idx
  on public.consultations (clinic_id, consulted_at desc);
create index if not exists consultations_pet_idx
  on public.consultations (pet_id, consulted_at desc);

create table if not exists public.consultation_addenda (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  consultation_id uuid not null references public.consultations (id) on delete cascade,
  reason text not null,
  text text not null,
  author_id uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text
);

create index if not exists consultation_addenda_clinic_idx
  on public.consultation_addenda (clinic_id, created_at desc);
create index if not exists consultation_addenda_consultation_idx
  on public.consultation_addenda (consultation_id, created_at);

create table if not exists public.vaccinations (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  pet_id uuid not null references public.pets (id) on delete cascade,
  vaccine_name text not null,
  applied_on date not null,
  next_due_on date,
  lot text,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text,
  constraint vaccinations_next_due_check
    check (next_due_on is null or next_due_on > applied_on)
);

create index if not exists vaccinations_clinic_updated_idx
  on public.vaccinations (clinic_id, applied_on desc);
create index if not exists vaccinations_pet_idx
  on public.vaccinations (pet_id, applied_on desc);
create index if not exists vaccinations_next_due_idx
  on public.vaccinations (clinic_id, next_due_on)
  where deleted_at is null;

drop trigger if exists consultations_touch_updated_at on public.consultations;
create trigger consultations_touch_updated_at
before update on public.consultations
for each row execute function public.touch_updated_at();

drop trigger if exists consultation_addenda_touch_updated_at on public.consultation_addenda;
create trigger consultation_addenda_touch_updated_at
before update on public.consultation_addenda
for each row execute function public.touch_updated_at();

drop trigger if exists vaccinations_touch_updated_at on public.vaccinations;
create trigger vaccinations_touch_updated_at
before update on public.vaccinations
for each row execute function public.touch_updated_at();

alter table public.consultations enable row level security;
alter table public.consultation_addenda enable row level security;
alter table public.vaccinations enable row level security;

drop policy if exists "consultations_select_members" on public.consultations;
create policy "consultations_select_members"
on public.consultations for select
to authenticated
using (public.is_clinic_member(clinic_id));

drop policy if exists "consultation_addenda_select_members" on public.consultation_addenda;
create policy "consultation_addenda_select_members"
on public.consultation_addenda for select
to authenticated
using (public.is_clinic_member(clinic_id));

drop policy if exists "vaccinations_select_members" on public.vaccinations;
create policy "vaccinations_select_members"
on public.vaccinations for select
to authenticated
using (public.is_clinic_member(clinic_id));

revoke insert, update, delete on public.consultations from anon, authenticated;
revoke insert, update, delete on public.consultation_addenda from anon, authenticated;
revoke insert, update, delete on public.vaccinations from anon, authenticated;

grant select on public.consultations to authenticated;
grant select on public.consultation_addenda to authenticated;
grant select on public.vaccinations to authenticated;

create or replace function public.apply_consultation_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_consultation jsonb
)
returns setof public.consultations
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

  if p_consultation is null or coalesce(p_consultation ->> 'id', '') = '' then
    raise exception 'invalid consultation payload';
  end if;

  v_id := (p_consultation ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select c.* from public.consultations c
      where c.id = v_id and c.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_consultation ->> 'reason', '') = '' then
    raise exception 'reason is required';
  end if;

  if coalesce(p_consultation ->> 'pet_id', '') = '' then
    raise exception 'pet_id is required';
  end if;

  v_pet_id := (p_consultation ->> 'pet_id')::uuid;
  if not exists (
    select 1 from public.pets p
    where p.id = v_pet_id and p.clinic_id = p_clinic_id
  ) then
    raise exception 'pet not found in clinic';
  end if;

  select c.clinic_id into v_existing_clinic
  from public.consultations c
  where c.id = v_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'consultation belongs to another clinic';
  end if;

  insert into public.consultations (
    id, clinic_id, pet_id, consulted_at, reason, exam, diagnosis, treatment,
    medication, instructions, deleted_at, origin_device_id
  )
  values (
    v_id,
    p_clinic_id,
    v_pet_id,
    coalesce(nullif(p_consultation ->> 'consulted_at', '')::timestamptz, now()),
    p_consultation ->> 'reason',
    nullif(p_consultation ->> 'exam', ''),
    nullif(p_consultation ->> 'diagnosis', ''),
    nullif(p_consultation ->> 'treatment', ''),
    nullif(p_consultation ->> 'medication', ''),
    nullif(p_consultation ->> 'instructions', ''),
    nullif(p_consultation ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_consultation ->> 'origin_device_id')
  )
  on conflict (id) do update set
    pet_id = excluded.pet_id,
    consulted_at = excluded.consulted_at,
    reason = excluded.reason,
    exam = excluded.exam,
    diagnosis = excluded.diagnosis,
    treatment = excluded.treatment,
    medication = excluded.medication,
    instructions = excluded.instructions,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where consultations.clinic_id = p_clinic_id;

  if not found then
    raise exception 'consultation could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'consultation')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'consultation', v_id, 'upsert',
    jsonb_build_object('operation_id', p_operation_id, 'device_id', p_device_id)
  );

  return query
    select c.* from public.consultations c
    where c.id = v_id and c.clinic_id = p_clinic_id;
end;
$$;

create or replace function public.apply_addendum_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_addendum jsonb
)
returns setof public.consultation_addenda
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_consultation_id uuid;
  v_existing_clinic uuid;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  if not public.is_clinic_member(p_clinic_id) then
    raise exception 'not a clinic member';
  end if;

  if p_addendum is null or coalesce(p_addendum ->> 'id', '') = '' then
    raise exception 'invalid addendum payload';
  end if;

  v_id := (p_addendum ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select a.* from public.consultation_addenda a
      where a.id = v_id and a.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_addendum ->> 'reason', '') = ''
     or coalesce(p_addendum ->> 'text', '') = '' then
    raise exception 'reason and text are required';
  end if;

  if coalesce(p_addendum ->> 'consultation_id', '') = '' then
    raise exception 'consultation_id is required';
  end if;

  v_consultation_id := (p_addendum ->> 'consultation_id')::uuid;
  if not exists (
    select 1 from public.consultations c
    where c.id = v_consultation_id and c.clinic_id = p_clinic_id
  ) then
    raise exception 'consultation not found in clinic';
  end if;

  select a.clinic_id into v_existing_clinic
  from public.consultation_addenda a
  where a.id = v_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'addendum belongs to another clinic';
  end if;

  insert into public.consultation_addenda (
    id, clinic_id, consultation_id, reason, text, author_id, deleted_at, origin_device_id
  )
  values (
    v_id,
    p_clinic_id,
    v_consultation_id,
    p_addendum ->> 'reason',
    p_addendum ->> 'text',
    coalesce(nullif(p_addendum ->> 'author_id', '')::uuid, v_user),
    nullif(p_addendum ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_addendum ->> 'origin_device_id')
  )
  on conflict (id) do update set
    consultation_id = excluded.consultation_id,
    reason = excluded.reason,
    text = excluded.text,
    author_id = excluded.author_id,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where consultation_addenda.clinic_id = p_clinic_id;

  if not found then
    raise exception 'addendum could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'consultation_addendum')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'consultation', v_consultation_id, 'addendum',
    jsonb_build_object('operation_id', p_operation_id, 'addendum_id', v_id)
  );

  return query
    select a.* from public.consultation_addenda a
    where a.id = v_id and a.clinic_id = p_clinic_id;
end;
$$;

create or replace function public.apply_vaccination_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_vaccination jsonb
)
returns setof public.vaccinations
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_pet_id uuid;
  v_existing_clinic uuid;
  v_applied_on date;
  v_next_due_on date;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  if not public.is_clinic_member(p_clinic_id) then
    raise exception 'not a clinic member';
  end if;

  if p_vaccination is null or coalesce(p_vaccination ->> 'id', '') = '' then
    raise exception 'invalid vaccination payload';
  end if;

  v_id := (p_vaccination ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select v.* from public.vaccinations v
      where v.id = v_id and v.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_vaccination ->> 'vaccine_name', '') = '' then
    raise exception 'vaccine_name is required';
  end if;

  if coalesce(p_vaccination ->> 'applied_on', '') = '' then
    raise exception 'applied_on is required';
  end if;

  v_applied_on := (p_vaccination ->> 'applied_on')::date;
  v_next_due_on := nullif(p_vaccination ->> 'next_due_on', '')::date;

  if v_next_due_on is not null and v_next_due_on <= v_applied_on then
    raise exception 'next_due_on must be after applied_on';
  end if;

  if coalesce(p_vaccination ->> 'pet_id', '') = '' then
    raise exception 'pet_id is required';
  end if;

  v_pet_id := (p_vaccination ->> 'pet_id')::uuid;
  if not exists (
    select 1 from public.pets p
    where p.id = v_pet_id and p.clinic_id = p_clinic_id
  ) then
    raise exception 'pet not found in clinic';
  end if;

  select v.clinic_id into v_existing_clinic
  from public.vaccinations v
  where v.id = v_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'vaccination belongs to another clinic';
  end if;

  insert into public.vaccinations (
    id, clinic_id, pet_id, vaccine_name, applied_on, next_due_on, lot, notes,
    deleted_at, origin_device_id
  )
  values (
    v_id,
    p_clinic_id,
    v_pet_id,
    p_vaccination ->> 'vaccine_name',
    v_applied_on,
    v_next_due_on,
    nullif(p_vaccination ->> 'lot', ''),
    nullif(p_vaccination ->> 'notes', ''),
    nullif(p_vaccination ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_vaccination ->> 'origin_device_id')
  )
  on conflict (id) do update set
    pet_id = excluded.pet_id,
    vaccine_name = excluded.vaccine_name,
    applied_on = excluded.applied_on,
    next_due_on = excluded.next_due_on,
    lot = excluded.lot,
    notes = excluded.notes,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where vaccinations.clinic_id = p_clinic_id;

  if not found then
    raise exception 'vaccination could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'vaccination')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'vaccination', v_id, 'upsert',
    jsonb_build_object('operation_id', p_operation_id, 'device_id', p_device_id)
  );

  return query
    select v.* from public.vaccinations v
    where v.id = v_id and v.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_consultation_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_consultation_operation(uuid, uuid, text, jsonb) to authenticated;

revoke all on function public.apply_addendum_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_addendum_operation(uuid, uuid, text, jsonb) to authenticated;

revoke all on function public.apply_vaccination_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_vaccination_operation(uuid, uuid, text, jsonb) to authenticated;
