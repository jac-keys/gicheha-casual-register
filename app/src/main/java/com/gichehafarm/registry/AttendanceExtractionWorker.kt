package com.gichehafarm.registry

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import com.gichehafarm.registry.data.DatabaseHelper
import com.gichehafarm.registry.data.WorkerAttendance

class AttendanceExtractionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                val dbHelper = DatabaseHelper.getInstance(applicationContext)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = dateFormat.format(Date())

                val presentWorkers = dbHelper.getPresentWorkersForDate(today)

                if (presentWorkers.isNotEmpty()) {
                    saveToCsv(presentWorkers, today)
                    Result.success()
                } else {
                    Result.success() // No workers present is still a success case
                }
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun saveToCsv(workers: List<WorkerAttendance>, date: String) {
        val fileName = "attendance_$date.csv"
        val documentsDir = applicationContext.getExternalFilesDir(null) ?: return
        val file = File(documentsDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("Work Number,Name,Date\n")
            workers.forEach { worker ->
                writer.append("${worker.workNumber},${worker.firstName},${worker.date}\n")
            }
        }
    }
}