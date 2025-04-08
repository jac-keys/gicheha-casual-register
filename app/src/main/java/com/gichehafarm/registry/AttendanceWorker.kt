package com.gichehafarm.registry

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.gichehafarm.registry.data.DatabaseHelper
import com.gichehafarm.registry.utils.DateUtils
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val TAG = "AttendanceWorker"

    override fun doWork(): Result {
        Log.d(TAG, "Worker started at ${Date()}")
        val dbHelper = DatabaseHelper.getInstance(applicationContext)

        return try {
            // 1. Log initial state
            logAttendanceState(dbHelper, "Before processing")

            // 2. Process monthly reset if needed
            dbHelper.checkAndResetMonthlyAttendanceIfNeeded()
            Log.d(TAG, "Monthly reset check completed")

            // 3. Increment counters for present workers
            val incrementedCount = dbHelper.incrementDaysAttended()
            Log.d(TAG, "Incremented days for $incrementedCount workers")

            // 4. Reset daily flags
            val resetCount = dbHelper.resetDailyAttendanceFlags()
            Log.d(TAG, "Reset attendance flags for $resetCount workers")

            // 5. Log final state
            logAttendanceState(dbHelper, "After processing")

            Log.d(TAG, "Worker completed successfully at ${Date()}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing attendance", e)
            Result.failure()
        }
    }

    private fun logAttendanceState(dbHelper: DatabaseHelper, prefix: String) {
        try {
            // 1. First get today's date
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 2. Get summary metrics
            dbHelper.readableDatabase.rawQuery(
                """
            SELECT 
                (SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REGISTER_CASUALS}) AS total_workers,
                (SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_HR_ATTENDANCE} 
                 WHERE ${DatabaseHelper.COLUMN_DATE} = '$today') AS present_today
            """, null
            ).use { summaryCursor ->
                if (summaryCursor.moveToFirst()) {
                    val totalWorkers = summaryCursor.getInt(0)
                    val presentToday = summaryCursor.getInt(1)

                    // 3. Get per-worker attendance details
                    dbHelper.readableDatabase.rawQuery(
                        """
                    SELECT 
                        r.${DatabaseHelper.COLUMN_WORK_NUMBER},
                        COUNT(a.${DatabaseHelper.COLUMN_ID}) AS total_days
                    FROM ${DatabaseHelper.TABLE_REGISTER_CASUALS} r
                    LEFT JOIN ${DatabaseHelper.TABLE_HR_ATTENDANCE} a
                        ON r.${DatabaseHelper.COLUMN_ID} = a.${DatabaseHelper.COLUMN_WORKER_ID}
                    GROUP BY r.${DatabaseHelper.COLUMN_ID}
                    """, null
                    ).use { detailCursor ->
                        val attendanceDetails = StringBuilder()
                        while (detailCursor.moveToNext()) {
                            attendanceDetails.appendLine(
                                "Worker ${detailCursor.getString(0)}: " +
                                        "${detailCursor.getInt(1)} days attended"
                            )
                        }

                        // 4. Log everything together
                        Log.d(TAG, """
                        |$prefix:
                        |Total Registered Workers: $totalWorkers
                        |Present Today ($today): $presentToday
                        |
                        |INDIVIDUAL ATTENDANCE:
                        |${attendanceDetails.toString().trimIndent()}
                    """.trimMargin())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging attendance state", e)
        }
    }
}