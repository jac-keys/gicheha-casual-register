package com.gichehafarm.registry

import java.util.Calendar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun String.toMillis(): Long? {
    return try {
        val parts = this.split("/")
        if (parts.size == 3) {
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calendar months are 0-based
            val year = parts[2].toInt()

            Calendar.getInstance().apply {
                set(year, month, day)
            }.timeInMillis
        } else null
    } catch (e: Exception) {
        null
    }
}