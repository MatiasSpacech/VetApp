package com.veterapp.ui.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veterapp.AppContainer
import com.veterapp.data.ReportFilters
import com.veterapp.data.ReportResult
import com.veterapp.data.ReportType
import com.veterapp.domain.CashSessionDetail
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.DashboardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

class ReportsViewModel(private val container: AppContainer) : ViewModel() {
  private val _dashboard = MutableStateFlow<DashboardData?>(null)
  val dashboard: StateFlow<DashboardData?> = _dashboard.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _report = MutableStateFlow<ReportResult?>(null)
  val report: StateFlow<ReportResult?> = _report.asStateFlow()

  init {
    refreshDashboard()
  }

  fun refreshDashboard() {
    viewModelScope.launch {
      runCatching { container.reportRepository.dashboard() }
        .onSuccess { _dashboard.value = it }
        .onFailure { _error.value = it.message }
    }
  }

  fun loadReport(
    type: ReportType,
    from: LocalDate,
    to: LocalDate,
    filters: ReportFilters = ReportFilters(),
  ) {
    viewModelScope.launch {
      _loading.value = true
      runCatching { container.reportRepository.report(type, from, to, filters) }
        .onSuccess {
          _report.value = it
          _error.value = null
        }
        .onFailure { _error.value = it.message }
      _loading.value = false
    }
  }

  fun clearReport() {
    _report.value = null
  }

  fun exportCsv(uri: Uri, result: ReportResult, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val outcome = container.reportExporter.exportCsv(uri, result)
      outcome.onSuccess {
        runCatching {
          container.auditRepository.record(
            entity = "report",
            entityId = null,
            action = "report.export",
            payload =
              buildJsonObject {
                put("type", result.type.name)
                put("period", result.periodLabel)
              },
          )
        }
      }
      onResult(outcome)
    }
  }

  fun exportCashPdf(uri: Uri, detail: CashSessionDetail, onResult: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val outcome = container.reportExporter.exportCashPdf(uri, detail)
      outcome.onSuccess {
        runCatching {
          container.auditRepository.record(
            entity = "cash_session",
            entityId = detail.session.id,
            action = "cash.pdf.export",
          )
        }
      }
      onResult(outcome)
    }
  }
}
