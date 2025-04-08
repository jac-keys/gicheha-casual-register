// ResetAttendanceWorker.kt
package com.gichehafarm.registry

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gichehafarm.registry.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResetAttendanceWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                Log.d("ResetAttendance", "Starting attendance reset...")
                val dbHelper = DatabaseHelper.getInstance(applicationContext)
                dbHelper.copyCasualsToAttendance()
                Log.d("ResetAttendance", "Attendance reset completed successfully")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("ResetAttendance", "Failed to reset attendance", e)
            Result.failure()
        }
    }

    companion object {
        const val WORKER_TAG = "reset_attendance_worker"
        const val UNIQUE_WORK_NAME = "reset_attendance_unique_work"
    }
}