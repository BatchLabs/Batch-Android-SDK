package com.batch.android


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.GrantPermissionRule
import com.batch.android.core.NotificationPermissionHelper
import com.batch.android.di.providers.LocalBroadcastManagerProvider
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@SmallTest
@RunWith(AndroidJUnit4::class)
class BatchPermissionActivityTest {

    private var scenario: ActivityScenario<BatchPermissionActivity>? = null

    @Rule  @JvmField
    val mRuntimePermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(NotificationPermissionHelper.PERMISSION_NOTIFICATION)

    @After
    fun cleanup() {
        scenario?.close()
    }

    /**
     * Test the activity is finished if no extra permission is given
     */
    @Test
    fun testActivityFinishedWhenNoExtra() {
        scenario = ActivityScenario.launch(BatchPermissionActivity::class.java)
        Assert.assertEquals(Lifecycle.State.DESTROYED, scenario?.state)
    }

    /**
     * Test the activity is finished if permission is already granted
     */
    @Test
    fun testActivityFinishedWhenPermissionAlreadyGranted() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), BatchPermissionActivity::class.java)
        intent.putExtra(BatchPermissionActivity.EXTRA_PERMISSION, NotificationPermissionHelper.PERMISSION_NOTIFICATION);
        scenario = ActivityScenario.launch(intent)
        scenario?.onActivity {
            Assert.assertTrue(it.isFinishing)
        }
    }

    /**
     * Test we get permission result from the broadcast receiver
     * Also test the activity is finishing well
     */
    @Test
    fun testActivityPermissionResult() {
        val testPermission ="android.permission.ACCESS_COARSE_LOCATION";
        val filter = IntentFilter(BatchPermissionActivity.ACTION_PERMISSION_RESULT)
        LocalBroadcastManagerProvider.get(ApplicationProvider.getApplicationContext()).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val permission = intent.getStringExtra(BatchPermissionActivity.EXTRA_PERMISSION)
                val granted = intent.getBooleanExtra(BatchPermissionActivity.EXTRA_RESULT, false)
                Assert.assertEquals(testPermission, permission)
                Assert.assertTrue(granted)
            }
        }, filter)
        val intent = Intent(ApplicationProvider.getApplicationContext(), BatchPermissionActivity::class.java)
        intent.putExtra(BatchPermissionActivity.EXTRA_PERMISSION, testPermission);
        scenario = ActivityScenario.launch(intent)
        scenario?.onActivity {
            it.onRequestPermissionsResult(51, arrayOf(testPermission), intArrayOf(PackageManager.PERMISSION_GRANTED))
            Assert.assertTrue(it.isFinishing)
        }
    }

}