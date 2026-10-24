package com.veterapp.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TotalsRow(
  val grossCents: Long,
  val discountCents: Long,
  val netCents: Long,
  val productCostCents: Long,
  val serviceCostCents: Long,
  val expensesCents: Long,
)

data class AreaTotalsRow(
  val areaId: String,
  val areaName: String,
  val grossCents: Long,
  val discountCents: Long,
  val netCents: Long,
  val productCostCents: Long,
  val serviceCostCents: Long,
  val expensesCents: Long,
)

data class MethodTotalsRow(
  val methodId: String,
  val methodName: String,
  val amountCents: Long,
  val saleCount: Int,
)

data class TopItemRow(
  val itemId: String,
  val itemName: String,
  val itemKind: String,
  val quantity: Long,
  val netCents: Long,
)

data class DaySeriesRow(
  val day: String,
  val netCents: Long,
  val productCostCents: Long,
  val serviceCostCents: Long,
  val expensesCents: Long,
  val saleCount: Int,
)

data class StatusCountRow(
  val status: String,
  val total: Int,
)

@Dao
interface ReportDao {
  @Query(
    "SELECT COALESCE(SUM(si.grossCents), 0) AS grossCents, " +
      "COALESCE(SUM(si.discountCents), 0) AS discountCents, " +
      "COALESCE(SUM(si.netCents), 0) AS netCents, " +
      "COALESCE(SUM(CASE WHEN si.itemKind = 'product' THEN si.quantity * COALESCE(si.unitCostCents, 0) ELSE 0 END), 0) AS productCostCents, " +
      "COALESCE(SUM(CASE WHEN si.itemKind = 'service' THEN si.quantity * COALESCE(si.unitCostCents, 0) ELSE 0 END), 0) AS serviceCostCents, " +
      "COALESCE(SUM(si.netCents * si.expensesPercentBp / 10000), 0) AS expensesCents " +
      "FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
      "WHERE s.status = 'completed' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:areaId IS NULL OR si.areaId = :areaId) " +
      "AND (:methodId IS NULL OR EXISTS (" +
      "SELECT 1 FROM payments p WHERE p.saleId = s.id AND p.methodId = :methodId)) " +
      "AND (:kind IS NULL OR si.itemKind = :kind)",
  )
  suspend fun totals(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
  ): TotalsRow

  @Query(
    "SELECT COUNT(DISTINCT s.id) FROM sales s " +
      "WHERE s.status = 'completed' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:areaId IS NULL OR EXISTS (" +
      "SELECT 1 FROM sale_items si WHERE si.saleId = s.id AND si.areaId = :areaId)) " +
      "AND (:methodId IS NULL OR EXISTS (" +
      "SELECT 1 FROM payments p WHERE p.saleId = s.id AND p.methodId = :methodId)) " +
      "AND (:kind IS NULL OR EXISTS (" +
      "SELECT 1 FROM sale_items si WHERE si.saleId = s.id AND si.itemKind = :kind))",
  )
  suspend fun completedCount(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
  ): Int

  @Query(
    "SELECT COUNT(DISTINCT s.id) FROM sales s " +
      "WHERE s.status = 'cancelled' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:areaId IS NULL OR EXISTS (" +
      "SELECT 1 FROM sale_items si WHERE si.saleId = s.id AND si.areaId = :areaId)) " +
      "AND (:methodId IS NULL OR EXISTS (" +
      "SELECT 1 FROM payments p WHERE p.saleId = s.id AND p.methodId = :methodId)) " +
      "AND (:kind IS NULL OR EXISTS (" +
      "SELECT 1 FROM sale_items si WHERE si.saleId = s.id AND si.itemKind = :kind))",
  )
  suspend fun cancelledCount(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
  ): Int

  @Query(
    "SELECT si.areaId AS areaId, si.areaName AS areaName, " +
      "COALESCE(SUM(si.grossCents), 0) AS grossCents, " +
      "COALESCE(SUM(si.discountCents), 0) AS discountCents, " +
      "COALESCE(SUM(si.netCents), 0) AS netCents, " +
      "COALESCE(SUM(CASE WHEN si.itemKind = 'product' THEN si.quantity * COALESCE(si.unitCostCents, 0) ELSE 0 END), 0) AS productCostCents, " +
      "COALESCE(SUM(CASE WHEN si.itemKind = 'service' THEN si.quantity * COALESCE(si.unitCostCents, 0) ELSE 0 END), 0) AS serviceCostCents, " +
      "COALESCE(SUM(si.netCents * si.expensesPercentBp / 10000), 0) AS expensesCents " +
      "FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
      "WHERE s.status = 'completed' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:areaId IS NULL OR si.areaId = :areaId) " +
      "AND (:methodId IS NULL OR EXISTS (" +
      "SELECT 1 FROM payments p WHERE p.saleId = s.id AND p.methodId = :methodId)) " +
      "AND (:kind IS NULL OR si.itemKind = :kind) " +
      "GROUP BY si.areaId, si.areaName ORDER BY netCents DESC",
  )
  suspend fun areaTotals(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
  ): List<AreaTotalsRow>

  @Query(
    "SELECT p.methodId AS methodId, COALESCE(m.name, 'Medio') AS methodName, " +
      "COALESCE(SUM(p.amountCents), 0) AS amountCents, COUNT(DISTINCT p.saleId) AS saleCount " +
      "FROM payments p INNER JOIN sales s ON s.id = p.saleId " +
      "LEFT JOIN payment_methods m ON m.id = p.methodId " +
      "WHERE s.status = 'completed' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:methodId IS NULL OR p.methodId = :methodId) " +
      "AND (:areaId IS NULL OR EXISTS (" +
      "SELECT 1 FROM sale_items si WHERE si.saleId = s.id AND si.areaId = :areaId)) " +
      "AND (:kind IS NULL OR EXISTS (" +
      "SELECT 1 FROM sale_items si WHERE si.saleId = s.id AND si.itemKind = :kind)) " +
      "GROUP BY p.methodId, m.name ORDER BY amountCents DESC",
  )
  suspend fun methodTotals(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
  ): List<MethodTotalsRow>

  @Query(
    "SELECT si.itemId AS itemId, si.itemName AS itemName, si.itemKind AS itemKind, " +
      "COALESCE(SUM(si.quantity), 0) AS quantity, COALESCE(SUM(si.netCents), 0) AS netCents " +
      "FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
      "WHERE s.status = 'completed' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:areaId IS NULL OR si.areaId = :areaId) " +
      "AND (:methodId IS NULL OR EXISTS (" +
      "SELECT 1 FROM payments p WHERE p.saleId = s.id AND p.methodId = :methodId)) " +
      "AND (:kind IS NULL OR si.itemKind = :kind) " +
      "GROUP BY si.itemId, si.itemName, si.itemKind ORDER BY quantity DESC, netCents DESC LIMIT :limit",
  )
  suspend fun topItems(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
    limit: Int,
  ): List<TopItemRow>

  @Query(
    "SELECT date(s.soldAt / 1000, 'unixepoch', '-3 hours') AS day, " +
      "COALESCE(SUM(si.netCents), 0) AS netCents, " +
      "COALESCE(SUM(CASE WHEN si.itemKind = 'product' THEN si.quantity * COALESCE(si.unitCostCents, 0) ELSE 0 END), 0) AS productCostCents, " +
      "COALESCE(SUM(CASE WHEN si.itemKind = 'service' THEN si.quantity * COALESCE(si.unitCostCents, 0) ELSE 0 END), 0) AS serviceCostCents, " +
      "COALESCE(SUM(si.netCents * si.expensesPercentBp / 10000), 0) AS expensesCents, " +
      "COUNT(DISTINCT s.id) AS saleCount " +
      "FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
      "WHERE s.status = 'completed' AND s.soldAt >= :from AND s.soldAt < :to " +
      "AND (:areaId IS NULL OR si.areaId = :areaId) " +
      "AND (:methodId IS NULL OR EXISTS (" +
      "SELECT 1 FROM payments p WHERE p.saleId = s.id AND p.methodId = :methodId)) " +
      "AND (:kind IS NULL OR si.itemKind = :kind) " +
      "GROUP BY day ORDER BY day ASC",
  )
  suspend fun dailySeries(
    from: Long,
    to: Long,
    areaId: String?,
    methodId: String?,
    kind: String?,
  ): List<DaySeriesRow>

  @Query(
    "SELECT status, COUNT(*) AS total FROM appointments WHERE deletedAt IS NULL " +
      "AND startsAt >= :from AND startsAt < :to GROUP BY status",
  )
  suspend fun appointmentCounts(from: Long, to: Long): List<StatusCountRow>

  @Query(
    "SELECT COUNT(*) FROM clients WHERE deletedAt IS NULL AND createdAt >= :from AND createdAt < :to",
  )
  suspend fun newClients(from: Long, to: Long): Int

  @Query(
    "SELECT COUNT(*) FROM pets WHERE deletedAt IS NULL AND createdAt >= :from AND createdAt < :to",
  )
  suspend fun newPets(from: Long, to: Long): Int

  @Query(
    "SELECT COUNT(*) FROM clients WHERE deletedAt IS NULL",
  )
  fun observeClientCount(): Flow<Int>

  @Query("SELECT * FROM cash_sessions WHERE openedAt >= :from AND openedAt < :to ORDER BY openedAt DESC")
  suspend fun cashSessions(from: Long, to: Long): List<CashSessionEntity>

  @Query("SELECT * FROM cash_sessions ORDER BY openedAt DESC LIMIT 30")
  fun observeRecentCashSessions(): Flow<List<CashSessionEntity>>
}
