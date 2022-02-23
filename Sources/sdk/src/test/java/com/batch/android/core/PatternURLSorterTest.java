package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Test pattern url sorter
 *
 */
public class PatternURLSorterTest {

    /**
     * Test that without pattern the sorter dont change order
     *
     * @throws Exception
     */
    @Test
    public void testWithoutPattern() throws Exception {
        List<String> values = getAtoFUnsortedList();

        PatternURLSorter sorter = new PatternURLSorter();

        List<String> out = sorter.getKeysOrdered(values);
        assertEquals(values.get(0), out.get(0));
        assertEquals(values.get(1), out.get(1));
        assertEquals(values.get(2), out.get(2));
        assertEquals(values.get(3), out.get(3));
        assertEquals(values.get(4), out.get(4));
        assertEquals(values.get(5), out.get(5));
    }

    /**
     * Test a simple reorder without wildcard
     *
     * @throws Exception
     */
    @Test
    public void testSimpleReorder() throws Exception {
        List<String> values = getAtoFUnsortedList();

        PatternURLSorter sorter = new PatternURLSorter("a,b,c,d,e,f");

        List<String> out = sorter.getKeysOrdered(values);
        assertEquals("a", out.get(0));
        assertEquals("b", out.get(1));
        assertEquals("c", out.get(2));
        assertEquals("d", out.get(3));
        assertEquals("e", out.get(4));
        assertEquals("f", out.get(5));
    }

    /**
     * Test a reorder with wildcard
     *
     * @throws Exception
     */
    @Test
    public void testWildCardReorder() throws Exception {
        List<String> values = getAtoFUnsortedList();

        PatternURLSorter sorter = new PatternURLSorter("a,b,c,*");

        List<String> out = sorter.getKeysOrdered(values);
        assertEquals("a", out.get(0));
        assertEquals("b", out.get(1));
        assertEquals("c", out.get(2));
        assertEquals("e", out.get(3));
        assertEquals("f", out.get(4));
        assertEquals("d", out.get(5));
    }

    /**
     * Test a reorder with a limited number of values
     *
     * @throws Exception
     */
    @Test
    public void testLimitedReorder() throws Exception {
        List<String> values = getAtoFUnsortedList();

        PatternURLSorter sorter = new PatternURLSorter("a,b,c");

        List<String> out = sorter.getKeysOrdered(values);
        assertEquals(3, out.size());
        assertEquals("a", out.get(0));
        assertEquals("b", out.get(1));
        assertEquals("c", out.get(2));
    }

    // ---------------------------------------->

    /**
     * Create an unsorted list of A to F values
     *
     * @return
     */
    private List<String> getAtoFUnsortedList() {
        List<String> values = new ArrayList<>();
        values.add("e");
        values.add("f");
        values.add("a");
        values.add("c");
        values.add("b");
        values.add("d");

        return values;
    }
}
