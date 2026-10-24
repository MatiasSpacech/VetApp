package com.veterapp.data

import androidx.room.withTransaction
import com.veterapp.data.local.AreaCostSettingEntity
import com.veterapp.data.local.AreaEntity
import com.veterapp.data.local.CatalogItemEntity
import com.veterapp.data.local.CategoryEntity
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.StockMovementEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.AreaCostSettingDto
import com.veterapp.data.remote.AreaDto
import com.veterapp.data.remote.CatalogItemDto
import com.veterapp.data.remote.CategoryDto
import com.veterapp.data.remote.StockMovementDto
import com.veterapp.data.remote.toDto
import com.veterapp.data.remote.toEntity
import com.veterapp.domain.Area
import com.veterapp.domain.CatalogItem
import com.veterapp.domain.CatalogItemDraft
import com.veterapp.domain.CatalogKind
import com.veterapp.domain.Category
import com.veterapp.domain.CategoryScope
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.StockMovement
import com.veterapp.domain.StockMovementDraft
import com.veterapp.domain.StockMovementType
import com.veterapp.domain.normalizeBarcode
import com.veterapp.domain.toDomain
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CatalogRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val auditRepository: AuditRepository = AuditRepository(database, settings),
) {
  private val areaDao = database.areaDao()
  private val categoryDao = database.categoryDao()
  private val itemDao = database.catalogItemDao()
  private val movementDao = database.stockMovementDao()
  private val outboxDao = database.outboxDao()
  private val costDao = database.areaCostSettingDao()
  private val json = Json { encodeDefaults = true }

  val areas: Flow<List<Area>> = areaDao.observeAll().map { rows -> rows.map(AreaEntity::toDomain) }

  val activeAreas: Flow<List<Area>> =
    areaDao.observeActive().map { rows -> rows.map(AreaEntity::toDomain) }

  val categories: Flow<List<Category>> =
    categoryDao.observeActive().map { rows -> rows.map(CategoryEntity::toDomain) }

  val items: Flow<List<CatalogItem>> =
    combine(itemDao.observeAll(), areaDao.observeAll(), categoryDao.observeActive()) {
      items,
      areas,
      categories,
      ->
      val areaNames = areas.associate { it.id to it.name }
      val categoryNames = categories.associate { it.id to it.name }
      items.map { item ->
        item.toDomain(
          areaName = areaNames[item.areaId] ?: "Sin área",
          categoryName = item.categoryId?.let { categoryNames[it] },
        )
      }
    }

  val areaCostSettings: Flow<List<AreaCostSettingEntity>> = costDao.observeAll()

  fun observeItem(itemId: String): Flow<CatalogItem?> =
    combine(itemDao.observeById(itemId), areaDao.observeAll(), categoryDao.observeActive()) {
      item,
      areas,
      categories,
      ->
      item?.toDomain(
        areaName = areas.firstOrNull { it.id == item.areaId }?.name ?: "Sin área",
        categoryName = item.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name },
      )
    }

  fun observeMovements(itemId: String): Flow<List<StockMovement>> =
    movementDao.observeForItem(itemId).map { rows -> rows.map(StockMovementEntity::toDomain) }

  suspend fun ensureDefaults(clinicId: String) {
    DEFAULT_AREAS.forEachIndexed { index, name ->
      val id = stableUuid("area", clinicId, name)
      if (areaDao.findById(id) != null) return@forEachIndexed
      val now = System.currentTimeMillis()
      val entity =
        AreaEntity(
          id = id,
          clinicId = clinicId,
          name = name,
          active = true,
          sortOrder = index,
          createdAt = now,
          updatedAt = now,
          revision = 0L,
          deletedAt = null,
          originDeviceId = settings.deviceId,
          pendingSync = true,
        )
      enqueueArea(entity, null, "area.seed", buildJsonObject { put("name", name) })
    }
  }

  suspend fun createArea(name: String): Area {
    val clinicId = settings.currentClinicId
    val clean = name.trim()
    require(clean.isNotBlank()) { "Completá el nombre del área" }
    require(areaDao.findByName(clean) == null || areaDao.findByName(clean)?.deletedAt != null) {
      "Ya existe un área con ese nombre"
    }
    val now = System.currentTimeMillis()
    val entity =
      AreaEntity(
        id = UUID.randomUUID().toString(),
        clinicId = clinicId,
        name = clean,
        active = true,
        sortOrder = areaDao.all().size,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    enqueueArea(entity, null, "area.create", buildJsonObject { put("name", clean) })
    return entity.toDomain()
  }

  suspend fun updateArea(areaId: String, name: String, active: Boolean) {
    val existing = areaDao.findById(areaId) ?: error("El área no existe")
    val clean = name.trim()
    require(clean.isNotBlank()) { "Completá el nombre del área" }
    val duplicate = areaDao.findByName(clean)
    require(duplicate == null || duplicate.id == areaId || duplicate.deletedAt != null) {
      "Ya existe un área con ese nombre"
    }
    if (existing.active && !active) {
      require(itemDao.countActiveForArea(areaId) == 0) {
        "No se puede desactivar un área con ítems activos"
      }
    }
    val updated = existing.copy(name = clean, active = active, updatedAt = System.currentTimeMillis(), pendingSync = true)
    enqueueArea(
      updated,
      existing.revision,
      "area.update",
      buildJsonObject {
        put("name", clean)
        put("active", active)
      },
    )
  }

  suspend fun createCategory(name: String, parentId: String?, scope: CategoryScope): Category {
    val clean = name.trim()
    require(clean.isNotBlank()) { "Completá el nombre de la categoría" }
    if (parentId != null) {
      val parent = categoryDao.findById(parentId) ?: error("La categoría padre no existe")
      require(parent.parentId == null) { "Las categorías admiten solo dos niveles" }
    }
    val now = System.currentTimeMillis()
    val entity =
      CategoryEntity(
        id = UUID.randomUUID().toString(),
        clinicId = settings.currentClinicId,
        name = clean,
        parentId = parentId,
        scope = scope.storage,
        active = true,
        sortOrder = categoryDao.all().size,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    enqueueCategory(
      entity,
      null,
      "category.create",
      buildJsonObject {
        put("name", clean)
        put("parent_id", parentId)
        put("scope", scope.storage)
      },
    )
    return entity.toDomain()
  }

  suspend fun updateCategory(categoryId: String, name: String, active: Boolean) {
    val existing = categoryDao.findById(categoryId) ?: error("La categoría no existe")
    val clean = name.trim()
    require(clean.isNotBlank()) { "Completá el nombre de la categoría" }
    if (existing.active && !active) {
      require(itemDao.countActiveForCategory(categoryId) == 0) {
        "No se puede desactivar una categoría con ítems activos"
      }
    }
    val updated =
      existing.copy(name = clean, active = active, updatedAt = System.currentTimeMillis(), pendingSync = true)
    enqueueCategory(
      updated,
      existing.revision,
      "category.update",
      buildJsonObject {
        put("name", clean)
        put("active", active)
      },
    )
  }

  suspend fun createItem(draft: CatalogItemDraft): CatalogItem {
    draft.validate()?.let { error(it) }
    val clinicId = settings.currentClinicId
    val area = areaDao.findById(draft.areaId) ?: error("El área no existe")
    require(area.active) { "El área está desactivada" }
    require(area.clinicId == clinicId) { "El área pertenece a otra clínica" }
    validateCategory(draft.categoryId, draft.kind)
    val code =
      draft.internalCode.trim().ifBlank {
        val prefix = if (draft.kind == CatalogKind.Product) "PROD" else "SERV"
        "$prefix-${UUID.randomUUID().toString().take(8).uppercase()}"
      }
    require(itemDao.findByInternalCode(code) == null) { "El código interno ya existe" }
    val barcode = normalizeBarcode(draft.barcode)
    barcode?.let { require(itemDao.findByBarcode(it) == null) { "El código de barras ya existe" } }
    val now = System.currentTimeMillis()
    val entity =
      CatalogItemEntity(
        id = UUID.randomUUID().toString(),
        clinicId = clinicId,
        kind = draft.kind.storage,
        internalCode = code,
        barcode = barcode,
        name = draft.name.trim(),
        description = draft.description?.trim()?.takeIf { it.isNotBlank() },
        categoryId = draft.categoryId,
        brand = draft.brand?.trim()?.takeIf { it.isNotBlank() },
        unitCostCents = draft.unitCostCents,
        salePriceCents = draft.salePriceCents,
        trackStock = draft.kind == CatalogKind.Product,
        stockCached = if (draft.kind == CatalogKind.Product) draft.initialStock else 0,
        stockMin = if (draft.kind == CatalogKind.Product) draft.stockMin else 0,
        areaId = draft.areaId,
        lot = draft.lot?.trim()?.takeIf { it.isNotBlank() },
        expiresOn = draft.expiresOn,
        durationMinutes = draft.durationMinutes,
        active = draft.active,
        createdAt = now,
        updatedAt = now,
        revision = 0L,
        deletedAt = null,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    val initial =
      if (entity.kind == CatalogKind.Product.storage && draft.initialStock > 0) {
        StockMovementEntity(
          id = UUID.randomUUID().toString(),
          clinicId = clinicId,
          itemId = entity.id,
          type = StockMovementType.Entry.storage,
          quantityDelta = draft.initialStock,
          unitCostCents = draft.unitCostCents,
          reason = "Stock inicial",
          saleId = null,
          saleItemId = null,
          saleCancellationId = null,
          createdAt = now,
          originDeviceId = settings.deviceId,
          pendingSync = true,
        )
      } else {
        null
      }
    database.withTransaction {
      itemDao.upsert(entity)
      outboxDao.insert(outbox("catalog_item", entity.id, json.encodeToString(entity.toDto()), null, now))
      initial?.let { movement ->
        movementDao.insert(movement)
        outboxDao.insert(
          outbox(
            "stock_movement",
            movement.id,
            json.encodeToString(movement.toDto()),
            null,
            now,
          ),
        )
      }
      auditRepository.record(
        entity = "catalog_item",
        entityId = entity.id,
        action = "catalog.create",
        payload =
          buildJsonObject {
            put("name", entity.name)
            put("kind", entity.kind)
            put("price_cents", entity.salePriceCents)
          },
        clinicId = clinicId,
      )
    }
    return entity.toDomain(area.name, categoryName(entity.categoryId))
  }

  suspend fun updateItem(itemId: String, draft: CatalogItemDraft) {
    draft.validate()?.let { error(it) }
    val existing = itemDao.findById(itemId) ?: error("El ítem no existe")
    val area = areaDao.findById(draft.areaId) ?: error("El área no existe")
    require(area.active) { "El área está desactivada" }
    validateCategory(draft.categoryId, CatalogKind.fromStorage(existing.kind))
    val code = draft.internalCode.trim()
    require(code.isNotBlank()) { "El código interno es obligatorio" }
    val duplicateCode = itemDao.findByInternalCode(code)
    require(duplicateCode == null || duplicateCode.id == itemId) { "El código interno ya existe" }
    val barcode = normalizeBarcode(draft.barcode)
    val duplicateBarcode = barcode?.let { itemDao.findByBarcode(it) }
    require(duplicateBarcode == null || duplicateBarcode.id == itemId) {
      "El código de barras ya existe"
    }
    val updated =
      existing.copy(
        internalCode = code,
        barcode = barcode,
        name = draft.name.trim(),
        description = draft.description?.trim()?.takeIf { it.isNotBlank() },
        categoryId = draft.categoryId,
        brand = draft.brand?.trim()?.takeIf { it.isNotBlank() },
        unitCostCents = draft.unitCostCents,
        salePriceCents = draft.salePriceCents,
        stockMin = if (existing.trackStock) draft.stockMin else 0,
        areaId = draft.areaId,
        lot = draft.lot?.trim()?.takeIf { it.isNotBlank() },
        expiresOn = draft.expiresOn,
        durationMinutes = draft.durationMinutes,
        active = draft.active,
        updatedAt = System.currentTimeMillis(),
        pendingSync = true,
      )
    val changed =
      buildList {
        if (existing.name != updated.name) add("nombre")
        if (existing.salePriceCents != updated.salePriceCents) add("precio")
        if (existing.unitCostCents != updated.unitCostCents) add("costo")
        if (existing.areaId != updated.areaId) add("área")
        if (existing.categoryId != updated.categoryId) add("categoría")
        if (existing.active != updated.active) add("estado")
      }
    database.withTransaction {
      itemDao.upsert(updated)
      outboxDao.insert(
        outbox(
          "catalog_item",
          updated.id,
          json.encodeToString(updated.toDto()),
          existing.revision,
          updated.updatedAt,
        ),
      )
      auditRepository.record(
        entity = "catalog_item",
        entityId = updated.id,
        action = if (existing.salePriceCents != updated.salePriceCents) "catalog.price" else "catalog.update",
        payload =
          buildJsonObject {
            put("name", updated.name)
            put("changed", changed.joinToString(", "))
            put("price_cents", updated.salePriceCents)
          },
        clinicId = updated.clinicId,
      )
    }
  }

  suspend fun setItemActive(itemId: String, active: Boolean) {
    val existing = itemDao.findById(itemId) ?: error("El ítem no existe")
    if (existing.active == active) return
    val updated = existing.copy(active = active, updatedAt = System.currentTimeMillis(), pendingSync = true)
    database.withTransaction {
      itemDao.upsert(updated)
      outboxDao.insert(outbox("catalog_item", updated.id, json.encodeToString(updated.toDto()), existing.revision, updated.updatedAt))
      auditRepository.record(
        entity = "catalog_item",
        entityId = updated.id,
        action = if (active) "catalog.activate" else "catalog.deactivate",
        payload = buildJsonObject { put("name", updated.name) },
        clinicId = updated.clinicId,
      )
    }
  }

  suspend fun addMovement(itemId: String, draft: StockMovementDraft): StockMovement {
    val item = itemDao.findById(itemId) ?: error("El ítem no existe")
    draft.validate(item.trackStock)?.let { error(it) }
    val delta = draft.delta()
    val resulting = item.stockCached + delta
    if (resulting < 0) {
      require(settings.allowNegativeStock) { "Stock insuficiente para ${item.name}" }
      auditRepository.record(
        entity = "catalog_item",
        entityId = item.id,
        action = "stock.negative",
        payload =
          buildJsonObject {
            put("name", item.name)
            put("stock", item.stockCached)
            put("delta", delta)
          },
        clinicId = item.clinicId,
      )
    }
    val now = System.currentTimeMillis()
    val movement =
      StockMovementEntity(
        id = UUID.randomUUID().toString(),
        clinicId = item.clinicId,
        itemId = item.id,
        type = draft.type.storage,
        quantityDelta = delta,
        unitCostCents = draft.unitCostCents,
        reason = draft.reason?.trim()?.takeIf { it.isNotBlank() },
        saleId = null,
        saleItemId = null,
        saleCancellationId = null,
        createdAt = now,
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    val updated = item.copy(stockCached = resulting, updatedAt = now, pendingSync = true)
    val action =
      when (draft.type) {
        StockMovementType.Entry -> "stock.entry"
        StockMovementType.Adjustment -> "stock.adjustment"
        StockMovementType.Loss -> "stock.loss"
        else -> "stock.movement"
      }
    database.withTransaction {
      movementDao.insert(movement)
      itemDao.upsert(updated)
      outboxDao.insert(outbox("stock_movement", movement.id, json.encodeToString(movement.toDto()), null, now))
      outboxDao.insert(outbox("catalog_item", updated.id, json.encodeToString(updated.toDto()), item.revision, now))
      auditRepository.record(
        entity = "stock_movement",
        entityId = movement.id,
        action = action,
        payload =
          buildJsonObject {
            put("item", item.name)
            put("delta", delta)
            put("stock", resulting)
            put("reason", movement.reason)
          },
        clinicId = item.clinicId,
      )
    }
    return movement.toDomain()
  }

  suspend fun setAreaCost(areaId: String, percentBp: Int, validFrom: LocalDate) {
    require(percentBp in 0..10_000) { "El porcentaje debe estar entre 0 y 100" }
    val area = areaDao.findById(areaId) ?: error("El área no existe")
    val clinicId = area.clinicId
    val entity =
      AreaCostSettingEntity(
        id = UUID.randomUUID().toString(),
        clinicId = clinicId,
        areaId = areaId,
        expensesPercentBp = percentBp,
        validFrom = validFrom.toString(),
        createdAt = System.currentTimeMillis(),
        originDeviceId = settings.deviceId,
        pendingSync = true,
      )
    database.withTransaction {
      costDao.insert(entity)
      outboxDao.insert(outbox("area_cost", entity.id, json.encodeToString(entity.toDto()), null, entity.createdAt))
      auditRepository.record(
        entity = "area_cost",
        entityId = entity.id,
        action = "area.cost.update",
        payload =
          buildJsonObject {
            put("area", area.name)
            put("percent_bp", percentBp)
            put("valid_from", validFrom.toString())
          },
        clinicId = clinicId,
      )
    }
  }

  suspend fun reconcileStock(clinicId: String) {
    itemDao.all()
      .filter { it.clinicId == clinicId && it.trackStock }
      .forEach { item ->
        val sum = movementDao.sumForItem(item.id)
        if (sum != item.stockCached) {
          itemDao.upsert(item.copy(stockCached = sum, updatedAt = System.currentTimeMillis(), pendingSync = true))
          auditRepository.record(
            entity = "catalog_item",
            entityId = item.id,
            action = "stock.recalculate",
            payload =
              buildJsonObject {
                put("from", item.stockCached)
                put("to", sum)
              },
            clinicId = clinicId,
          )
        }
      }
  }

  suspend fun upsertAreaFromRemote(dto: AreaDto, clinicId: String) {
    val existing = areaDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    areaDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markAreaSynced(dto: AreaDto, clinicId: String) {
    val existing = areaDao.findById(dto.id)
    areaDao.upsert(dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId).copy(pendingSync = false))
  }

  suspend fun upsertCategoryFromRemote(dto: CategoryDto, clinicId: String) {
    val existing = categoryDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    categoryDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markCategorySynced(dto: CategoryDto, clinicId: String) {
    val existing = categoryDao.findById(dto.id)
    categoryDao.upsert(
      dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId).copy(pendingSync = false),
    )
  }

  suspend fun upsertItemFromRemote(dto: CatalogItemDto, clinicId: String) {
    val existing = itemDao.findById(dto.id)
    if (existing?.pendingSync == true) return
    itemDao.upsert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun markItemSynced(dto: CatalogItemDto, clinicId: String) {
    val existing = itemDao.findById(dto.id)
    val remote = dto.toEntity(clinicId, existing?.originDeviceId ?: settings.deviceId)
    itemDao.upsert(
      remote.copy(
        pendingSync = false,
        stockCached = existing?.stockCached ?: remote.stockCached,
      ),
    )
  }

  suspend fun upsertMovementFromRemote(dto: StockMovementDto, clinicId: String) {
    movementDao.insert(dto.toEntity(clinicId, settings.deviceId))
  }

  suspend fun upsertAreaCostFromRemote(dto: AreaCostSettingDto, clinicId: String) {
    val existing = costDao.all().firstOrNull { it.id == dto.id }
    if (existing?.pendingSync == true) return
    costDao.insert(dto.toEntity(clinicId, settings.deviceId))
  }

  private suspend fun validateCategory(categoryId: String?, kind: CatalogKind) {
    if (categoryId == null) return
    val category = categoryDao.findById(categoryId) ?: error("La categoría no existe")
    require(category.active) { "La categoría está desactivada" }
    require(CategoryScope.fromStorage(category.scope).accepts(kind)) {
      "La categoría no corresponde al tipo de ítem"
    }
    if (category.parentId != null) {
      val parent = categoryDao.findById(category.parentId)
      require(parent != null && CategoryScope.fromStorage(parent.scope).accepts(kind)) {
        "La subcategoría no corresponde al tipo de ítem"
      }
    }
  }

  private suspend fun categoryName(categoryId: String?): String? =
    categoryId?.let { categoryDao.findById(it)?.name }

  private suspend fun enqueueArea(
    entity: AreaEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    database.withTransaction {
      areaDao.upsert(entity)
      outboxDao.insert(outbox("area", entity.id, json.encodeToString(entity.toDto()), baseRevision, System.currentTimeMillis()))
      if (action != "area.seed") {
        auditRepository.record(
          entity = "area",
          entityId = entity.id,
          action = action,
          payload = payload,
          clinicId = entity.clinicId,
        )
      }
    }
  }

  private suspend fun enqueueCategory(
    entity: CategoryEntity,
    baseRevision: Long?,
    action: String,
    payload: JsonObject,
  ) {
    database.withTransaction {
      categoryDao.upsert(entity)
      outboxDao.insert(outbox("category", entity.id, json.encodeToString(entity.toDto()), baseRevision, System.currentTimeMillis()))
      auditRepository.record(
        entity = "category",
        entityId = entity.id,
        action = action,
        payload = payload,
        clinicId = entity.clinicId,
      )
    }
  }

  private fun outbox(
    entityType: String,
    entityId: String,
    payload: String,
    baseRevision: Long?,
    now: Long,
  ): OutboxEntity =
    OutboxEntity(
      operationId = UUID.randomUUID().toString(),
      entityType = entityType,
      entityId = entityId,
      operation = "UPSERT",
      payload = payload,
      baseRevision = baseRevision,
      attempts = 0,
      nextAttemptAt = 0L,
      lastError = null,
      createdAt = now,
    )

  companion object {
    val DEFAULT_AREAS =
      listOf("Consultorio", "Farmacia", "Pet shop", "Peluquería", "Alimentos")

    fun stableUuid(vararg parts: String): String =
      UUID.nameUUIDFromBytes(parts.joinToString(":").toByteArray(StandardCharsets.UTF_8)).toString()
  }
}
