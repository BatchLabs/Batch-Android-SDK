package com.batch.android.localcampaigns

import com.batch.android.localcampaigns.model.DayOfWeek
import com.batch.android.localcampaigns.model.QuietHours
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuietHoursTest {

    // --- DayOfWeek Tests ---

    @Test
    fun dayOfWeek_valueOf_returnsCorrectDay() {
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.valueOf(0))
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.valueOf(1))
        assertEquals(DayOfWeek.TUESDAY, DayOfWeek.valueOf(2))
        assertEquals(DayOfWeek.WEDNESDAY, DayOfWeek.valueOf(3))
        assertEquals(DayOfWeek.THURSDAY, DayOfWeek.valueOf(4))
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.valueOf(5))
        assertEquals(DayOfWeek.SATURDAY, DayOfWeek.valueOf(6))
    }

    @Test
    fun dayOfWeek_valueOf_returnsNullForInvalidValue() {
        assertNull(DayOfWeek.valueOf(7))
        assertNull(DayOfWeek.valueOf(-1))
    }

    @Test
    fun dayOfWeek_fromCalendar_returnsCorrectDay() {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.fromCalendar(calendar))

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.fromCalendar(calendar))

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        assertEquals(DayOfWeek.TUESDAY, DayOfWeek.fromCalendar(calendar))

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        assertEquals(DayOfWeek.WEDNESDAY, DayOfWeek.fromCalendar(calendar))

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
        assertEquals(DayOfWeek.THURSDAY, DayOfWeek.fromCalendar(calendar))

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.fromCalendar(calendar))

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        assertEquals(DayOfWeek.SATURDAY, DayOfWeek.fromCalendar(calendar))
    }

    // --- QuietHours Tests ---

    @Test
    fun quietHours_creation_setsPropertiesCorrectly() {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        val quietHours =
            QuietHours(
                startHour = 9,
                startMinute = 30,
                endHour = 17,
                endMinute = 0,
                daysOfWeek = days,
            )

        assertEquals(9, quietHours.startHour)
        assertEquals(30, quietHours.startMinute)
        assertEquals(17, quietHours.endHour)
        assertEquals(0, quietHours.endMinute)
        assertEquals(days, quietHours.daysOfWeek)
    }

    @Test
    fun quietHours_creation_withNullDays_setsPropertiesCorrectly() {
        val quietHours =
            QuietHours(
                startHour = 22,
                startMinute = 0,
                endHour = 6,
                endMinute = 0,
                daysOfWeek = null,
            )

        assertEquals(22, quietHours.startHour)
        assertEquals(0, quietHours.startMinute)
        assertEquals(6, quietHours.endHour)
        assertEquals(0, quietHours.endMinute)
        assertNull(quietHours.daysOfWeek)
    }
}
