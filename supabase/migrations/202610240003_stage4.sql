-- VeterApp - etapa 4: turnos y agenda.
-- Ejecutar en el SQL Editor de Supabase o con `supabase db push`.

create table if not exists public.appointments (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  pet_id uuid not null references public.pets (id) on delete cascade,
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  status text not null default 'scheduled',
  reason text not null,
  notes text,
  consultation_id uuid references public.consultations (id) on delete set null,
  cancel_reason text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text,
  constraint appointments_time_check check (ends_at > starts_at),
  constraint appointments_status_check
    check (status in ('scheduled', 'confirmed', 'attended', 'cancelled', 'no_show'))
);

create index if not exists appointments_clinic_starts_idx
  on public.appointments (clinic_id, starts_at);
create index if not exists appointments_pet_starts_idx
  on public.appointments (pet_id, starts_at);
create index if not exists appointments_clinic_status_idx
  on public.appointments (clinic_id, status, starts_at)
  where deleted_at is null;

drop trigger if exists appointments_touch_updated_at on public.appointments;
create trigger appointments_touch_updated_at
before update on public.appointments
for each row execute function public.touch_updated_at();

alter table public.appointments enable row level security;

drop policy if exists "appointments_select_members" on public.appointments;
create policy "appointments_select_members"
on public.appointments for select
to authenticated
using (public.is_clinic_member(clinic_id));

revoke insert, update, delete on public.appointments from anon, authenticated;
grant select on public.appointments to authenticated;

create or replace function public.apply_appointment_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_appointment jsonb
)
returns setof public.appointments
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_pet_id uuid;
  v_consultation_id uuid;
  v_existing_clinic uuid;
  v_starts_at timestamptz;
  v_ends_at timestamptz;
  v_status text;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  if not public.is_clinic_member(p_clinic_id) then
    raise exception 'not a clinic member';
  end if;

  if p_appointment is null or coalesce(p_appointment ->> 'id', '') = '' then
    raise exception 'invalid appointment payload';
  end if;

  v_id := (p_appointment ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query
      select a.* from public.appointments a
      where a.id = v_id and a.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_appointment ->> 'reason', '') = '' then
    raise exception 'reason is required';
  end if;

  if coalesce(p_appointment ->> 'pet_id', '') = '' then
    raise exception 'pet_id is required';
  end if;

  if coalesce(p_appointment ->> 'starts_at', '') = ''
     or coalesce(p_appointment ->> 'ends_at', '') = '' then
    raise exception 'starts_at and ends_at are required';
  end if;

  v_starts_at := (p_appointment ->> 'starts_at')::timestamptz;
  v_ends_at := (p_appointment ->> 'ends_at')::timestamptz;
  if v_ends_at <= v_starts_at then
    raise exception 'ends_at must be after starts_at';
  end if;

  v_status := coalesce(nullif(p_appointment ->> 'status', ''), 'scheduled');
  if v_status not in ('scheduled', 'confirmed', 'attended', 'cancelled', 'no_show') then
    raise exception 'invalid appointment status';
  end if;

  v_pet_id := (p_appointment ->> 'pet_id')::uuid;
  if not exists (
    select 1 from public.pets p
    where p.id = v_pet_id and p.clinic_id = p_clinic_id
  ) then
    raise exception 'pet not found in clinic';
  end if;

  if nullif(p_appointment ->> 'consultation_id', '') is not null then
    v_consultation_id := (p_appointment ->> 'consultation_id')::uuid;
    if not exists (
      select 1 from public.consultations c
      where c.id = v_consultation_id
        and c.clinic_id = p_clinic_id
        and c.pet_id = v_pet_id
    ) then
      raise exception 'consultation not found for pet in clinic';
    end if;
  end if;

  select a.clinic_id into v_existing_clinic
  from public.appointments a
  where a.id = v_id;

  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'appointment belongs to another clinic';
  end if;

  insert into public.appointments (
    id, clinic_id, pet_id, starts_at, ends_at, status, reason, notes,
    consultation_id, cancel_reason, deleted_at, origin_device_id
  )
  values (
    v_id,
    p_clinic_id,
    v_pet_id,
    v_starts_at,
    v_ends_at,
    v_status,
    p_appointment ->> 'reason',
    nullif(p_appointment ->> 'notes', ''),
    v_consultation_id,
    nullif(p_appointment ->> 'cancel_reason', ''),
    nullif(p_appointment ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_appointment ->> 'origin_device_id')
  )
  on conflict (id) do update set
    pet_id = excluded.pet_id,
    starts_at = excluded.starts_at,
    ends_at = excluded.ends_at,
    status = excluded.status,
    reason = excluded.reason,
    notes = excluded.notes,
    consultation_id = excluded.consultation_id,
    cancel_reason = excluded.cancel_reason,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where appointments.clinic_id = p_clinic_id;

  if not found then
    raise exception 'appointment could not be applied';
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'appointment')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'appointment', v_id, 'upsert',
    jsonb_build_object(
      'operation_id', p_operation_id,
      'device_id', p_device_id,
      'status', v_status
    )
  );

  return query
    select a.* from public.appointments a
    where a.id = v_id and a.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_appointment_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_appointment_operation(uuid, uuid, text, jsonb) to authenticated;
