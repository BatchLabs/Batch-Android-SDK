package com.batch.android;
/*
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchUserStartTest
{
    private ActivityScenario<TestActivity> scenario;

    @After
    public void tearDown()
    {
        TestUtils.closeScenario(scenario);
    }

    @Test
    public void testCustomDataBeforeStart()
    {
        final Context context = ApplicationProvider.getApplicationContext();
        assertNull(Batch.User.getIdentifier(context));
        assertNull(Batch.User.getRegion(context));
        assertNull(Batch.User.getLanguage(context));

        BatchUserDataEditor editor = Batch.User.editor();
        editor.setIdentifier("jesuisuntest");
        editor.setRegion("FR");
        editor.setLanguage("fr-FR");
        editor.save();

        assertNull(Batch.User.getIdentifier(context));
        assertNull(Batch.User.getRegion(context));
        assertNull(Batch.User.getLanguage(context));

        scenario = ActivityScenario.launch(TestActivity.class);

        assertEquals("jesuisuntest", Batch.User.getIdentifier(context));
        assertEquals("FR", Batch.User.getRegion(context));
        assertEquals("fr-FR", Batch.User.getLanguage(context));

        TestUtils.closeScenario(scenario);

        editor = Batch.User.editor();
        editor.setIdentifier(null);
        editor.setRegion(null);
        editor.setLanguage(null);
        editor.save();

        scenario = ActivityScenario.launch(TestActivity.class);

        assertNull(Batch.User.getIdentifier(context));
        assertNull(Batch.User.getRegion(context));
        assertNull(Batch.User.getLanguage(context));
    }

    @Test
    public void testAttributesBeforeStart()
    {
        final Context context = ApplicationProvider.getApplicationContext();

        BatchUserDataEditor editor = Batch.User.editor();
        editor.setAttribute("today", new Date());
        editor.setAttribute("float_value", 3.2);
        editor.setAttribute("int_value", 4);
        editor.save();

        MockBatchAttributesFetchListener listener = new MockBatchAttributesFetchListener();
        UserDataAccessor.fetchAttributes(context, listener, false);
        Map<String, BatchUserAttribute> result = listener.getAttributes();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        scenario = ActivityScenario.launch(TestActivity.class);

        listener = new MockBatchAttributesFetchListener();
        UserDataAccessor.fetchAttributes(context, listener, false);
        result = listener.getAttributes();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertEquals(3, result.size());
        BatchUserAttribute dateValue = result.get("today");
        assertNotNull(dateValue);
        assertNull(dateValue.getStringValue());
        assertNull(dateValue.getNumberValue());
        assertNull(dateValue.getBooleanValue());
        assertNotNull(dateValue.getDateValue());

        TestUtils.closeScenario(scenario);

        editor = Batch.User.editor();
        editor.clearAttributes();
        editor.save();

        listener = new MockBatchAttributesFetchListener();
        UserDataAccessor.fetchAttributes(context, listener, false);
        result = listener.getAttributes();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertEquals(3, result.size());

        scenario = ActivityScenario.launch(TestActivity.class);

        listener = new MockBatchAttributesFetchListener();
        UserDataAccessor.fetchAttributes(context, listener, false);
        result = listener.getAttributes();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTagCollectionsBeforeStart()
    {
        final Context context = ApplicationProvider.getApplicationContext();

        BatchUserDataEditor editor = Batch.User.editor();
        editor.addTag("collection_1", "tag_1");
        editor.addTag("collection_1", "tag_2");
        editor.addTag("collection_2", "tag_3");
        editor.addTag("collection_3", "TAG_4");
        editor.save();

        MockBatchTagCollectionsFetchListener listener = new MockBatchTagCollectionsFetchListener();
        UserDataAccessor.fetchTagCollections(context, listener, false);
        Map<String, Set<String>> result = listener.getTagCollections();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        scenario = ActivityScenario.launch(TestActivity.class);

        listener = new MockBatchTagCollectionsFetchListener();
        UserDataAccessor.fetchTagCollections(context, listener, false);
        result = listener.getTagCollections();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertEquals(result.size(), 3);
        Set<String> collection1 = result.get("collection_1");
        assertTrue(collection1.contains("tag_2"));
        assertFalse(collection1.contains("tag_3"));
        Set<String> collection3 = result.get("collection_3");
        assertTrue(collection3.contains("tag_4")); // tags are set to lowercase when saved

        TestUtils.closeScenario(scenario);

        editor = Batch.User.editor();
        editor.clearTags();
        editor.save();

        listener = new MockBatchTagCollectionsFetchListener();
        UserDataAccessor.fetchTagCollections(context, listener, false);
        result = listener.getTagCollections();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertEquals(3, result.size());

        scenario = ActivityScenario.launch(TestActivity.class);

        listener = new MockBatchTagCollectionsFetchListener();
        UserDataAccessor.fetchTagCollections(context, listener, false);
        result = listener.getTagCollections();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
*/
