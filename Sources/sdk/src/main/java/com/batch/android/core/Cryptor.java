package com.batch.android.core;

/**
 * Interface for cryptor.
 *
 */
interface Cryptor {
    /**
     * Encrypt the given byte array
     *
     * @param tocrypt
     * @return the encrypted byte array
     */
    byte[] encrypt(byte[] tocrypt);

    /**
     * Encrypt the given string
     *
     * @param tocrypt
     * @return the encrypted string
     * @throws IllegalAccessError if the cryptor doesn't support string crypting
     */
    String encrypt(String tocrypt);

    /**
     * Decrypt the given byte array
     *
     * @param crypted
     * @return the decrypted byte array
     */
    byte[] decrypt(byte[] crypted);

    /**
     * Decrypt the given string
     *
     * @param string
     * @return the decrypted string
     * @throws IllegalAccessError if the cryptor doesn't support string crypting
     */
    String decrypt(String string);

    /**
     * Decrypt the given string and returns bytes
     *
     * @param string
     * @return
     * @throws IllegalAccessError if the cryptor doesn't support string crypting
     */
    byte[] decryptToByte(String string);
}
