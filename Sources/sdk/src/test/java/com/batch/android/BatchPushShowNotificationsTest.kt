package com.batch.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.core.ParameterKeys
import com.batch.android.core.PushNotificationType
import com.batch.android.di.DITest
import com.batch.android.di.providers.ParametersProvider
import java.util.EnumSet
import junit.framework.TestCase.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BatchPushShowNotificationsTest : DITest() {

    // Application context for testing
    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testShowNotifications() {
        // Ensure that the default value is true
        assertTrue(Batch.Push.shouldShowNotifications(context))

        // Set the value to false
        Batch.Push.setShowNotifications(false)
        assertFalse(Batch.Push.shouldShowNotifications(context))

        // Set the value to true
        Batch.Push.setShowNotifications(true)
        assertTrue(Batch.Push.shouldShowNotifications(context))
    }

    @Test
    fun testShowNotificationsCompat() {

        // Ensure that the default value is true
        assertTrue(Batch.Push.shouldShowNotifications(context))

        // Set only the NONE value
        setNotificationType(EnumSet.of(PushNotificationType.NONE))
        assertFalse(Batch.Push.shouldShowNotifications(context))

        // Set all the values except ALERT
        setNotificationType(
            EnumSet.of(
                PushNotificationType.SOUND,
                PushNotificationType.VIBRATE,
                PushNotificationType.LIGHTS,
            )
        )
        assertFalse(Batch.Push.shouldShowNotifications(context))

        // Set only ALERT value
        setNotificationType(EnumSet.of(PushNotificationType.ALERT))
        assertTrue(Batch.Push.shouldShowNotifications(context))
    }

    /** Set the notification type in the parameters provider. */
    private fun setNotificationType(set: EnumSet<PushNotificationType>) {
        ParametersProvider.get(context)
            .set(ParameterKeys.PUSH_NOTIF_TYPE, PushNotificationType.toValue(set).toString(), false)
    }
}
