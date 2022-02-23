package com.batch.android.core;

import java.util.ArrayList;

/**
 * A simple extends of ArrayList that is size limited.
 * Add an item if the list is already full will result in deleting the first one.
 *
 * @param <T>
 */
public class FixedSizeArrayList<T> extends ArrayList<T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // --------------------------------------->

    private final int maxSize;

    // --------------------------------------->

    public FixedSizeArrayList(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public boolean add(T t) {
        if (size() >= maxSize) {
            remove(0);
        }

        return super.add(t);
    }
}
