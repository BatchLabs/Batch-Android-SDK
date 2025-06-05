package com.batch.android.messaging.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.BatchMessageAction;
import com.batch.android.di.DITest;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class BatchMessageActionTest extends DITest {

    @Test
    public void testIsDismissibleAction() throws JSONException {
        Action isCallback = new Action("callback", new JSONObject());
        BatchMessageAction isCallbackAction = new BatchMessageAction(isCallback);

        Action isDismissibleActionWithNil = new Action(null, new JSONObject());
        BatchMessageAction isDismissibleActionWithNilAction = new BatchMessageAction(isDismissibleActionWithNil);

        Action isDismissibleActionWithDismiss = new Action("batch.dismiss", new JSONObject());
        BatchMessageAction isDismissibleActionWithDismissAction = new BatchMessageAction(
            isDismissibleActionWithDismiss
        );

        assertFalse("Should not be a dismissible action because it is a callback", isCallback.isDismissAction());
        assertFalse("Should not be a dismissible action because it is a callback", isCallbackAction.isDismissAction());

        assertTrue(
            "Should be a dismissible action because it is a Nil action",
            isDismissibleActionWithNil.isDismissAction()
        );
        assertTrue(
            "Should be a dismissible action because it is a Nil action",
            isDismissibleActionWithNilAction.isDismissAction()
        );
        assertTrue(
            "Should be a dismissible action because it is Batch dismiss action",
            isDismissibleActionWithDismiss.isDismissAction()
        );
        assertTrue(
            "Should be a dismissible action because it is Batch dismiss action",
            isDismissibleActionWithDismissAction.isDismissAction()
        );
    }
}
