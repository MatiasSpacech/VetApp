package com.veterapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.veterapp.MainActivity
import com.veterapp.R
import com.veterapp.domain.ReminderMilestone
import com.veterapp.domain.ReminderPlan
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReminderNotifications(private val context: Context) : ReminderNotifier {
  fun ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        "Recordatorios de vacunas",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Avisos de vacunas próximas y vencidas"
      }
    manager.createNotificationChannel(channel)
  }

  override fun canPost(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  override fun show(plan: ReminderPlan): Boolean {
    if (!canPost()) return false
    ensureChannel()

    val title =
      when (plan.milestone) {
        ReminderMilestone.Overdue -> "Vacuna vencida"
        ReminderMilestone.Week -> "Vacuna por vencer"
        ReminderMilestone.Month -> "Próxima vacuna"
      }
    val dueDate = formatEpochDay(plan.dueOn)
    val text =
      when (plan.milestone) {
        ReminderMilestone.Overdue ->
          "${plan.petName}: ${plan.vaccineName} venció el $dueDate"
        ReminderMilestone.Week ->
          when (plan.daysUntilDue) {
            0L -> "${plan.petName}: ${plan.vaccineName} vence hoy ($dueDate)"
            1L -> "${plan.petName}: ${plan.vaccineName} vence mañana ($dueDate)"
            else ->
              "${plan.petName}: ${plan.vaccineName} vence en ${plan.daysUntilDue} días ($dueDate)"
          }
        ReminderMilestone.Month ->
          "${plan.petName}: ${plan.vaccineName} vence el $dueDate"
      }

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    return try {
      NotificationManagerCompat.from(context).notify(notificationId(plan), notification)
      true
    } catch (_: SecurityException) {
      false
    }
  }

  override fun cancelForVaccination(vaccinationId: String) {
    val manager = NotificationManagerCompat.from(context)
    ReminderMilestone.entries.forEach { milestone ->
      manager.cancel(notificationId(vaccinationId, milestone))
    }
  }

  private fun notificationId(plan: ReminderPlan): Int =
    notificationId(plan.vaccinationId, plan.milestone)

  private fun notificationId(vaccinationId: String, milestone: ReminderMilestone): Int =
    "veterapp-reminder-$vaccinationId-${milestone.storage}".hashCode()

  private fun formatEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

  private companion object {
    const val CHANNEL_ID = "veterapp-vacunas"
  }
}
