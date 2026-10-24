package com.veterapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veterapp.VeterAppApplication
import com.veterapp.domain.AuditEvent
import com.veterapp.domain.PetStatus
import com.veterapp.security.BiometricUnlock
import com.veterapp.security.PinPolicy
import com.veterapp.security.findFragmentActivity
import com.veterapp.ui.common.TimeField
import com.veterapp.ui.common.formatDateTime
import com.veterapp.util.userMessage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val app = context.applicationContext as VeterAppApplication
  val viewModel: SettingsViewModel = viewModel { SettingsViewModel(app.container) }
  val security by viewModel.securityState.collectAsStateWithLifecycle()
  val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
  val auditEvents by viewModel.auditEvents.collectAsStateWithLifecycle(initialValue = emptyList())
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val biometricAvailable = remember(context) { BiometricUnlock.isAvailable(context) }

  var pinDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }

  fun showMessage(message: String, isError: Boolean = false) {
    scope.launch {
      snackbarHostState.showSnackbar(if (isError) "Error: $message" else message)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Ajustes") },
        navigationIcon = {
          TextButton(onClick = onBack) { Text("Volver") }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { Spacer(Modifier.height(4.dp)) }

      item {
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text("Seguridad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              if (security.hasPin) {
                "Bloqueo activo con PIN de ${security.pinLength} dígitos."
              } else {
                "La app no está bloqueada. Configurá un PIN para proteger los datos."
              },
              style = MaterialTheme.typography.bodyMedium,
            )
            if (security.hasPin) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pinDialogMode = PinDialogMode.Change }) {
                  Text("Cambiar PIN")
                }
                TextButton(onClick = { pinDialogMode = PinDialogMode.Remove }) {
                  Text("Quitar bloqueo", color = MaterialTheme.colorScheme.error)
                }
              }
              OutlinedButton(onClick = viewModel::lockNow) { Text("Bloquear ahora") }
            } else {
              Button(onClick = { pinDialogMode = PinDialogMode.Create }) { Text("Crear PIN") }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Desbloqueo biométrico", style = MaterialTheme.typography.titleSmall)
                Text(
                  when {
                    !security.hasPin -> "Configurá un PIN para activarlo."
                    !biometricAvailable -> "No disponible en este dispositivo."
                    else -> "Huella o reconocimiento facial."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = security.biometricEnabled,
                enabled = security.hasPin && (biometricAvailable || security.biometricEnabled),
                onCheckedChange = { enabled ->
                  if (!enabled) {
                    viewModel.setBiometricEnabled(false)
                    showMessage("Desbloqueo biométrico desactivado")
                  } else {
                    val activity = context.findFragmentActivity()
                    if (activity == null) {
                      showMessage("No se pudo abrir la biometría", isError = true)
                    } else {
                      BiometricUnlock.authenticate(
                        activity = activity,
                        title = "Activar biometría",
                        subtitle = "Confirmá tu identidad para desbloquear VeterApp",
                        onSuccess = {
                          viewModel.setBiometricEnabled(true)
                          showMessage("Desbloqueo biométrico activado")
                        },
                        onError = { code, message ->
                          if (
                            code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                              code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                              code != androidx.biometric.BiometricPrompt.ERROR_CANCELED
                          ) {
                            showMessage(message.toString(), isError = true)
                          }
                        },
                      )
                    }
                  }
                },
              )
            }
            Text(
              "El bloqueo se pide al abrir la app y después de un minuto en segundo plano.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      item {
        AgendaCard(
          viewModel = viewModel,
          onMessage = { text, isError -> showMessage(text, isError) },
        )
      }

      item {
        BackupCard(
          lastBackupAt = lastBackupAt,
          viewModel = viewModel,
        )
      }

      item {
        Spacer(Modifier.height(4.dp))
        Text("Auditoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
          "Eventos críticos guardados en este dispositivo.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (auditEvents.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Text(
              "Todavía no hay eventos registrados.",
              modifier = Modifier.padding(16.dp),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      } else {
        items(auditEvents.take(30), key = { it.id }) { event ->
          AuditRow(event)
        }
        item {
          Text(
            "Se muestran los últimos 30 eventos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item { Spacer(Modifier.height(24.dp)) }
    }
  }

  pinDialogMode?.let { mode ->
    PinDialog(
      mode = mode,
      viewModel = viewModel,
      onDismiss = { pinDialogMode = null },
      onSuccess = { message -> showMessage(message) },
    )
  }
}

@Composable
private fun AgendaCard(
  viewModel: SettingsViewModel,
  onMessage: (String, Boolean) -> Unit,
) {
  var hours by remember { mutableStateOf(viewModel.businessHours()) }
  var duration by remember { mutableStateOf(viewModel.defaultDurationMinutes()) }
  var busy by remember { mutableStateOf(false) }
  val dayLabels = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

  fun updateDay(index: Int, day: com.veterapp.domain.DayHours) {
    hours = hours.copy(days = hours.days.toMutableList().also { it[index] = day })
  }

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        "Agenda",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        "Duración predeterminada de los turnos nuevos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(15, 30, 45, 60, 90, 120).forEach { option ->
          OutlinedButton(onClick = { duration = option }, enabled = !busy) {
            Text(if (duration == option) "✓ $option" else "$option")
          }
        }
      }
      Text(
        "Horario de atención",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      hours.days.forEachIndexed { index, day ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayLabels[index], modifier = Modifier.weight(1f))
            Text(
              if (day.closed) "Cerrado" else "Abierto",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
              checked = !day.closed,
              enabled = !busy,
              onCheckedChange = { open -> updateDay(index, day.copy(closed = !open)) },
            )
          }
          if (!day.closed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TimeField(
                label = "Apertura",
                minuteOfDay = day.openMinute,
                onChange = { updateDay(index, day.copy(openMinute = it)) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
              )
              TimeField(
                label = "Cierre",
                minuteOfDay = day.closeMinute,
                onChange = { updateDay(index, day.copy(closeMinute = it)) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
        HorizontalDivider()
      }
      Text(
        "Los turnos fuera de este horario se pueden guardar con confirmación.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = {
          busy = true
          viewModel.saveAgenda(hours, duration) { result ->
            busy = false
            result
              .onSuccess { onMessage("Agenda guardada", false) }
              .onFailure { onMessage(it.userMessage("No se pudo guardar la agenda"), true) }
          }
        },
        enabled = !busy,
      ) {
        Text(if (busy) "Guardando…" else "Guardar agenda")
      }
    }
  }
}

@Composable
private fun BackupCard(
  lastBackupAt: Long,
  viewModel: SettingsViewModel,
) {
  var passphrase by rememberSaveable { mutableStateOf("") }
  var confirmation by rememberSaveable { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }
  var messageIsError by remember { mutableStateOf(false) }

  fun finish(result: Result<Int>, successText: String) {
    busy = false
    result
      .onSuccess {
        message = successText.format(it)
        messageIsError = false
      }
      .onFailure {
        message = it.userMessage("No se pudo completar el backup")
        messageIsError = true
      }
  }

  val createBackup =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      when {
        passphrase.length < 8 -> {
          message = "La frase debe tener al menos 8 caracteres"
          messageIsError = true
        }
        passphrase != confirmation -> {
          message = "Las frases no coinciden"
          messageIsError = true
        }
        else -> {
          busy = true
          message = null
          viewModel.exportBackup(passphrase, uri) { result ->
            finish(result, "Backup guardado: %d mascota(s)")
          }
        }
      }
    }

  val openBackup =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      if (passphrase.length < 8) {
        message = "Escribí la frase del backup"
        messageIsError = true
      } else {
        busy = true
        message = null
        viewModel.restoreBackup(passphrase, uri) { result ->
          finish(result, "Backup restaurado: %d mascota(s)")
        }
      }
    }

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        "Backup local cifrado",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        "Incluye clientes, mascotas, historial de propietario, historia clínica, vacunas, turnos, auditoría y cambios pendientes. No incluye las fotos. La frase no se guarda: anotala.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        "Último backup: ${formatDateTime(lastBackupAt)}",
        style = MaterialTheme.typography.bodySmall,
      )
      OutlinedTextField(
        value = passphrase,
        onValueChange = { passphrase = it },
        label = { Text("Frase de backup") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = confirmation,
        onValueChange = { confirmation = it },
        label = { Text("Repetir frase (solo al guardar)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            createBackup.launch(
              "veterapp-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}.vbackup",
            )
          },
          enabled = !busy,
        ) {
          Text("Guardar backup")
        }
        OutlinedButton(
          onClick = { openBackup.launch(arrayOf("application/octet-stream", "*/*")) },
          enabled = !busy,
        ) {
          Text("Restaurar")
        }
      }
      if (busy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(8.dp))
          Text("Procesando…", style = MaterialTheme.typography.bodySmall)
        }
      }
      message?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = if (messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
      }
      Text(
        "Restaurar reemplaza los datos locales. Iniciá sesión con la misma cuenta y clínica antes de restaurar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private enum class PinDialogMode { Create, Change, Remove }

@Composable
private fun PinDialog(
  mode: PinDialogMode,
  viewModel: SettingsViewModel,
  onDismiss: () -> Unit,
  onSuccess: (String) -> Unit,
) {
  var currentPin by rememberSaveable(mode) { mutableStateOf("") }
  var newPin by rememberSaveable(mode) { mutableStateOf("") }
  var confirmation by rememberSaveable(mode) { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun filterDigits(value: String): String =
    value.filter { it in '0'..'9' }.take(PinPolicy.MAX_LENGTH)

  fun finish(result: Result<Unit>, successText: String) {
    busy = false
    result
      .onSuccess {
        onDismiss()
        onSuccess(successText)
      }
      .onFailure { error = it.userMessage("No se pudo completar la operación") }
  }

  fun submit() {
    error = null
    when (mode) {
      PinDialogMode.Create -> {
        PinPolicy.error(newPin)?.let {
          error = it
          return
        }
        if (newPin != confirmation) {
          error = "Los PIN no coinciden"
          return
        }
        busy = true
        viewModel.setPin(newPin) { finish(it, "PIN configurado") }
      }
      PinDialogMode.Change -> {
        PinPolicy.error(newPin)?.let {
          error = it
          return
        }
        if (newPin != confirmation) {
          error = "Los PIN nuevos no coinciden"
          return
        }
        if (currentPin.isBlank()) {
          error = "Escribí el PIN actual"
          return
        }
        busy = true
        viewModel.changePin(currentPin, newPin) { finish(it, "PIN cambiado") }
      }
      PinDialogMode.Remove -> {
        if (currentPin.isBlank()) {
          error = "Escribí el PIN actual"
          return
        }
        busy = true
        viewModel.removePin(currentPin) { finish(it, "Bloqueo desactivado") }
      }
    }
  }

  AlertDialog(
    onDismissRequest = { if (!busy) onDismiss() },
    title = {
      Text(
        when (mode) {
          PinDialogMode.Create -> "Crear PIN"
          PinDialogMode.Change -> "Cambiar PIN"
          PinDialogMode.Remove -> "Quitar bloqueo"
        },
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        when (mode) {
          PinDialogMode.Create ->
            Text(
              "Elegí un PIN de ${PinPolicy.MIN_LENGTH} a ${PinPolicy.MAX_LENGTH} dígitos. Lo vas a usar para desbloquear la app.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          PinDialogMode.Change ->
            Text(
              "Ingresá el PIN actual y elegí uno nuevo.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          PinDialogMode.Remove ->
            Text(
              "Se desactiva el bloqueo y también la biometría. La app quedará abierta para cualquiera que use el dispositivo.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
        }

        if (mode != PinDialogMode.Create) {
          PinInput(
            value = currentPin,
            onValueChange = { currentPin = filterDigits(it) },
            label = "PIN actual",
          )
        }
        if (mode != PinDialogMode.Remove) {
          PinInput(
            value = newPin,
            onValueChange = { newPin = filterDigits(it) },
            label = if (mode == PinDialogMode.Create) "PIN" else "PIN nuevo",
          )
          PinInput(
            value = confirmation,
            onValueChange = { confirmation = filterDigits(it) },
            label = "Repetir PIN",
          )
        }

        error?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (busy) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Guardando…", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = ::submit,
        enabled = !busy,
      ) {
        Text(
          when (mode) {
            PinDialogMode.Create -> "Crear"
            PinDialogMode.Change -> "Cambiar"
            PinDialogMode.Remove -> "Quitar"
          },
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
    },
  )
}

@Composable
private fun PinInput(value: String, onValueChange: (String) -> Unit, label: String) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    visualTransformation = PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun AuditRow(event: AuditEvent) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(
        auditActionLabel(event.action),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        auditDetail(event),
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        formatDateTime(event.createdAt),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun auditActionLabel(action: String): String =
  when (action) {
    "pet.create" -> "Mascota creada"
    "pet.update" -> "Mascota editada"
    "pet.status" -> "Estado de mascota cambiado"
    "pet.owner.change" -> "Cambio de propietario"
    "pet.delete" -> "Mascota archivada"
    "client.create" -> "Cliente creado"
    "client.update" -> "Cliente editado"
    "client.delete" -> "Cliente archivado"
    "appointment.create" -> "Turno creado"
    "appointment.update" -> "Turno editado"
    "appointment.confirm" -> "Turno confirmado"
    "appointment.cancel" -> "Turno cancelado"
    "appointment.no_show" -> "Turno marcado como ausente"
    "appointment.attend" -> "Turno atendido"
    "settings.agenda.update" -> "Agenda actualizada"
    "backup.export" -> "Backup exportado"
    "backup.restore" -> "Backup restaurado"
    "security.pin.create" -> "PIN configurado"
    "security.pin.change" -> "PIN cambiado"
    "security.pin.remove" -> "Bloqueo desactivado"
    "security.biometric.enable" -> "Biometría activada"
    "security.biometric.disable" -> "Biometría desactivada"
    else -> action
  }

private fun auditDetail(event: AuditEvent): String =
  when (event.action) {
    "pet.create" -> "Ficha de ${payloadText(event, "name") ?: "mascota"} creada."
    "pet.update" -> {
      val name = payloadText(event, "name") ?: "mascota"
      val changed = payloadText(event, "changed").orEmpty()
      if (changed.isBlank()) "Ficha de $name editada." else "Ficha de $name editada. Cambios: $changed."
    }
    "pet.status" -> {
      val name = payloadText(event, "name") ?: "mascota"
      val from = PetStatus.fromStorage(payloadText(event, "from")).label
      val to = PetStatus.fromStorage(payloadText(event, "to")).label
      "Estado de $name: $from → $to."
    }
    "pet.owner.change" -> {
      val name = payloadText(event, "name") ?: "mascota"
      val previous = payloadText(event, "previous_owner") ?: "sin propietario"
      val current = payloadText(event, "new_owner") ?: "sin propietario"
      val reason = payloadText(event, "reason")
      "Mascota $name: de $previous a $current.${reason?.let { " Motivo: $it." }.orEmpty()}"
    }
    "pet.delete" -> "Ficha de ${payloadText(event, "name") ?: "mascota"} archivada."
    "client.create" -> "Cliente ${payloadText(event, "name") ?: "sin nombre"} creado."
    "client.update" -> {
      val name = payloadText(event, "name") ?: "cliente"
      val changed = payloadText(event, "changed").orEmpty()
      if (changed.isBlank()) "Cliente $name editado." else "Cliente $name editado. Cambios: $changed."
    }
    "client.delete" -> "Cliente ${payloadText(event, "name") ?: "sin nombre"} archivado."
    "appointment.create" -> "Se agendó un turno."
    "appointment.update" -> "Se editó un turno."
    "appointment.confirm" -> "Se confirmó un turno."
    "appointment.cancel" -> {
      val reason = payloadText(event, "reason")
      "Se canceló un turno.${reason?.let { " Motivo: $it." }.orEmpty()}"
    }
    "appointment.no_show" -> "Se marcó un turno como ausente."
    "appointment.attend" -> "Se atendió un turno y se vinculó la consulta."
    "settings.agenda.update" -> "Se actualizó el horario de atención."
    "backup.export" ->
      "Clientes: ${payloadInt(event, "clients") ?: 0} · Mascotas: ${payloadInt(event, "pets") ?: 0}."
    "backup.restore" ->
      "Clientes: ${payloadInt(event, "clients") ?: 0} · Mascotas: ${payloadInt(event, "pets") ?: 0}."
    "security.pin.create" -> "Se configuró el bloqueo con PIN."
    "security.pin.change" -> "Se cambió el PIN de desbloqueo."
    "security.pin.remove" -> "Se desactivó el bloqueo de la app."
    "security.biometric.enable" -> "Se activó el desbloqueo biométrico."
    "security.biometric.disable" -> "Se desactivó el desbloqueo biométrico."
    else -> event.entity
  }

private val auditJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun payloadObject(event: AuditEvent): Map<String, kotlinx.serialization.json.JsonElement>? =
  event.payload?.let {
    runCatching { auditJson.parseToJsonElement(it).jsonObject }.getOrNull()
  }

private fun payloadText(event: AuditEvent, key: String): String? =
  payloadObject(event)?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun payloadInt(event: AuditEvent, key: String): Int? =
  payloadObject(event)?.get(key)?.jsonPrimitive?.intOrNull
