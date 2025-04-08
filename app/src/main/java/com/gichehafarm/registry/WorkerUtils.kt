// WorkerUtils.kt
package com.gichehafarm.registry.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gichehafarm.registry.ResetAttendanceWorker
import java.util.concurrent.TimeUnit

object WorkerUtils {
    private const val TAG = "WorkerUtils"

    fun scheduleDailyReset(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val resetRequest = PeriodicWorkRequestBuilder<ResetAttendanceWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .addTag(ResetAttendanceWorker.WORKER_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ResetAttendanceWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                resetRequest
            )
            Log.d(TAG, "Successfully scheduled daily reset")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule daily reset", e)
        }
    }

    fun isResetWorkScheduled(context: Context): Boolean {
        return try {
            val workManager = WorkManager.getInstance(context)
            val statuses = workManager
                .getWorkInfosForUniqueWork(ResetAttendanceWorker.UNIQUE_WORK_NAME)
                .get()

            statuses?.any { status ->
                status.state == WorkInfo.State.RUNNING ||
                        status.state == WorkInfo.State.ENQUEUED
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking work status", e)
            false
        }
    }

    fun cancelResetWork(context: Context) {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(ResetAttendanceWorker.UNIQUE_WORK_NAME)
            Log.d(TAG, "Successfully canceled reset work")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reset work", e)
        }
    }
}