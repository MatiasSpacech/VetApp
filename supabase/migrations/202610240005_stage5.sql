-- VeterApp - etapa 5: catálogo e inventario.

create table if not exists public.areas (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  name text not null,
  active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text
);

create index if not exists areas_clinic_idx on public.areas (clinic_id, active, sort_order);

drop trigger if exists areas_touch_updated_at on public.areas;
create trigger areas_touch_updated_at
before update on public.areas
for each row execute function public.touch_updated_at();

alter table public.areas enable row level security;
drop policy if exists "areas_select_members" on public.areas;
create policy "areas_select_members"
on public.areas for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.areas from anon, authenticated;
grant select on public.areas to authenticated;

create table if not exists public.categories (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  name text not null,
  parent_id uuid references public.categories (id) on delete set null,
  scope text not null default 'both',
  active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text,
  constraint categories_scope_check check (scope in ('product', 'service', 'both'))
);

create index if not exists categories_clinic_parent_idx
  on public.categories (clinic_id, parent_id, sort_order);

drop trigger if exists categories_touch_updated_at on public.categories;
create trigger categories_touch_updated_at
before update on public.categories
for each row execute function public.touch_updated_at();

alter table public.categories enable row level security;
drop policy if exists "categories_select_members" on public.categories;
create policy "categories_select_members"
on public.categories for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.categories from anon, authenticated;
grant select on public.categories to authenticated;

create table if not exists public.catalog_items (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  kind text not null,
  internal_code text not null,
  barcode text,
  name text not null,
  description text,
  category_id uuid references public.categories (id) on delete set null,
  brand text,
  unit_cost_cents bigint,
  sale_price_cents bigint not null,
  track_stock boolean not null default true,
  stock_cached integer not null default 0,
  stock_min integer not null default 0,
  area_id uuid not null references public.areas (id),
  lot text,
  expires_on date,
  duration_minutes integer,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  revision bigint not null default 1,
  deleted_at timestamptz,
  origin_device_id text,
  constraint catalog_kind_check check (kind in ('product', 'service')),
  constraint catalog_price_check check (sale_price_cents >= 0),
  constraint catalog_cost_check check (unit_cost_cents is null or unit_cost_cents >= 0),
  constraint catalog_stock_check check (stock_min >= 0),
  constraint catalog_duration_check check (duration_minutes is null or duration_minutes > 0),
  constraint catalog_service_check
    check (kind = 'product' or (track_stock = false and lot is null and expires_on is null))
);

create unique index if not exists catalog_items_clinic_code_uq
  on public.catalog_items (clinic_id, internal_code);
create unique index if not exists catalog_items_clinic_barcode_uq
  on public.catalog_items (clinic_id, barcode)
  where barcode is not null;
create index if not exists catalog_items_clinic_name_idx
  on public.catalog_items (clinic_id, active, name);
create index if not exists catalog_items_clinic_area_idx
  on public.catalog_items (clinic_id, area_id);

drop trigger if exists catalog_items_touch_updated_at on public.catalog_items;
create trigger catalog_items_touch_updated_at
before update on public.catalog_items
for each row execute function public.touch_updated_at();

alter table public.catalog_items enable row level security;
drop policy if exists "catalog_items_select_members" on public.catalog_items;
create policy "catalog_items_select_members"
on public.catalog_items for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.catalog_items from anon, authenticated;
grant select on public.catalog_items to authenticated;

create table if not exists public.stock_movements (
  id uuid primary key,
  clinic_id uuid not null references public.clinics (id) on delete cascade,
  item_id uuid not null references public.catalog_items (id) on delete cascade,
  type text not null,
  quantity_delta integer not null,
  unit_cost_cents bigint,
  reason text,
  sale_id uuid,
  sale_item_id uuid,
  sale_cancellation_id uuid,
  created_at timestamptz not null default now(),
  origin_device_id text,
  constraint stock_movement_type_check
    check (type in ('entry', 'adjustment', 'loss', 'sale', 'cancellation')),
  constraint stock_movement_quantity_check check (quantity_delta <> 0)
);

create index if not exists stock_movements_clinic_item_idx
  on public.stock_movements (clinic_id, item_id, created_at);

alter table public.stock_movements enable row level security;
drop policy if exists "stock_movements_select_members" on public.stock_movements;
create policy "stock_movements_select_members"
on public.stock_movements for select to authenticated
using (public.is_clinic_member(clinic_id));
revoke insert, update, delete on public.stock_movements from anon, authenticated;
grant select on public.stock_movements to authenticated;

create or replace function public.apply_area_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_area jsonb
)
returns setof public.areas
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_existing_clinic uuid;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_area is null or coalesce(p_area ->> 'id', '') = '' then raise exception 'invalid area payload'; end if;
  v_id := (p_area ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select a.* from public.areas a where a.id = v_id and a.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_area ->> 'name', '') = '' then raise exception 'area name is required'; end if;

  select a.clinic_id into v_existing_clinic from public.areas a where a.id = v_id;
  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'area belongs to another clinic';
  end if;

  insert into public.areas (id, clinic_id, name, active, sort_order, deleted_at, origin_device_id)
  values (
    v_id, p_clinic_id, p_area ->> 'name',
    coalesce((p_area ->> 'active')::boolean, true),
    coalesce((p_area ->> 'sort_order')::integer, 0),
    nullif(p_area ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_area ->> 'origin_device_id')
  )
  on conflict (id) do update set
    name = excluded.name,
    active = excluded.active,
    sort_order = excluded.sort_order,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where areas.clinic_id = p_clinic_id;

  if not found then raise exception 'area could not be applied'; end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'area')
  on conflict (operation_id) do nothing;

  return query select a.* from public.areas a where a.id = v_id and a.clinic_id = p_clinic_id;
end;
$$;

create or replace function public.apply_category_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_category jsonb
)
returns setof public.categories
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_parent uuid;
  v_existing_clinic uuid;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_category is null or coalesce(p_category ->> 'id', '') = '' then raise exception 'invalid category payload'; end if;
  v_id := (p_category ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select c.* from public.categories c where c.id = v_id and c.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_category ->> 'name', '') = '' then raise exception 'category name is required'; end if;

  if nullif(p_category ->> 'parent_id', '') is not null then
    v_parent := (p_category ->> 'parent_id')::uuid;
    if not exists (
      select 1 from public.categories c
      where c.id = v_parent and c.clinic_id = p_clinic_id and c.parent_id is null
    ) then
      raise exception 'parent category must be a top-level category in the clinic';
    end if;
  end if;

  select c.clinic_id into v_existing_clinic from public.categories c where c.id = v_id;
  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'category belongs to another clinic';
  end if;

  insert into public.categories (
    id, clinic_id, name, parent_id, scope, active, sort_order, deleted_at, origin_device_id
  )
  values (
    v_id, p_clinic_id, p_category ->> 'name', v_parent,
    coalesce(nullif(p_category ->> 'scope', ''), 'both'),
    coalesce((p_category ->> 'active')::boolean, true),
    coalesce((p_category ->> 'sort_order')::integer, 0),
    nullif(p_category ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_category ->> 'origin_device_id')
  )
  on conflict (id) do update set
    name = excluded.name,
    parent_id = excluded.parent_id,
    scope = excluded.scope,
    active = excluded.active,
    sort_order = excluded.sort_order,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where categories.clinic_id = p_clinic_id;

  if not found then raise exception 'category could not be applied'; end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'category')
  on conflict (operation_id) do nothing;

  return query select c.* from public.categories c where c.id = v_id and c.clinic_id = p_clinic_id;
end;
$$;

create or replace function public.apply_catalog_item_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_item jsonb
)
returns setof public.catalog_items
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_kind text;
  v_existing_clinic uuid;
  v_area uuid;
  v_category uuid;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_item is null or coalesce(p_item ->> 'id', '') = '' then raise exception 'invalid item payload'; end if;
  v_id := (p_item ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select i.* from public.catalog_items i where i.id = v_id and i.clinic_id = p_clinic_id;
    return;
  end if;

  if coalesce(p_item ->> 'name', '') = '' then raise exception 'item name is required'; end if;
  if coalesce(p_item ->> 'internal_code', '') = '' then raise exception 'internal_code is required'; end if;
  v_kind := coalesce(p_item ->> 'kind', '');
  if v_kind not in ('product', 'service') then raise exception 'invalid item kind'; end if;

  v_area := (p_item ->> 'area_id')::uuid;
  if not exists (
    select 1 from public.areas a where a.id = v_area and a.clinic_id = p_clinic_id
  ) then
    raise exception 'area not found in clinic';
  end if;

  if nullif(p_item ->> 'category_id', '') is not null then
    v_category := (p_item ->> 'category_id')::uuid;
    if not exists (
      select 1 from public.categories c where c.id = v_category and c.clinic_id = p_clinic_id
    ) then
      raise exception 'category not found in clinic';
    end if;
  end if;

  select i.clinic_id into v_existing_clinic from public.catalog_items i where i.id = v_id;
  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'item belongs to another clinic';
  end if;

  insert into public.catalog_items (
    id, clinic_id, kind, internal_code, barcode, name, description, category_id,
    brand, unit_cost_cents, sale_price_cents, track_stock, stock_cached, stock_min,
    area_id, lot, expires_on, duration_minutes, active, deleted_at, origin_device_id
  )
  values (
    v_id, p_clinic_id, v_kind, p_item ->> 'internal_code', nullif(p_item ->> 'barcode', ''),
    p_item ->> 'name', nullif(p_item ->> 'description', ''), v_category,
    nullif(p_item ->> 'brand', ''),
    nullif(p_item ->> 'unit_cost_cents', '')::bigint,
    (p_item ->> 'sale_price_cents')::bigint,
    v_kind = 'product',
    0,
    coalesce((p_item ->> 'stock_min')::integer, 0),
    v_area,
    case when v_kind = 'product' then nullif(p_item ->> 'lot', '') end,
    case when v_kind = 'product' then nullif(p_item ->> 'expires_on', '')::date end,
    case when v_kind = 'service' then nullif(p_item ->> 'duration_minutes', '')::integer end,
    coalesce((p_item ->> 'active')::boolean, true),
    nullif(p_item ->> 'deleted_at', '')::timestamptz,
    coalesce(nullif(p_device_id, ''), p_item ->> 'origin_device_id')
  )
  on conflict (id) do update set
    kind = excluded.kind,
    internal_code = excluded.internal_code,
    barcode = excluded.barcode,
    name = excluded.name,
    description = excluded.description,
    category_id = excluded.category_id,
    brand = excluded.brand,
    unit_cost_cents = excluded.unit_cost_cents,
    sale_price_cents = excluded.sale_price_cents,
    track_stock = excluded.track_stock,
    stock_min = excluded.stock_min,
    area_id = excluded.area_id,
    lot = excluded.lot,
    expires_on = excluded.expires_on,
    duration_minutes = excluded.duration_minutes,
    active = excluded.active,
    deleted_at = excluded.deleted_at,
    origin_device_id = excluded.origin_device_id
  where catalog_items.clinic_id = p_clinic_id;

  if not found then raise exception 'item could not be applied'; end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'catalog_item')
  on conflict (operation_id) do nothing;

  return query select i.* from public.catalog_items i where i.id = v_id and i.clinic_id = p_clinic_id;
end;
$$;

create or replace function public.apply_stock_movement_operation(
  p_operation_id uuid,
  p_clinic_id uuid,
  p_device_id text,
  p_movement jsonb
)
returns setof public.stock_movements
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
  v_item uuid;
  v_type text;
  v_delta integer;
  v_existing_clinic uuid;
  v_inserted boolean := false;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if not public.is_clinic_member(p_clinic_id) then raise exception 'not a clinic member'; end if;
  if p_movement is null or coalesce(p_movement ->> 'id', '') = '' then raise exception 'invalid movement payload'; end if;
  v_id := (p_movement ->> 'id')::uuid;

  if exists (select 1 from public.sync_operations where operation_id = p_operation_id) then
    return query select m.* from public.stock_movements m where m.id = v_id and m.clinic_id = p_clinic_id;
    return;
  end if;

  v_item := (p_movement ->> 'item_id')::uuid;
  if not exists (
    select 1 from public.catalog_items i where i.id = v_item and i.clinic_id = p_clinic_id
  ) then
    raise exception 'item not found in clinic';
  end if;

  v_type := p_movement ->> 'type';
  if v_type not in ('entry', 'adjustment', 'loss', 'sale', 'cancellation') then
    raise exception 'invalid movement type';
  end if;

  v_delta := (p_movement ->> 'quantity_delta')::integer;
  if v_delta = 0 then raise exception 'quantity_delta cannot be zero'; end if;
  if v_type in ('entry', 'cancellation') and v_delta < 0 then raise exception 'invalid sign'; end if;
  if v_type in ('loss', 'sale') and v_delta > 0 then raise exception 'invalid sign'; end if;
  if v_type in ('adjustment', 'loss') and coalesce(p_movement ->> 'reason', '') = '' then
    raise exception 'reason is required';
  end if;

  select m.clinic_id into v_existing_clinic from public.stock_movements m where m.id = v_id;
  if v_existing_clinic is not null and v_existing_clinic <> p_clinic_id then
    raise exception 'movement belongs to another clinic';
  end if;

  insert into public.stock_movements (
    id, clinic_id, item_id, type, quantity_delta, unit_cost_cents, reason,
    sale_id, sale_item_id, sale_cancellation_id, created_at, origin_device_id
  )
  values (
    v_id, p_clinic_id, v_item, v_type, v_delta,
    nullif(p_movement ->> 'unit_cost_cents', '')::bigint,
    nullif(p_movement ->> 'reason', ''),
    nullif(p_movement ->> 'sale_id', '')::uuid,
    nullif(p_movement ->> 'sale_item_id', '')::uuid,
    nullif(p_movement ->> 'sale_cancellation_id', '')::uuid,
    coalesce(nullif(p_movement ->> 'created_at', '')::timestamptz, now()),
    coalesce(nullif(p_device_id, ''), p_movement ->> 'origin_device_id')
  )
  on conflict (id) do nothing;
  v_inserted := found;

  if v_inserted then
    update public.catalog_items
    set stock_cached = stock_cached + v_delta
    where id = v_item and clinic_id = p_clinic_id;
  end if;

  insert into public.sync_operations (operation_id, clinic_id, device_id, entity)
  values (p_operation_id, p_clinic_id, p_device_id, 'stock_movement')
  on conflict (operation_id) do nothing;

  return query select m.* from public.stock_movements m where m.id = v_id and m.clinic_id = p_clinic_id;
end;
$$;

revoke all on function public.apply_area_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_category_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_catalog_item_operation(uuid, uuid, text, jsonb) from public;
revoke all on function public.apply_stock_movement_operation(uuid, uuid, text, jsonb) from public;
grant execute on function public.apply_area_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_category_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_catalog_item_operation(uuid, uuid, text, jsonb) to authenticated;
grant execute on function public.apply_stock_movement_operation(uuid, uuid, text, jsonb) to authenticated;
