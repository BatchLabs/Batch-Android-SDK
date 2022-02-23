package com.batch.android.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.Batch;
import com.batch.android.BatchTagCollectionsFetchListener;
import com.batch.android.Config;
import com.batch.android.UserAction;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.di.DITest;
import com.batch.android.di.DITestUtils;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;
import com.batch.android.module.UserModule;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.shadows.ShadowLog;

@RunWith(AndroidJUnit4.class)
@MediumTest
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*" })
@PrepareForTest({ UserModule.class })
public class ActionModuleTest extends DITest {

    private static final String apiKey = "apiKey";
    private Context context;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        context = ApplicationProvider.getApplicationContext();

        RuntimeManagerProvider.get().setContext(context);
        Batch.setConfig(new Config(apiKey));
    }

    @Test
    public void testAddUserTagRegisterAction() throws JSONException {
        JSONObject jsonAdd = generateJSONTestAction("add");

        new ActionModule().performAction(context, "batch.user.tag", jsonAdd, null);

        Batch.User.fetchTagCollections(
            context,
            new BatchTagCollectionsFetchListener() {
                @Override
                public void onSuccess(Map<String, Set<String>> tagCollections) {
                    assertTrue(tagCollections.get("bar").contains("foo"));
                    assertEquals(1, tagCollections.get("bar").size());
                }

                @Override
                public void onError() {
                    fail();
                }
            }
        );
    }

    @Test
    public void testRemoveUserTagRegisterAction() throws JSONException {
        JSONObject jsonAdd = generateJSONTestAction("add");
        new ActionModule().performAction(context, "batch.user.tag", jsonAdd, null);

        JSONObject jsonRemove = generateJSONTestAction("remove");
        new ActionModule().performAction(context, "batch.user.tag", jsonRemove, null);

        Batch.User.fetchTagCollections(
            context,
            new BatchTagCollectionsFetchListener() {
                @Override
                public void onSuccess(Map<String, Set<String>> tagCollections) {
                    assertNull(tagCollections.get("bar"));
                }

                @Override
                public void onError() {
                    fail();
                }
            }
        );
    }

    @Test
    public void testGroupAction() throws JSONException {
        ActionModule actionModule = new ActionModule();

        StateSavingAction firstAction = new StateSavingAction();
        StateSavingAction secondAction = new StateSavingAction();
        actionModule.registerAction(new UserAction("first", firstAction));
        actionModule.registerAction(new UserAction("second", secondAction));

        String groupJSON = "{'actions':[['first', {'foo': 'bar'}],[],['invalid'],['second']]}";

        actionModule.performAction(context, "batch.group", new JSONObject(groupJSON), null);

        assertTrue(firstAction.executed);
        assertTrue(secondAction.executed);

        // Test that an invalid json doesn't crash
        actionModule.performAction(context, "batch.group", new JSONObject("{}"), null);
        actionModule.performAction(context, "batch.group", new JSONObject("{'foo':'bar'}"), null);
        actionModule.performAction(context, "batch.group", new JSONObject("{'actions':'bar'}"), null);
        actionModule.performAction(context, "batch.group", new JSONObject("{'actions':[]}"), null);
        actionModule.performAction(context, "batch.group", new JSONObject("{'actions':{'foo':'bar'}}"), null);
        actionModule.performAction(context, "batch.group", new JSONObject("{'actions':[{'foo':'bar'}]}"), null);
    }

    @Test
    public void testGroupActionLimits() throws JSONException {
        ActionModule actionModule;

        // Check that nested "batch.group" actions doesn't work
        actionModule = new ActionModule();
        StateSavingAction shouldNotRun = new StateSavingAction();
        actionModule.registerAction(new UserAction("shouldNotRun", shouldNotRun));

        String nestedAction = "{'actions':[['batch.group', {'actions': ['shouldNotRun']}]]}";
        actionModule.performAction(context, "batch.group", new JSONObject(nestedAction), null);

        assertFalse(shouldNotRun.executed);

        // Check that you can't run too many actions

        actionModule = new ActionModule();
        shouldNotRun = new StateSavingAction();
        StateSavingAction dummyAction = new StateSavingAction();
        actionModule.registerAction(new UserAction("dummy", dummyAction));
        actionModule.registerAction(new UserAction("shouldNotRun", shouldNotRun));

        // Make sure that 10 actions max can run
        // This should only count valid actions
        String manyActions =
            "{'actions':[['dummy'], [], ['foo', 'bar'], ['dummy'], ['dummy'], ['dummy'], ['dummy'], ['dummy'], ['dummy'], ['dummy'], ['dummy'], ['dummy'], ['shouldNotRun']]}";

        actionModule.performAction(context, "batch.group", new JSONObject(manyActions), null);

        assertTrue(dummyAction.executed);
        assertFalse(shouldNotRun.executed);
    }

    @Test
    public void testClipboardAction() throws JSONException {
        ActionModule actionModule = new ActionModule();

        String clipboardJSON = "{'t':'best text ever', 'd':'best description ever'}";
        assertTrue(actionModule.performAction(context, "batch.clipboard", new JSONObject(clipboardJSON), null));

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData text = clipboard.getPrimaryClip();

        assertEquals("best description ever", text.getDescription().getLabel().toString());
        assertEquals(1, text.getItemCount());
        assertEquals("best text ever", text.getItemAt(0).getText().toString());
    }

    private JSONObject generateJSONTestAction(String action) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("c", "bar");
        json.put("t", "foo");
        json.put("a", action);

        return json;
    }

    /**
     * An action that saves its state once executed, to make tracking its execution easier
     */
    class StateSavingAction implements UserActionRunnable {

        public boolean executed = false;

        @Override
        public void performAction(
            @Nullable Context context,
            @NonNull String identifier,
            @NonNull JSONObject args,
            @Nullable UserActionSource source
        ) {
            executed = true;
        }
    }
}
