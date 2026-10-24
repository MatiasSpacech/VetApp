package com.veterapp.notifications

import com.veterapp.domain.ReminderPlan

interface ReminderNotifier {
  fun canPost(): Boolean

  fun show(plan: ReminderPlan): Boolean

  fun cancelForVaccination(vaccinationId: String)
}
