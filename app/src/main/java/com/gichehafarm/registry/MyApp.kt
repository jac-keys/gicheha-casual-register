package com.gichehafarm.registry

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.Date
import java.util.TimeZone
import com.gichehafarm.registry.data.DatabaseHelper
import android.util.Log

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize database and check monthly attendance
        initializeAttendanceSystem()
        WorkManagerInitializer.scheduleDailyAttendanceWorker(this)
        // Schedule the daily worker
     //   scheduleDailyTask()
    }

    private fun initializeAttendanceSystem() {
        val dbHelper = DatabaseHelper.getInstance(this)

        // This will:
        // 1. Check if month has changed
        // 2. Initialize monthly records for all workers if needed
        dbHelper.checkAndResetMonthlyAttendanceIfNeeded()

        // Optional: Log initialization
        Log.d("MyApp", "Attendance system initialized")
    }

  private fun scheduleDailyTask() {
        val workManager = WorkManager.getInstance(this)
        val request = PeriodicWorkRequestBuilder<AttendanceWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelayTo8PM(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_attendance_processor",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateInitialDelayTo8PM(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi")).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 7) // 8 PM
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)

            // If the target time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis - System.currentTimeMillis()
    }
}