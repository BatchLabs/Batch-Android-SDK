package com.batch.android.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;

public class BatchDateTest {

    private DateFormat dateFormat;

    @Before
    public void setUp() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testCreateUTCDate() throws ParseException {
        final long ts = stringDateToTimestamp("2017-10-22T12:00:00");
        assertEquals(ts, new UTCDate(ts).getTime());
    }

    @Test
    public void testCreateTimezoneAwareDate() throws ParseException {
        final long tsUTC = stringDateToTimestamp("2017-10-22T12:00:00");
        final long expectedLocalTs = tsUTC - Calendar.getInstance().getTimeZone().getOffset(tsUTC);
        assertEquals(expectedLocalTs, new TimezoneAwareDate(tsUTC).getTime());
    }

    @Test
    public void testCompareUTCDates() throws ParseException {
        final long dateTs = stringDateToTimestamp("2017-10-22T12:00:00");
        final long theDayAfterTs = stringDateToTimestamp("2017-10-23T12:00:00");

        UTCDate date = new UTCDate(dateTs);
        UTCDate theDayAfter = new UTCDate(theDayAfterTs);

        assertTrue(date.compareTo(theDayAfter) < 0);
        assertTrue(theDayAfter.compareTo(date) > 0);
        assertTrue(date.compareTo(new UTCDate(dateTs)) == 0);
    }

    @Test
    public void testCompareTimezoneAwareDates() throws ParseException {
        final long dateTs = stringDateToTimestamp("2017-10-22T12:00:00");
        final long theDayAfterTs = stringDateToTimestamp("2017-10-23T12:00:00");

        TimezoneAwareDate date = new TimezoneAwareDate(dateTs);
        TimezoneAwareDate theDayAfter = new TimezoneAwareDate(theDayAfterTs);

        assertTrue(date.compareTo(theDayAfter) < 0);
        assertTrue(theDayAfter.compareTo(date) > 0);
        assertTrue(date.compareTo(new TimezoneAwareDate(dateTs)) == 0);
    }

    @Test
    public void compareUTCandTimezoneAwareDates() throws ParseException {
        final long utcTs = stringDateToTimestamp("2017-10-22T12:00:00");
        UTCDate dateUTC = new UTCDate(utcTs);
        TimezoneAwareDate dateLocal = new TimezoneAwareDate(utcTs);
        final long offset = Calendar.getInstance().getTimeZone().getOffset(utcTs);

        // TODO : If PowerMock could work, mock static calendar instance and getOffset then test the three cases
        switch ((int) Math.signum(offset)) {
            case 0:
                // If timezone offset is 0, dateLocal == dateUtc
                assertTrue(dateUTC.compareTo(dateLocal) == 0);
                break;
            case -1:
                // If timezone offset is > 0, dateUtc < dateLocal
                assertTrue(dateUTC.compareTo(dateLocal) == -1);
                break;
            case 1:
            default:
                // If timezone offset is < 0, dateUtc > dateLocal
                assertTrue(dateUTC.compareTo(dateLocal) == 1);
                break;
        }
    }

    private long stringDateToTimestamp(String date) throws ParseException {
        return dateFormat.parse(date).getTime();
    }
}
