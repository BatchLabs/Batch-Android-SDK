package com.batch.android.lisp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import com.batch.android.BatchEventData;
import com.batch.android.TestActivity;
import com.batch.android.user.AttributeType;
import com.batch.android.user.UserAttribute;
import com.batch.android.user.UserDatabaseException;
import com.batch.android.user.UserDatasource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.Null;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.batch.android.BuildConfig.API_LEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ContextTest
{
    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class,
            false,
            true);


    @Test
    public void testMetaContext()
    {
        ArrayList<EvaluationContext> evalContexts = new ArrayList<>();
        evalContexts.add(new SimpleVariableContext("foo", new PrimitiveValue("bar")));
        evalContexts.add(new SimpleVariableContext("lorem", new PrimitiveValue("ipsum")));

        MetaContext context = new MetaContext(evalContexts);

        assertEquals(new PrimitiveValue("bar"), context.resolveVariableNamed("foo"));
        assertEquals(new PrimitiveValue("ipsum"), context.resolveVariableNamed("lorem"));
        assertNull(context.resolveVariableNamed("missing"));
    }

    @Test
    public void testCachingContext()
    {
        AlternatingContext alternatingContext = new AlternatingContext(new PrimitiveValue("foo"));

        CachingContext cachingContext = new CachingContext(alternatingContext);
        assertEquals(new PrimitiveValue("foo"), cachingContext.resolveVariableNamed("foo"));
        // Test that the alternating context works
        assertNull(alternatingContext.resolveVariableNamed("foo"));
        assertEquals(new PrimitiveValue("foo"), cachingContext.resolveVariableNamed("foo"));

        alternatingContext = new AlternatingContext(PrimitiveValue.nilValue());

        cachingContext = new CachingContext(alternatingContext);
        assertEquals(PrimitiveValue.nilValue(), cachingContext.resolveVariableNamed("foo"));
        // Test that the alternating context works
        assertNull(alternatingContext.resolveVariableNamed("foo"));
        assertEquals(PrimitiveValue.nilValue(), cachingContext.resolveVariableNamed("foo"));
    }

    @Test
    public void testPrivateEventContext()
    {
        EventContext context = new EventContext("_START", null, null);

        assertEquals(new PrimitiveValue("_START"), resolveSimple(context, "e.name"));
        assertEquals(PrimitiveValue.nilValue(), resolveSimple(context, "e.label"));
        assertEquals(PrimitiveValue.nilValue(), resolveSimple(context, "e.tags"));
        assertEquals(PrimitiveValue.nilValue(),
                resolveSimple(context, "e.attr['foo']"));

        // Check that the context fall through unknown variables
        assertNull(resolveSimple(context, "test"));
    }

    @Test
    public void testPublicEventContext()
    {
        BatchEventData eventData = new BatchEventData();
        eventData.addTag("foo");
        eventData.addTag("bar");
        eventData.put("bool", true);
        eventData.put("double", (double) 2);
        eventData.put("float", (float) 2);
        eventData.put("int", (int) 2);
        eventData.put("string", "str");
        eventData.put("CAPS", "str");

        EventContext context = new EventContext("E.TEST_EVENT", "test label", eventData);

        assertEquals(new PrimitiveValue("E.TEST_EVENT"), resolveLowercased(context, "e.name"));
        assertEquals(new PrimitiveValue("test label"), resolveLowercased(context, "e.label"));
        assertEquals(new PrimitiveValue(new HashSet<>(Arrays.asList("foo", "bar"))),
                resolveLowercased(context, "e.tags"));

        assertEquals(new PrimitiveValue(true),
                resolveLowercased(context, "e.attr['bool']"));
        assertEquals(new PrimitiveValue(2.0),
                resolveLowercased(context, "e.attr['double']"));
        assertEquals(new PrimitiveValue(2.0),
                resolveLowercased(context, "e.attr['float']"));
        assertEquals(new PrimitiveValue(2.0),
                resolveLowercased(context, "e.attr['int']"));
        assertEquals(new PrimitiveValue("str"),
                resolveLowercased(context, "e.attr['string']"));
        assertEquals(PrimitiveValue.nilValue(),
                resolveLowercased(context, "e.attr['missing']"));

        assertEquals(new PrimitiveValue("str"),
                resolveLowercased(context, "e.attr['STRING']"));
        assertEquals(new PrimitiveValue("str"),
                resolveLowercased(context, "e.attr['caps']"));

        // Check that the context fall through unknown variables
        assertNull(resolveLowercased(context, "test"));
    }

    @Test
    public void testNativeAttributeContext()
    {
        NativeAttributeContext context = new NativeAttributeContext(activityRule.getActivity());

        assertEquals(new PrimitiveValue(String.valueOf(API_LEVEL)),
                resolveLowercased(context, "b.lvl"));
        assertEquals(PrimitiveValue.nilValue(), resolveLowercased(context, "b.foobar"));

        // Check that the context fall through unknown variables
        assertNull(resolveLowercased(context, "test"));
    }

    @Test
    public void testUserAttributeContext()
    {
        Map<String, Set<String>> tagCollections = new HashMap<>();
        tagCollections.put("collection1", new HashSet<>(Arrays.asList("foo", "bar", "foo")));

        HashMap<String, UserAttribute> attributes = new HashMap<>();
        attributes.put("c.str", new UserAttribute("string", AttributeType.STRING));
        attributes.put("c.long", new UserAttribute(2, AttributeType.LONG));
        attributes.put("c.double", new UserAttribute(234.567, AttributeType.DOUBLE));
        attributes.put("c.bool", new UserAttribute(true, AttributeType.BOOL));
        attributes.put("c.date", new UserAttribute(new Date(1536325808234L), AttributeType.DATE));
        attributes.put("c.null", new UserAttribute(Null.NULL, AttributeType.DELETED));
        attributes.put("c.inconsistent", new UserAttribute(2, AttributeType.STRING));

        MockDataSource dataSource = new MockDataSource(attributes, tagCollections);

        UserAttributeContext context = new UserAttributeContext(dataSource);

        assertEquals(PrimitiveValue.nilValue(), resolveLowercased(context, "c.missing"));
        assertEquals(new PrimitiveValue("string"), resolveLowercased(context, "c.str"));
        assertEquals(new PrimitiveValue(2), resolveLowercased(context, "c.long"));
        assertEquals(new PrimitiveValue(234.567), resolveLowercased(context, "c.double"));
        assertEquals(new PrimitiveValue(true), resolveLowercased(context, "c.bool"));
        assertEquals(new PrimitiveValue(1536325808234.), resolveLowercased(context, "c.date"));
        assertEquals(PrimitiveValue.nilValue(), resolveLowercased(context, "c.null"));
        assertEquals(PrimitiveValue.nilValue(), resolveLowercased(context, "c.inconsistent"));

        assertEquals(PrimitiveValue.nilValue(), resolveLowercased(context, "t.missing"));
        assertEquals(new PrimitiveValue(new HashSet<>(Arrays.asList("foo", "bar"))),
                resolveLowercased(context, "t.collection1"));

        // Check that the context fall through unknown variables
        assertNull(resolveLowercased(context, "test"));
    }

    @Nullable
    private Value resolveSimple(EvaluationContext ctx, String name)
    {
        return ctx.resolveVariableNamed(name);
    }

    @Nullable
    private Value resolveLowercased(EvaluationContext ctx, String name)
    {
        return ctx.resolveVariableNamed(name.toLowerCase(Locale.US));
    }
}

class SimpleVariableContext implements EvaluationContext
{
    private final String name;

    private final Value value;

    SimpleVariableContext(String name, Value value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public Value resolveVariableNamed(String name)
    {
        if (name.toLowerCase(Locale.US).equals(this.name)) {
            return value;
        }

        return null;
    }
}

class AlternatingContext implements EvaluationContext
{
    private Value value;

    private boolean shouldReturnValue = true;

    AlternatingContext(Value value)
    {
        this.value = value;
    }

    @Override
    public Value resolveVariableNamed(String name)
    {
        if (shouldReturnValue) {
            shouldReturnValue = false;
            return value;
        }
        return null;
    }
}

class MockDataSource implements UserDatasource
{
    private HashMap<String, UserAttribute> mockedAttributes;

    private Map<String, Set<String>> mockedTags;

    MockDataSource(HashMap<String, UserAttribute> attributes,
                   Map<String, Set<String>> tagCollections)
    {
        this.mockedAttributes = attributes;
        this.mockedTags = tagCollections;
    }

    @NonNull
    @Override
    public Map<String, Set<String>> getTagCollections()
    {
        return mockedTags;
    }

    @NonNull
    @Override
    public HashMap<String, UserAttribute> getAttributes()
    {
        return mockedAttributes;
    }

    @Override
    public void close() {}

    @Override
    public void acquireTransactionLock(long changeset) throws UserDatabaseException {}

    @Override
    public void commitTransaction() throws UserDatabaseException {}

    @Override
    public void rollbackTransaction() throws UserDatabaseException {}

    @Override
    public void setAttribute(@NonNull String key, long attribute) throws UserDatabaseException {}

    @Override
    public void setAttribute(@NonNull String key, double attribute) throws UserDatabaseException {}

    @Override
    public void setAttribute(@NonNull String key, boolean attribute) throws UserDatabaseException {}

    @Override
    public void setAttribute(@NonNull String key,
                             @NonNull String attribute) throws UserDatabaseException
    {}

    @Override
    public void setAttribute(@NonNull String key,
                             @NonNull Date attribute) throws UserDatabaseException
    {}

    @Override
    public void removeAttribute(@NonNull String key) throws UserDatabaseException {}

    @Override
    public void addTag(@NonNull String collection,
                       @NonNull String tag) throws UserDatabaseException
    {}

    @Override
    public void removeTag(@NonNull String collection,
                          @NonNull String tag) throws UserDatabaseException
    {}

    @Override
    public void clear() {}

    @Override
    public void clearTags() {}

    @Override
    public void clearTags(String collection) {}

    @Override
    public void clearAttributes() {}

    @Override
    public String printDebugDump()
    {
        return null;
    }
}