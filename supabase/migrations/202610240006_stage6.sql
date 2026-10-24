-- VeterApp - etapa 6: ventas, pagos y caja.

create table if not exists public.payment_methods (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  name text not null,
  affects_cash boolean not null default false,
  active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text
);

create index if not exists payment_methods_clinic_idx
  on public.payment_methods (clinic_id, active, sort_order);

drop trigger if exists payment_methods_touch_updated_at on public.payment_methods;
create trigger payment_methods_touch_updated_at
before update on public.payment_methods
for each row execute function public.touch_updated_at();

alter table public.payment_methods enable row level security;
drop policy if exists "payment_methods_select_members" on public.payment_methods;
create policy "payment_methods_select_members"
on public.payment_methods for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.payment_methods from anon, authenticated;
grant select on public.payment_methods to authenticated;

create table if not exists public.sales (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  number text not null,
  client_id uuid references public.clients (id) on delete set null,
  client_name text,
  status text not null default 'completed',
  subtotal_cents bigint not null,
  discount_cents bigint not null,
  total_cents bigint not null,
  notes text,
  sold_at timestamptz not null,
  cancelled_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  origin_device_id text,
  constraint sales_status_check check (status in ('completed', 'cancelled')),
  constraint sales_totals_check
    check (subtotal_cents >= 0 and discount_cents >= 0 and total_cents >= 0
      and total_cents = subtotal_cents - discount_cents)
);

create unique index if not exists sales_clinic_number_uq on public.sales (clinic_id, number);
create index if not exists sales_clinic_sold_idx on public.sales (clinic_id, sold_at, status);

drop trigger if exists sales_touch_updated_at on public.sales;
create trigger sales_touch_updated_at
before update on public.sales
for each row execute function public.touch_updated_at();

alter table public.sales enable row level security;
drop policy if exists "sales_select_members" on public.sales;
create policy "sales_select_members"
on public.sales for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.sales from anon, authenticated;
grant select on public.sales to authenticated;

create table if not exists public.sale_items (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  sale_id uuid not null references public.sales (id) on delete cascade,
  item_id uuid not null references public.catalog_items (id),
  item_kind text not null,
  item_name text not null,
  area_id uuid not null,
  area_name text not null,
  quantity integer not null,
  unit_price_cents bigint not null,
  unit_cost_cents bigint,
  discount_cents bigint not null,
  gross_cents bigint not null,
  net_cents bigint not null,
  expenses_percent_bp integer not null default 0,
  lot text,
  expires_on date,
  created_at timestamptz not null default now(),
  origin_device_id text,
  constraint sale_items_quantity_check check (quantity > 0),
  constraint sale_items_amounts_check
    check (unit_price_cents >= 0 and discount_cents >= 0 and net_cents = gross_cents - discount_cents
      and expenses_percent_bp between 0 and 10000)
);

create index if not exists sale_items_clinic_sale_idx on public.sale_items (clinic_id, sale_id);
create index if not exists sale_items_clinic_area_idx on public.sale_items (clinic_id, area_id);
create index if not exists sale_items_clinic_item_idx on public.sale_items (clinic_id, item_id);

alter table public.sale_items enable row level security;
drop policy if exists "sale_items_select_members" on public.sale_items;
create policy "sale_items_select_members"
on public.sale_items for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.sale_items from anon, authenticated;
grant select on public.sale_items to authenticated;

create table if not exists public.payments (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  sale_id uuid not null references public.sales (id) on delete cascade,
  method_id uuid not null references public.payment_methods (id),
  amount_cents bigint not null,
  tendered_cents bigint,
  change_cents bigint,
  created_at timestamptz not null default now(),
  origin_device_id text,
  constraint payments_amount_check check (amount_cents > 0),
  constraint payments_change_check
    check (change_cents is null or tendered_cents is null or change_cents = tendered_cents - amount_cents)
);

create index if not exists payments_clinic_sale_idx on public.payments (clinic_id, sale_id);
create index if not exists payments_clinic_method_idx on public.payments (clinic_id, method_id);

alter table public.payments enable row level security;
drop policy if exists "payments_select_members" on public.payments;
create policy "payments_select_members"
on public.payments for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.payments from anon, authenticated;
grant select on public.payments to authenticated;

create table if not exists public.cash_sessions (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  opened_at timestamptz not null,
  closed_at timestamptz,
  opening_cents bigint not null,
  expected_cash_cents bigint,
  counted_cash_cents bigint,
  difference_cents bigint,
  status text not null default 'open',
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  origin_device_id text,
  constraint cash_sessions_status_check check (status in ('open', 'closed')),
  constraint cash_sessions_amounts_check
    check (opening_cents >= 0 and (counted_cash_cents is null or counted_cash_cents >= 0)
      and (difference_cents is null or expected_cash_cents is null
        or difference_cents = counted_cash_cents - expected_cash_cents))
);

create unique index if not exists cash_sessions_one_open_uq
  on public.cash_sessions (clinic_id) where status = 'open';
create index if not exists cash_sessions_clinic_open_idx
  on public.cash_sessions (clinic_id, opened_at desc);

drop trigger if exists cash_sessions_touch_updated_at on public.cash_sessions;
create trigger cash_sessions_touch_updated_at
before update on public.cash_sessions
for each row execute function public.touch_updated_at();

alter table public.cash_sessions enable row level security;
drop policy if exists "cash_sessions_select_members" on public.cash_sessions;
create policy "cash_sessions_select_members"
on public.cash_sessions for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.cash_sessions from anon, authenticated;
grant select on public.cash_sessions to authenticated;

create table if not exists public.cash_movements (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  session_id uuid not null references public.cash_sessions (id) on delete cascade,
  type text not null,
  amount_cents bigint not null,
  reason text,
  sale_id uuid references public.sales (id) on delete set null,
  payment_id uuid references public.payments (id) on delete set null,
  sale_cancellation_id uuid,
  created_at timestamptz not null default now(),
  origin_device_id text,
  constraint cash_movements_type_check
    check (type in ('sale', 'cancellation', 'withdrawal', 'expense', 'adjustment')),
  constraint cash_movements_amount_check check (amount_cents <> 0)
);

create index if not exists cash_movements_clinic_session_idx
  on public.cash_movements (clinic_id, session_id, created_at);

alter table public.cash_movements enable row level security;
drop policy if exists "cash_movements_select_members" on public.cash_movements;
create policy "cash_movements_select_members"
on public.cash_movements for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.cash_movements from anon, authenticated;
grant select on public.cash_movements to authenticated;

create table if not exists public.sale_cancellations (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  sale_id uuid not null references public.sales (id) on delete cascade,
  reason text not null,
  cancelled_at timestamptz not null,
  created_at timestamptz not null default now(),
  origin_device_id text,
  constraint sale_cancellations_unique unique (sale_id)
);

alter table public.sale_cancellations enable row level security;
drop policy if exists "sale_cancellations_select_members" on public.sale_cancellations;
create policy "sale_cancellations_select_members"
on public.sale_cancellations for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.sale_cancellations from anon, authenticated;
grant select on public.sale_cancellations to authenticated;

create table if not exists public.area_cost_settings (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  area_id uuid not null references public.areas (id),
  expenses_percent_bp integer not null,
  valid_from date not null,
  created_at timestamptz not null default now(),
  origin_device_id text,
  constraint area_cost_percent_check check (expenses_percent_bp between 0 and 10000)
);

create unique index if not exists area_cost_settings_area_date_uq
  on public.area_cost_settings (clinic_id, area_id, valid_from);

alter table public.area_cost_settings enable row level security;
drop policy if exists "area_cost_settings_select_members" on public.area_cost_settings;
create policy "area_cost_settings_select_members"
on public.area_cost_settings for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.area_cost_settings from anon, authenticated;
grant select on public.area_cost_settings to authenticated;

create or replace function public.apply_payment_method_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_method jsonb
)
returns setof public.payment_methods
language plpgsql security definer set search_path = public, pg_temp
as $$
declare
  v_id uuid;
  v_existing uuid;
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_method is null or coalesce(p_method ->> 'id', '') = '' then raise exception 'invalid method payload'; end if;
  v_id := (p_method ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select m.* from public.payment_methods m where m.id = v_id;
    return;
  end if;

  select m.clinic_id into v_existing from public.payment_methods m where m.id = v_id;
  if v_existing is not null and v_existing <> p_clinic_id then
    raise exception 'method belongs to another clinic';
  end if;

  insert into public.payment_methods (
    id, clinic_id, name, affects_cash, active, sort_order, deleted_at, origin_device_id
  ) values (
    v_id, p_clinic_id, p_method ->> 'name',
    coalesce((p_method ->> 'affects_cash')::boolean, false),
    coalesce((p_method ->> 'active')::boolean, true),
    coalesce((p_method ->> 'sort_order')::integer, 0),
    nullif(p_method ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_method ->> 'origin_device_id')
  )
  on conflict (id) do update set
    name = excluded.name,
    affects_cash = excluded.affects_cash,
    active = excluded.active,
    sort_order = excluded.sort_order,
    deleted_at = excluded.deleted_at
  where payment_methods.clinic_id = p_clinic_id;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'payment_method')
  on conflict (operation_id) do nothing;

  return query select m.* from public.payment_methods m where m.id = v_id;
end;
$$;

create or replace function public.apply_area_cost_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_setting jsonb
)
returns setof public.area_cost_settings
language plpgsql security definer set search_path = public, pg_temp
as $$
declare
  v_id uuid;
  v_area uuid;
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_setting is null or coalesce(p_setting ->> 'id', '') = '' then raise exception 'invalid setting payload'; end if;
  v_id := (p_setting ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select s.* from public.area_cost_settings s where s.id = v_id;
    return;
  end if;

  v_area := (p_setting ->> 'area_id')::uuid;
  if not exists (select 1 from public.areas a where a.id = v_area and a.clinic_id = p_clinic_id) then
    raise exception 'area not found in clinic';
  end if;

  insert into public.area_cost_settings (
    id, clinic_id, area_id, expenses_percent_bp, valid_from, origin_device_id
  ) values (
    v_id, p_clinic_id, v_area,
    (p_setting ->> 'expenses_percent_bp')::integer,
    (p_setting ->> 'valid_from')::date,
    coalesce(nullif(p_device_id, ''), p_setting ->> 'origin_device_id')
  )
  on conflict (id) do nothing;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'area_cost')
  on conflict (operation_id) do nothing;

  return query select s.* from public.area_cost_settings s where s.id = v_id;
end;
$$;

create or replace function public.apply_cash_session_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_session jsonb
)
returns setof public.cash_sessions
language plpgsql security definer set search_path = public, pg_temp
as $$
declare
  v_id uuid;
  v_existing uuid;
  v_status text;
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_session is null or coalesce(p_session ->> 'id', '') = '' then raise exception 'invalid session payload'; end if;
  v_id := (p_session ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select s.* from public.cash_sessions s where s.id = v_id;
    return;
  end if;

  v_status := coalesce(nullif(p_session ->> 'status', ''), 'open');
  if v_status not in ('open', 'closed') then raise exception 'invalid session status'; end if;

  select s.clinic_id into v_existing from public.cash_sessions s where s.id = v_id;
  if v_existing is not null and v_existing <> p_clinic_id then
    raise exception 'session belongs to another clinic';
  end if;

  if v_status = 'open' and exists (
    select 1 from public.cash_sessions s
    where s.clinic_id = p_clinic_id and s.status = 'open' and s.id <> v_id
  ) then
    raise exception 'another cash session is open';
  end if;

  insert into public.cash_sessions (
    id, clinic_id, opened_at, closed_at, opening_cents, expected_cash_cents,
    counted_cash_cents, difference_cents, status, notes, origin_device_id
  ) values (
    v_id, p_clinic_id,
    (p_session ->> 'opened_at')::timestamptz,
    nullif(p_session ->> 'closed_at', '')::timestamptz,
    (p_session ->> 'opening_cents')::bigint,
    nullif(p_session ->> 'expected_cash_cents', '')::bigint,
    nullif(p_session ->> 'counted_cash_cents', '')::bigint,
    nullif(p_session ->> 'difference_cents', '')::bigint,
    v_status,
    nullif(p_session ->> 'notes', ''),
    coalesce(nullif(p_device_id, ''), p_session ->> 'origin_device_id')
  )
  on conflict (id) do update set
    closed_at = excluded.closed_at,
    expected_cash_cents = excluded.expected_cash_cents,
    counted_cash_cents = excluded.counted_cash_cents,
    difference_cents = excluded.difference_cents,
    status = excluded.status,
    notes = excluded.notes
  where cash_sessions.clinic_id = p_clinic_id;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'cash_session')
  on conflict (operation_id) do nothing;

  return query select s.* from public.cash_sessions s where s.id = v_id;
end;
$$;

create or replace function public.apply_sale_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_sale jsonb
)
returns setof public.sales
language plpgsql security definer set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_existing uuid;
  v_item jsonb;
  v_movement jsonb;
  v_payment jsonb;
  v_movement_id uuid;
  v_item_id uuid;
  v_delta integer;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_sale is null or p_sale -> 'sale' is null then raise exception 'invalid sale payload'; end if;
  v_id := (p_sale -> 'sale' ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select s.* from public.sales s where s.id = v_id and s.clinic_id = p_clinic_id;
    return;
  end if;

  select s.clinic_id into v_existing from public.sales s where s.id = v_id;
  if v_existing is not null then
    return query select s.* from public.sales s where s.id = v_id and s.clinic_id = p_clinic_id;
    return;
  end if;

  insert into public.sales (
    id, clinic_id, number, client_id, client_name, status, subtotal_cents,
    discount_cents, total_cents, notes, sold_at, cancelled_at, origin_device_id
  ) values (
    v_id, p_clinic_id,
    p_sale -> 'sale' ->> 'number',
    nullif(p_sale -> 'sale' ->> 'client_id', '')::uuid,
    nullif(p_sale -> 'sale' ->> 'client_name', ''),
    coalesce(nullif(p_sale -> 'sale' ->> 'status', ''), 'completed'),
    (p_sale -> 'sale' ->> 'subtotal_cents')::bigint,
    (p_sale -> 'sale' ->> 'discount_cents')::bigint,
    (p_sale -> 'sale' ->> 'total_cents')::bigint,
    nullif(p_sale -> 'sale' ->> 'notes', ''),
    (p_sale -> 'sale' ->> 'sold_at')::timestamptz,
    nullif(p_sale -> 'sale' ->> 'cancelled_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_sale -> 'sale' ->> 'origin_device_id')
  );

  for v_item in select * from jsonb_array_elements(coalesce(p_sale -> 'items', '[]'::jsonb))
  loop
    insert into public.sale_items (
      id, clinic_id, sale_id, item_id, item_kind, item_name, area_id, area_name,
      quantity, unit_price_cents, unit_cost_cents, discount_cents, gross_cents,
      net_cents, expenses_percent_bp, lot, expires_on, origin_device_id
    ) values (
      (v_item ->> 'id')::uuid, p_clinic_id, v_id,
      (v_item ->> 'item_id')::uuid, v_item ->> 'item_kind', v_item ->> 'item_name',
      (v_item ->> 'area_id')::uuid, v_item ->> 'area_name',
      (v_item ->> 'quantity')::integer,
      (v_item ->> 'unit_price_cents')::bigint,
      nullif(v_item ->> 'unit_cost_cents', '')::bigint,
      (v_item ->> 'discount_cents')::bigint,
      (v_item ->> 'gross_cents')::bigint,
      (v_item ->> 'net_cents')::bigint,
      (v_item ->> 'expenses_percent_bp')::integer,
      nullif(v_item ->> 'lot', ''),
      nullif(v_item ->> 'expires_on', '')::date,
      v_item ->> 'origin_device_id'
    )
    on conflict (id) do nothing;
  end loop;

  for v_payment in select * from jsonb_array_elements(coalesce(p_sale -> 'payments', '[]'::jsonb))
  loop
    insert into public.payments (
      id, clinic_id, sale_id, method_id, amount_cents, tendered_cents, change_cents, origin_device_id
    ) values (
      (v_payment ->> 'id')::uuid, p_clinic_id, v_id,
      (v_payment ->> 'method_id')::uuid,
      (v_payment ->> 'amount_cents')::bigint,
      nullif(v_payment ->> 'tendered_cents', '')::bigint,
      nullif(v_payment ->> 'change_cents', '')::bigint,
      v_payment ->> 'origin_device_id'
    )
    on conflict (id) do nothing;
  end loop;

  for v_movement in select * from jsonb_array_elements(coalesce(p_sale -> 'stock_movements', '[]'::jsonb))
  loop
    insert into public.stock_movements (
      id, clinic_id, item_id, type, quantity_delta, unit_cost_cents, reason,
      sale_id, sale_item_id, sale_cancellation_id, created_at, origin_device_id
    ) values (
      (v_movement ->> 'id')::uuid, p_clinic_id,
      (v_movement ->> 'item_id')::uuid,
      v_movement ->> 'type',
      (v_movement ->> 'quantity_delta')::integer,
      nullif(v_movement ->> 'unit_cost_cents', '')::bigint,
      nullif(v_movement ->> 'reason', ''),
      v_id,
      nullif(v_movement ->> 'sale_item_id', '')::uuid,
      null,
      coalesce(nullif(v_movement ->> 'created_at', '')::timestamptz, now()),
      v_movement ->> 'origin_device_id'
    )
    on conflict (id) do nothing;

    if found then
      v_item_id := (v_movement ->> 'item_id')::uuid;
      v_delta := (v_movement ->> 'quantity_delta')::integer;
      update public.catalog_items
      set stock_cached = stock_cached + v_delta
      where id = v_item_id and clinic_id = p_clinic_id;
    end if;
  end loop;

  for v_movement in select * from jsonb_array_elements(coalesce(p_sale -> 'cash_movements', '[]'::jsonb))
  loop
    insert into public.cash_movements (
      id, clinic_id, session_id, type, amount_cents, reason, sale_id, payment_id,
      sale_cancellation_id, created_at, origin_device_id
    ) values (
      (v_movement ->> 'id')::uuid, p_clinic_id,
      (v_movement ->> 'session_id')::uuid,
      v_movement ->> 'type',
      (v_movement ->> 'amount_cents')::bigint,
      nullif(v_movement ->> 'reason', ''),
      v_id,
      nullif(v_movement ->> 'payment_id', '')::uuid,
      null,
      coalesce(nullif(v_movement ->> 'created_at', '')::timestamptz, now()),
      v_movement ->> 'origin_device_id'
    )
    on conflict (id) do nothing;
  end loop;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'sale')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'sale', v_id, 'create',
    jsonb_build_object('operation_id', p_operation_id, 'number', p_sale -> 'sale' ->> 'number')
  );

  return query select s.* from public.sales s where s.id = v_id and s.clinic_id = p_clinic_id;
end;
$$;

create or replace function public.apply_sale_cancellation_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_cancellation jsonb
)
returns setof public.sale_cancellations
language plpgsql security definer set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_sale_id uuid;
  v_movement jsonb;
  v_item_id uuid;
  v_delta integer;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_cancellation is null or p_cancellation -> 'cancellation' is null then
    raise exception 'invalid cancellation payload';
  end if;
  v_id := (p_cancellation -> 'cancellation' ->> 'id')::uuid;
  v_sale_id := (p_cancellation -> 'cancellation' ->> 'sale_id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select c.* from public.sale_cancellations c where c.id = v_id;
    return;
  end if;

  insert into public.sale_cancellations (
    id, clinic_id, sale_id, reason, cancelled_at, origin_device_id
  ) values (
    v_id, p_clinic_id, v_sale_id,
    p_cancellation -> 'cancellation' ->> 'reason',
    (p_cancellation -> 'cancellation' ->> 'cancelled_at')::timestamptz,
    p_cancellation -> 'cancellation' ->> 'origin_device_id'
  )
  on conflict (id) do nothing;

  update public.sales
  set status = 'cancelled',
      cancelled_at = (p_cancellation -> 'cancellation' ->> 'cancelled_at')::timestamptz
  where id = v_sale_id and clinic_id = p_clinic_id;

  for v_movement in select * from jsonb_array_elements(coalesce(p_cancellation -> 'stock_movements', '[]'::jsonb))
  loop
    insert into public.stock_movements (
      id, clinic_id, item_id, type, quantity_delta, unit_cost_cents, reason,
      sale_id, sale_item_id, sale_cancellation_id, created_at, origin_device_id
    ) values (
      (v_movement ->> 'id')::uuid, p_clinic_id,
      (v_movement ->> 'item_id')::uuid,
      v_movement ->> 'type',
      (v_movement ->> 'quantity_delta')::integer,
      nullif(v_movement ->> 'unit_cost_cents', '')::bigint,
      nullif(v_movement ->> 'reason', ''),
      v_sale_id,
      nullif(v_movement ->> 'sale_item_id', '')::uuid,
      v_id,
      coalesce(nullif(v_movement ->> 'created_at', '')::timestamptz, now()),
      v_movement ->> 'origin_device_id'
    )
    on conflict (id) do nothing;

    if found then
      v_item_id := (v_movement ->> 'item_id')::uuid;
      v_delta := (v_movement ->> 'quantity_delta')::integer;
      update public.catalog_items
      set stock_cached = stock_cached + v_delta
      where id = v_item_id and clinic_id = p_clinic_id;
    end if;
  end loop;

  for v_movement in select * from jsonb_array_elements(coalesce(p_cancellation -> 'cash_movements', '[]'::jsonb))
  loop
    insert into public.cash_movements (
      id, clinic_id, session_id, type, amount_cents, reason, sale_id, payment_id,
      sale_cancellation_id, created_at, origin_device_id
    ) values (
      (v_movement ->> 'id')::uuid, p_clinic_id,
      (v_movement ->> 'session_id')::uuid,
      v_movement ->> 'type',
      (v_movement ->> 'amount_cents')::bigint,
      nullif(v_movement ->> 'reason', ''),
      v_sale_id,
      nullif(v_movement ->> 'payment_id', '')::uuid,
      v_id,
      coalesce(nullif(v_movement ->> 'created_at', '')::timestamptz, now()),
      v_movement ->> 'origin_device_id'
    )
    on conflict (id) do nothing;
  end loop;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'sale_cancellation')
  on conflict (operation_id) do nothing;

  insert into public.audit_log (clinic_id, actor_id, entity, entity_id, action, payload)
  values (
    p_clinic_id, v_user, 'sale', v_sale_id, 'cancel',
    jsonb_build_object('operation_id', p_operation_id, 'cancellation_id', v_id)
  );

  return query select c.* from public.sale_cancellations c where c.id = v_id;
end;
$$;

create or replace function public.apply_cash_movement_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_movement jsonb
)
returns setof public.cash_movements
language plpgsql security definer set search_path = public, pg_temp
as $$
declare
  v_id uuid;
  v_session uuid;
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_movement is null or coalesce(p_movement ->> 'id', '') = '' then raise exception 'invalid movement payload'; end if;
  v_id := (p_movement ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select m.* from public.cash_movements m where m.id = v_id;
    return;
  end if;

  v_session := (p_movement ->> 'session_id')::uuid;
  if not exists (
    select 1 from public.cash_sessions s where s.id = v_session and s.clinic_id = p_clinic_id
  ) then
    raise exception 'cash session not found in clinic';
  end if;

  insert into public.cash_movements (
    id, clinic_id, session_id, type, amount_cents, reason, sale_id, payment_id,
    sale_cancellation_id, created_at, origin_device_id
  ) values (
    v_id, p_clinic_id, v_session,
    p_movement ->> 'type',
    (p_movement ->> 'amount_cents')::bigint,
    nullif(p_movement ->> 'reason', ''),
    nullif(p_movement ->> 'sale_id', '')::uuid,
    nullif(p_movement ->> 'payment_id', '')::uuid,
    nullif(p_movement ->> 'sale_cancellation_id', '')::uuid,
    coalesce(nullif(p_movement ->> 'created_at', '')::timestamptz, now()),
    coalesce(nullif(p_device_id, ''), p_movement ->> 'origin_device_id')
  )
  on conflict (id) do nothing;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'cash_movement')
  on conflict (operation_id) do nothing;

  return query select m.* from public.cash_movements m where m.id = v_id;
end;
$$;

revoke all on function public.apply_payment_method_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_area_cost_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_cash_session_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_sale_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_sale_cancellation_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_cash_movement_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_payment_method_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_area_cost_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_cash_session_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_sale_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_sale_cancellation_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_cash_movement_operation(uuid, uuid, text, jsonb) to authenticated;
