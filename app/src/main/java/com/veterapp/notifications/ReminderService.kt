package com.veterapp.notifications

import com.veterapp.data.VaccinationRepository
import com.veterapp.data.local.VaccinationNotificationDao
import com.veterapp.data.local.VaccinationNotificationEntity
import com.veterapp.domain.ClinicTime
import com.veterapp.domain.ReminderKey
import com.veterapp.domain.ReminderMilestone
import com.veterapp.domain.planReminders
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReminderService(
  private val vaccinationRepository: VaccinationRepository,
  private val notificationDao: VaccinationNotificationDao,
  private val notifications: ReminderNotifier,
) {
  private val mutex = Mutex()

  suspend fun run(today: LocalDate = ClinicTime.today()): Int =
    mutex.withLock {
      if (!notifications.canPost()) return@withLock 0

      val candidates = vaccinationRepository.reminderCandidates()
      val alreadyNotified =
        notificationDao
          .all()
          .mapNotNull { entity ->
            ReminderMilestone.fromStorage(entity.milestone)?.let { milestone ->
              ReminderKey(entity.vaccinationId, entity.dueOn, milestone)
            }
          }
          .toSet()

      val plans = planReminders(today, candidates, alreadyNotified)
      var sent = 0
      plans.forEach { plan ->
        if (notifications.show(plan)) {
          notificationDao.insert(
            VaccinationNotificationEntity(
              id = UUID.randomUUID().toString(),
              clinicId = plan.clinicId,
              vaccinationId = plan.vaccinationId,
              petId = plan.petId,
              dueOn = plan.dueOn,
              milestone = plan.milestone.storage,
              notifiedAt = System.currentTimeMillis(),
            ),
          )
          sent++
        }
      }
      sent
    }
}
