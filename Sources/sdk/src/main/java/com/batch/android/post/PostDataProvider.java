package com.batch.android.post;

/**
 * Interface for post data provider
 *
 * @param <T> type of data
 */
public interface PostDataProvider<T> {
    /**
     * Get modifiable raw data.<br>
     * The given data should be modifiable.
     *
     * @return
     */
    T getRawData();

    /**
     * Get the data
     *
     * @return
     */
    byte[] getData();

    /**
     * Get the content type of the data
     *
     * @return
     */
    String getContentType();

    /**
     * Checks whether this provider is empty or not.
     * @return true if empty
     */
    boolean isEmpty();
}
