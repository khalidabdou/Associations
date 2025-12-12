package org.associations.project.utils

import kotlinx.datetime.*

data class MonthYear(val month: Int, val year: Int) {
    val displayName: String
        get() {
            val monthNames =
                    listOf(
                            "يناير",
                            "فبراير",
                            "مارس",
                            "أبريل",
                            "مايو",
                            "يونيو",
                            "يوليو",
                            "أغسطس",
                            "سبتمبر",
                            "أكتوبر",
                            "نوفمبر",
                            "ديسمبر"
                    )
            return "${monthNames[month - 1]} $year"
        }

    val startEpochMillis: Long
        get() {
            val localDate = LocalDate(year, month, 1)
            return localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }

    val endEpochMillis: Long
        get() {
            // Simple logic: go to next month/year and subtract 1 ms (or just use day 1 of next
            // month)
            // Actually, easier: get start of next month
            val nextMonth = if (month == 12) 1 else month + 1
            val nextYear = if (month == 12) year + 1 else year
            val nextMonthDate = LocalDate(nextYear, nextMonth, 1)
            // Subtract 1 millisecond from the start of next month
            return nextMonthDate
                    .atStartOfDayIn(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds() - 1
        }

    companion object {
        fun current(): MonthYear {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return MonthYear(now.monthNumber, now.year)
        }

        fun generateLast12Months(): List<MonthYear> {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val months = mutableListOf<MonthYear>()
            for (i in 0 until 12) {
                var month = now.monthNumber - i
                var year = now.year
                if (month <= 0) {
                    month += 12
                    year -= 1
                }
                months.add(MonthYear(month, year))
            }
            return months
        }

        fun generateMonthsForYear(year: Int): List<MonthYear> {
            // Return all 12 months for the given year, ordered desc or asc depending on preference.
            // Let's do January to December
            return (1..12).map { MonthYear(it, year) }
        }

        fun getAvailableYears(): List<Int> {
            val currentYear =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            // Include next year for forward planning (e.g., 2026 when in 2025)
            return ((currentYear + 1) downTo (currentYear - 4)).toList()
        }
    }
}
