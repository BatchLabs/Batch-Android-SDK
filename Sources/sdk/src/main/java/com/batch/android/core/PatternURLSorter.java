package com.batch.android.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorter of parameters by pattern
 *
 */
class PatternURLSorter {

    /**
     * Pattern list.
     */
    private List<String> pattern = new ArrayList<>();

    // ------------------------------------------>

    /**
     * Default constructor.
     */
    protected PatternURLSorter() {}

    /**
     * Standard constructor.
     *
     * @param pattern
     */
    protected PatternURLSorter(List<String> pattern) {
        if (pattern != null) {
            this.pattern.addAll(pattern);
        }
    }

    /**
     * Comma separated string constructor.
     *
     * @param pattern
     */
    protected PatternURLSorter(String pattern) {
        if (pattern != null && pattern.length() > 0) {
            this.pattern.addAll(Arrays.asList(pattern.split(",")));
        }
    }

    // ------------------------------------------>

    /**
     * Sort list values according to the URLSorter pattern.
     *
     * @param toSort
     * @return Sorted values.
     */
    public List<String> getKeysOrdered(List<String> toSort) {
        return order(new ArrayList<>(toSort));
    }

    /**
     * Sort set values according to the URLSorter pattern.
     *
     * @param toSort
     * @return Sorted values.
     */
    public List<String> getKeysOrdered(Set<String> toSort) {
        return order(new HashSet<>(toSort));
    }

    /**
     * Sort keys string of a map according to the URLSorter pattern.
     *
     * @param map
     * @return Sorted keys.
     */
    public List<String> getKeysOrdered(Map<String, ?> map) {
        return order(new HashSet<>(map.keySet()));
    }

    // ---------------------------------------------->

    /**
     * Order a collection with the pattern
     *
     * @param toSort
     * @return
     */
    private List<String> order(Collection<String> toSort) {
        if (pattern == null || pattern.size() == 0) {
            return new ArrayList<>(toSort);
        }

        List<String> sorted = new ArrayList<>();

        boolean addOthers = false;
        for (String string : pattern) {
            // Wildchar case.
            if (string.equals("*")) {
                addOthers = true;
                continue;
            }

            // Add to sorted.
            if (toSort.contains(string)) {
                sorted.add(string);
                toSort.remove(string);
            }
        }

        // Add other parameters (* case) or we failed retreiving all others parameters
        if (addOthers || sorted.size() <= 0) {
            sorted.addAll(toSort);
        }

        return sorted;
    }
}
