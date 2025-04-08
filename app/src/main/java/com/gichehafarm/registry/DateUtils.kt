// app/src/main/java/com/gichehafarm/registry/utils/DateUtils.kt
package com.gichehafarm.registry.utils

import java.util.*

object DateUtils {
    /**
     * Gets current operational month/year (21st of current month to 20th of next month)
     * @return Pair<Month (1-12), Year>
     */
    fun getCurrentOperationalMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based (0-11)
        val currentYear = calendar.get(Calendar.YEAR)

        return if (currentDay >= 21) {
            // Next month
            if (currentMonth == 11) Pair(0, currentYear + 1) // December -> January
            else Pair(currentMonth + 1, currentYear)
        } else {
            Pair(currentMonth, currentYear)
        }.let { (month, year) ->
            Pair(month + 1, year) // Convert to 1-based month
        }
    }

    fun getPreviousMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        return calendar.get(Calendar.MONTH) + 1 to calendar.get(Calendar.YEAR)
    }
        // Check if a date is within the 21st-20th operational month
        fun getOperationalMonthYear(date: Calendar): Pair<Int, Int> {
            val day = date.get(Calendar.DAY_OF_MONTH)
            var month = date.get(Calendar.MONTH) // 0-based (Jan=0)
            var year = date.get(Calendar.YEAR)

            if (day >= 21) {
                month += 1
                if (month > 11) { // Handle December → January
                    month = 0
                    year += 1
                }
            }
            // Convert to 1-based month (Jan=1)
            return Pair(month + 1, year)
        }

        // Generate start/end dates for an operational month (e.g., Jan 2024 → 2023-12-21 to 2024-01-20)
        fun getOperationalPeriod(month: Int, year: Int): Pair<String, String> {
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year

            return Pair(
                String.format("%04d-%02d-21", prevYear, prevMonth),
                String.format("%04d-%02d-20", year, month)
            )
        }

    fun isLastDayOfMonth(date: Calendar): Boolean {
        val lastDay = date.getActualMaximum(Calendar.DAY_OF_MONTH)
        return date.get(Calendar.DAY_OF_MONTH) == lastDay
    }
}