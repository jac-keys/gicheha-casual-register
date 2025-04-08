package com.gichehafarm.registry

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.time.Duration

object WorkManagerInitializer {
    private const val WORK_TAG = "daily_attendance_processor"
    private const val TARGET_HOUR = 7// 7 AM (note: comment said 11 PM but value is 7)
    private const val TARGET_MINUTE = 45
    private val TIME_ZONE = TimeZone.getTimeZone("Africa/Nairobi")

    fun scheduleDailyAttendanceWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val request = PeriodicWorkRequestBuilder<AttendanceWorker>(
            24, TimeUnit.HOURS // Repeat every 24 hours
        ).apply {
            setInitialDelay(calculateInitialDelayToTargetTime())
            setConstraints(buildConstraints())
        }.build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun buildConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
    }

    private fun calculateInitialDelayToTargetTime(): Duration {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TIME_ZONE).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, TARGET_HOUR)
            set(Calendar.MINUTE, TARGET_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return Duration.ofMillis(calendar.timeInMillis - now)
    }
}