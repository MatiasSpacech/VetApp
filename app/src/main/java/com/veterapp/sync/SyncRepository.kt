package com.veterapp.sync

import com.veterapp.data.AppSettings
import com.veterapp.data.AppointmentRepository
import com.veterapp.data.AuthRepository
import com.veterapp.data.CatalogRepository
import com.veterapp.data.ClientRepository
import com.veterapp.data.ClinicalRepository
import com.veterapp.data.CommerceRepository
import com.veterapp.data.PetRepository
import com.veterapp.data.VaccinationRepository
import com.veterapp.data.local.OutboxEntity
import com.veterapp.data.local.VetDatabase
import com.veterapp.data.remote.AppointmentDto
import com.veterapp.data.remote.AreaCostSettingDto
import com.veterapp.data.remote.AreaDto
import com.veterapp.data.remote.CashMovementDto
import com.veterapp.data.remote.CashSessionDto
import com.veterapp.data.remote.CatalogItemDto
import com.veterapp.data.remote.CategoryDto
import com.veterapp.data.remote.ClientDto
import com.veterapp.data.remote.ConsultationAddendumDto
import com.veterapp.data.remote.ConsultationDto
import com.veterapp.data.remote.PaymentDto
import com.veterapp.data.remote.PaymentMethodDto
import com.veterapp.data.remote.PetDto
import com.veterapp.data.remote.PetWeightDto
import com.veterapp.data.remote.SaleCancellationDto
import com.veterapp.data.remote.SaleDto
import com.veterapp.data.remote.SaleItemDto
import com.veterapp.data.remote.StockMovementDto
import com.veterapp.data.remote.SupabaseClientProvider
import com.veterapp.data.remote.VaccinationDto
import com.veterapp.util.isNetworkFailure
import com.veterapp.util.userMessage
import io.github.jan.supabase.postgrest.postgrest
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class SyncRepository(
  private val database: VetDatabase,
  private val settings: AppSettings,
  private val clientProvider: SupabaseClientProvider,
  private val authRepository: AuthRepository,
  private val petRepository: PetRepository,
  private val clientRepository: ClientRepository,
  private val clinicalRepository: ClinicalRepository,
  private val vaccinationRepository: VaccinationRepository,
  private val appointmentRepository: AppointmentRepository,
  private val catalogRepository: CatalogRepository,
  private val commerceRepository: CommerceRepository,
) {
  private val outboxDao = database.outboxDao()
  private val json = Json { ignoreUnknownKeys = true }

  private val _lastSyncAt = MutableStateFlow(settings.lastSyncAt)
  val lastSyncAt: StateFlow<Long> = _lastSyncAt.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  val lastError: StateFlow<String?> = _lastError.asStateFlow()

  suspend fun syncNow(): SyncResult {
    if (!clientProvider.isConfigured || clientProvider.client == null) return SyncResult.NotConfigured
    if (authRepository.currentUserId() == null) return SyncResult.NotAuthenticated

    _lastError.value = null
    return try {
      val clinicId = authRepository.ensureClinicId()
      catalogRepository.ensureDefaults(clinicId)
      commerceRepository.ensureDefaults(clinicId)
      pushPending(clinicId)
      pullClients(clinicId)
      pullPets(clinicId)
      pullPetWeights(clinicId)
      pullConsultations(clinicId)
      pullAddenda(clinicId)
      pullVaccinations(clinicId)
      pullAppointments(clinicId)
      pullAreas(clinicId)
      pullCategories(clinicId)
      pullCatalogItems(clinicId)
      pullStockMovements(clinicId)
      pullPaymentMethods(clinicId)
      pullAreaCosts(clinicId)
      pullCashSessions(clinicId)
      pullSales(clinicId)
      pullSaleItems(clinicId)
      pullPayments(clinicId)
      pullSaleCancellations(clinicId)
      pullCashMovements(clinicId)
      catalogRepository.reconcileStock(clinicId)
      val now = System.currentTimeMillis()
      settings.lastSyncAt = now
      _lastSyncAt.value = now
      _lastError.value = null
      SyncResult.Success
    } catch (error: Throwable) {
      val message = error.toSyncMessage()
      _lastError.value = message
      SyncResult.Failure(message)
    }
  }

  private suspend fun pushPending(clinicId: String) {
    val items = outboxDao.due(System.currentTimeMillis(), limit = 200)
    for (item in items) {
      try {
        when (item.entityType) {
          "client" -> applyClientOperation(item, clinicId)
          "pet" -> applyPetOperation(item, clinicId)
          "pet_weight" -> applyPetWeightOperation(item, clinicId)
          "consultation" -> applyConsultationOperation(item, clinicId)
          "consultation_addendum" -> applyAddendumOperation(item, clinicId)
          "vaccination" -> applyVaccinationOperation(item, clinicId)
          "appointment" -> applyAppointmentOperation(item, clinicId)
          "area" -> applyAreaOperation(item, clinicId)
          "category" -> applyCategoryOperation(item, clinicId)
          "catalog_item" -> applyCatalogItemOperation(item, clinicId)
          "stock_movement" -> applyStockMovementOperation(item, clinicId)
          "payment_method" -> applyPaymentMethodOperation(item, clinicId)
          "area_cost" -> applyAreaCostOperation(item, clinicId)
          "cash_session" -> applyCashSessionOperation(item, clinicId)
          "sale" -> applySaleOperation(item, clinicId)
          "sale_cancellation" -> applySaleCancellationOperation(item, clinicId)
          "cash_movement" -> applyCashMovementOperation(item, clinicId)
          else -> error("Entidad de sincronización desconocida: ${item.entityType}")
        }
        outboxDao.delete(item.operationId)
      } catch (error: Throwable) {
        val attempts = item.attempts + 1
        val backoff = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl min(attempts, 6))
        outboxDao.update(
          item.copy(
            attempts = attempts,
            nextAttemptAt = System.currentTimeMillis() + backoff,
            lastError = error.toSyncMessage(),
          ),
        )
        throw error
      }
    }
  }

  private suspend fun applyPetOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_pet_operation",
          rpcParameters(item, clinicId, "p_pet", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<PetDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la mascota")
    petRepository.markSynced(saved, clinicId)
  }

  private suspend fun applyPetWeightOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_pet_weight_operation",
          rpcParameters(item, clinicId, "p_weight", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<PetWeightDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el peso")
    petRepository.markWeightSynced(saved, clinicId)
  }

  private suspend fun applyClientOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_client_operation",
          rpcParameters(item, clinicId, "p_client", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<ClientDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el cliente")
    clientRepository.markSynced(saved, clinicId)
  }

  private suspend fun applyConsultationOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_consultation_operation",
          rpcParameters(item, clinicId, "p_consultation", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<ConsultationDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la consulta")
    clinicalRepository.markConsultationSynced(saved, clinicId)
  }

  private suspend fun applyAddendumOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_addendum_operation",
          rpcParameters(item, clinicId, "p_addendum", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<ConsultationAddendumDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el addendum")
    clinicalRepository.markAddendumSynced(saved, clinicId)
  }

  private suspend fun applyVaccinationOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_vaccination_operation",
          rpcParameters(item, clinicId, "p_vaccination", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<VaccinationDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la vacunación")
    vaccinationRepository.markSynced(saved, clinicId)
  }

  private suspend fun applyAppointmentOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_appointment_operation",
          rpcParameters(item, clinicId, "p_appointment", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<AppointmentDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el turno")
    appointmentRepository.markSynced(saved, clinicId)
  }

  private suspend fun applyAreaOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_area_operation",
          rpcParameters(item, clinicId, "p_area", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<AreaDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el área")
    catalogRepository.markAreaSynced(saved, clinicId)
  }

  private suspend fun applyCategoryOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_category_operation",
          rpcParameters(item, clinicId, "p_category", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<CategoryDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la categoría")
    catalogRepository.markCategorySynced(saved, clinicId)
  }

  private suspend fun applyCatalogItemOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_catalog_item_operation",
          rpcParameters(item, clinicId, "p_item", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<CatalogItemDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el ítem")
    catalogRepository.markItemSynced(saved, clinicId)
  }

  private suspend fun applyStockMovementOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_stock_movement_operation",
          rpcParameters(item, clinicId, "p_movement", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<StockMovementDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el movimiento")
    database.stockMovementDao().markSynced(saved.id)
  }

  private suspend fun applyPaymentMethodOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_payment_method_operation",
          rpcParameters(item, clinicId, "p_method", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<PaymentMethodDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el medio de pago")
    commerceRepository.markPaymentMethodSynced(saved, clinicId)
  }

  private suspend fun applyAreaCostOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_area_cost_operation",
          rpcParameters(item, clinicId, "p_setting", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<AreaCostSettingDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el porcentaje")
    database.areaCostSettingDao().markSynced(saved.id)
  }

  private suspend fun applyCashSessionOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_cash_session_operation",
          rpcParameters(item, clinicId, "p_session", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<CashSessionDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la caja")
    commerceRepository.markCashSessionSynced(saved, clinicId)
  }

  private suspend fun applySaleOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val payload = json.parseToJsonElement(item.payload).jsonObject
    val rows =
      client.postgrest
        .rpc(
          "apply_sale_operation",
          rpcParameters(item, clinicId, "p_sale", payload),
        )
        .decodeList<SaleDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la venta")
    database.saleDao().markSynced(saved.id)
    database.saleItemDao().markSyncedForSale(saved.id)
    database.paymentDao().markSyncedForSale(saved.id)
    database.stockMovementDao().markSyncedForSale(saved.id)
    database.cashMovementDao().markSyncedForSale(saved.id)
  }

  private suspend fun applySaleCancellationOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val payload = json.parseToJsonElement(item.payload).jsonObject
    val rows =
      client.postgrest
        .rpc(
          "apply_sale_cancellation_operation",
          rpcParameters(item, clinicId, "p_cancellation", payload),
        )
        .decodeList<SaleCancellationDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió la anulación")
    val saleId = payload["sale"]?.jsonObject?.get("id")?.toString()?.trim('"')
    database.saleCancellationDao().markSynced(saved.id)
    if (saleId != null) {
      database.saleDao().markSynced(saleId)
      database.stockMovementDao().markSyncedForSale(saleId)
      database.cashMovementDao().markSyncedForSale(saleId)
    }
  }

  private suspend fun applyCashMovementOperation(item: OutboxEntity, clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .rpc(
          "apply_cash_movement_operation",
          rpcParameters(item, clinicId, "p_movement", json.parseToJsonElement(item.payload).jsonObject),
        )
        .decodeList<CashMovementDto>()
    val saved = rows.firstOrNull() ?: error("El servidor no devolvió el movimiento de caja")
    database.cashMovementDao().markSynced(saved.id)
  }

  private suspend fun pullClients(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("clients")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<ClientDto>()
    rows.forEach { clientRepository.upsertFromRemote(it, clinicId) }
  }

  private suspend fun pullPets(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("pets")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<PetDto>()
    rows.forEach { petRepository.upsertFromRemote(it, clinicId) }
  }

  private suspend fun pullPetWeights(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("pet_weights")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<PetWeightDto>()
    rows.forEach { petRepository.upsertWeightFromRemote(it, clinicId) }
  }

  private suspend fun pullConsultations(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("consultations")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<ConsultationDto>()
    rows.forEach { clinicalRepository.upsertConsultationFromRemote(it, clinicId) }
  }

  private suspend fun pullAddenda(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("consultation_addenda")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<ConsultationAddendumDto>()
    rows.forEach { clinicalRepository.upsertAddendumFromRemote(it, clinicId) }
  }

  private suspend fun pullVaccinations(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("vaccinations")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<VaccinationDto>()
    rows.forEach { vaccinationRepository.upsertFromRemote(it, clinicId) }
  }

  private suspend fun pullAppointments(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("appointments")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<AppointmentDto>()
    rows.forEach { appointmentRepository.upsertFromRemote(it, clinicId) }
  }

  private suspend fun pullAreas(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("areas")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<AreaDto>()
    rows.forEach { catalogRepository.upsertAreaFromRemote(it, clinicId) }
  }

  private suspend fun pullCategories(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("categories")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<CategoryDto>()
    rows.forEach { catalogRepository.upsertCategoryFromRemote(it, clinicId) }
  }

  private suspend fun pullCatalogItems(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("catalog_items")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<CatalogItemDto>()
    rows.forEach { catalogRepository.upsertItemFromRemote(it, clinicId) }
  }

  private suspend fun pullStockMovements(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("stock_movements")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<StockMovementDto>()
    rows.forEach { catalogRepository.upsertMovementFromRemote(it, clinicId) }
  }

  private suspend fun pullPaymentMethods(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("payment_methods")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<PaymentMethodDto>()
    rows.forEach { commerceRepository.upsertPaymentMethodFromRemote(it, clinicId) }
  }

  private suspend fun pullAreaCosts(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("area_cost_settings")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<AreaCostSettingDto>()
    rows.forEach { commerceRepository.upsertAreaCostFromRemote(it, clinicId) }
  }

  private suspend fun pullCashSessions(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("cash_sessions")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<CashSessionDto>()
    rows.forEach { commerceRepository.upsertCashSessionFromRemote(it, clinicId) }
  }

  private suspend fun pullSales(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("sales")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<SaleDto>()
    rows.forEach { commerceRepository.upsertSaleFromRemote(it, clinicId) }
  }

  private suspend fun pullSaleItems(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("sale_items")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<SaleItemDto>()
    rows.forEach { commerceRepository.upsertSaleItemFromRemote(it, clinicId) }
  }

  private suspend fun pullPayments(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("payments")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<PaymentDto>()
    rows.forEach { commerceRepository.upsertPaymentFromRemote(it, clinicId) }
  }

  private suspend fun pullSaleCancellations(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("sale_cancellations")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<SaleCancellationDto>()
    rows.forEach { commerceRepository.upsertCancellationFromRemote(it, clinicId) }
  }

  private suspend fun pullCashMovements(clinicId: String) {
    val client = requireClient()
    val rows =
      client.postgrest
        .from("cash_movements")
        .select { filter { eq("clinic_id", clinicId) } }
        .decodeList<CashMovementDto>()
    rows.forEach { commerceRepository.upsertCashMovementFromRemote(it, clinicId) }
  }

  private fun requireClient() =
    clientProvider.client ?: error("Supabase no está configurado")

  private fun rpcParameters(
    item: OutboxEntity,
    clinicId: String,
    extraKey: String? = null,
    extraValue: kotlinx.serialization.json.JsonElement? = null,
  ): JsonObject =
    buildJsonObject {
      put("p_operation_id", item.operationId)
      put("p_clinic_id", clinicId)
      put("p_device_id", settings.deviceId)
      if (extraKey != null && extraValue != null) put(extraKey, extraValue)
    }

  sealed interface SyncResult {
    data object Success : SyncResult

    data object NotConfigured : SyncResult

    data object NotAuthenticated : SyncResult

    data class Failure(val message: String) : SyncResult
  }

  private fun Throwable.toSyncMessage(): String =
    if (isNetworkFailure()) "Sin conexión con Supabase"
    else userMessage("No se pudo sincronizar")

  private companion object {
    const val BASE_BACKOFF_MS = 5_000L
    const val MAX_BACKOFF_MS = 5 * 60_000L
  }
}
