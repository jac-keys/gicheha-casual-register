package com.gichehafarm.registry.utils
import java.util.Calendar


    object TimeUtils {
        fun calculateDelayUntilMidnight(): Long {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1) // Move to next day
            }
            return midnight.timeInMillis - now.timeInMillis
        }
    }

