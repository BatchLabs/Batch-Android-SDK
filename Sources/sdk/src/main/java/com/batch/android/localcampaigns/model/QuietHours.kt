package com.batch.android.localcampaigns.model

import java.util.Calendar

/** Enum that represents a day of the week. */
enum class DayOfWeek(val value: Int) {
    SUNDAY(0),
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6);

    companion object {
        @JvmStatic
        fun valueOf(value: Int) = entries.toTypedArray().firstOrNull { it.value == value }

        @JvmStatic
        fun fromCalendar(calendar: Calendar) =
            entries.toTypedArray().firstOrNull {
                it.value == (calendar.get(Calendar.DAY_OF_WEEK) - 1)
            }
    }
}

/** Data class representing a quiet hour. */
data class QuietHours(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<DayOfWeek>?,
)
