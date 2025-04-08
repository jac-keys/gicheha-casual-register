// AttendanceExtractionReceiver.kt
package com.gichehafarm.registry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class AttendanceExtractionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val workRequest = OneTimeWorkRequestBuilder<AttendanceExtractionWorker>()
            .setInputData(workDataOf("date" to System.currentTimeMillis()))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}