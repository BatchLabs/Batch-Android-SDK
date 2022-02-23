package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import org.junit.Test;

/**
 * Test URL Builder
 *
 */
public class URLBuilderTest {

    /**
     * Test a simple url without paramters
     *
     * @throws Exception
     */
    @Test
    public void testSimpleURLBuild() throws Exception {
        String url = "http://google.com/";

        URLBuilder builder = new URLBuilder(url, URLBuilder.CryptorMode.ALL, null);
        URL buildedURL = builder.build();

        assertEquals(url, buildedURL.toString());
    }

    /**
     * Test a simple url with parameters
     *
     * @throws Exception
     */
    @Test
    public void testParametersURLBuild() throws Exception {
        String url = "http://google.com?key1=value1&key2=value2";

        URLBuilder builder = new URLBuilder(url, URLBuilder.CryptorMode.ALL, null);
        URL buildedURL = builder.build();

        assertTrue(buildedURL.toString().contains("key1=value1"));
        assertTrue(buildedURL.toString().contains("key2=value2"));
    }

    /**
     * Test an url with parameters sorted
     *
     * @throws Exception
     */
    @Test
    public void testParametersSortBuild() throws Exception {
        String url = "http://google.com?key2=value2&key1=value1";
        String sortedUrl = "http://google.com?key1=value1&key2=value2";

        URLBuilder builder = new URLBuilder(url, URLBuilder.CryptorMode.ALL, null);
        URL buildedURL = builder.build(new PatternURLSorter("key1,key2"), null);

        assertEquals(sortedUrl, buildedURL.toString());
    }

    /**
     * Test an url with parameters crypted in ALL mode
     *
     * @throws Exception
     */
    @Test
    public void testCryptedAllBuild() throws Exception {
        String url = "http://google.com?key1=value1&key2=value2";
        String cryptedURL = "http://google.com?o=crypted";

        URLBuilder builder = new URLBuilder(url, URLBuilder.CryptorMode.ALL, null);
        URL buildedURL = builder.build(null, new StubCryptor());

        assertEquals(cryptedURL, buildedURL.toString());
    }

    /**
     * Test an url with parameters crypted in VALUE mode
     *
     * @throws Exception
     */
    @Test
    public void testCryptedValuesBuild() throws Exception {
        String url = "http://google.com?key1=value1&key2=value2";

        URLBuilder builder = new URLBuilder(url, URLBuilder.CryptorMode.VALUE, null);
        URL buildedURL = builder.build(null, new StubCryptor());

        assertTrue(buildedURL.toString().contains("key1=crypted"));
        assertTrue(buildedURL.toString().contains("key2=crypted"));
    }

    /**
     * Test an url with parameters crypted in EACH mode
     *
     * @throws Exception
     */
    @Test
    public void testCryptedEachBuild() throws Exception {
        String url = "http://google.com?key1=value1&key2=value2";
        String cryptedUrl = "http://google.com?crypted=crypted&crypted=crypted";

        URLBuilder builder = new URLBuilder(url, URLBuilder.CryptorMode.EACH, null);
        URL buildedURL = builder.build(null, new StubCryptor());

        assertEquals(cryptedUrl, buildedURL.toString());
    }

    // --------------------------------------------------->

    /**
     * Stub cryptor
     *
     */
    protected static class StubCryptor implements Cryptor {

        @Override
        public byte[] encrypt(byte[] tocrypt) {
            return tocrypt;
        }

        @Override
        public String encrypt(String tocrypt) {
            return "crypted";
        }

        @Override
        public byte[] decrypt(byte[] crypted) {
            return crypted;
        }

        @Override
        public String decrypt(String string) {
            return decrypt(string);
        }

        @Override
        public byte[] decryptToByte(String string) {
            return string.getBytes();
        }
    }
}
