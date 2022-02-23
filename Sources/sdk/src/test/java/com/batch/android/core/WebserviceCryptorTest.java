package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.CryptorFactory.CryptorType;
import java.net.HttpURLConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WebserviceCryptorTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test encrypt & decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecryptV1() throws Exception {
        String string =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد";
        WebserviceCryptor cryptor = new WebserviceCryptor(CryptorType.EAS_BASE64);
        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);
        ws.isDowngradedCipher = true;

        byte[] encrypted = cryptor.encryptData(string.getBytes("utf-8"), ws);

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        byte[] decrypted = cryptor.decryptData(encrypted, ws, connection);

        assertEquals(string, new String(decrypted, "utf-8"));
    }

    /**
     * Test decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testDecryptV1Fallback() throws Exception {
        // private key = wgHD33rM, public = 1oujETvr
        String encrypted =
            "1oujETvrm3YgFpuS2m2nR88XrFQPSuWwl6TaxGbmCNzQnkk1UHYfPNd8KiUWzXefrs/BP3rMXVs1behkBZ8KdCz0ORXJaTTpVnED+PBr5U89X4g8PTBUiGEpUbVRHPBVqjmQCKlon7H/ql9An7M2pgk+sZrU/X+hyBALbFkagSrKpB97sIznbctaDkSNVd93n8RrObvcrTd62K5nL9WuIzSTt4kPuOMIWsvcY0oU4GFebyKZZ6bFiI/Q/6uVRIf403pVRj/nWy6dKuP/AehE6lnd8FYEwHWeaB3dlQ0TuHp49TGiEIxceCJujEO+tSbAyNhmJ536SN/zcGF5n1dL/TBAHANRxA==";
        WebserviceCryptor cryptor = new WebserviceCryptor(CryptorFactory.CryptorType.EAS_BASE64);
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);
        ws.isDowngradedCipher = true;
        byte[] decrypted = cryptor.decryptData(encrypted.getBytes("utf-8"), ws, connection);

        assertEquals(
            "{\"header\":{\"version\":\"1.0\",\"status\":\"OK\"},\"body\":{\"queries\":[{\"id\":\"f9e05677-6789-4112-9244-48db00b13744\",\"promo\":{\"tok\":\"t397Yo95\",\"id\":\"promo1\",\"bundles\":[],\"feat\":[{\"id\":\"UNLOCKABLE1\",\"val\":null}],\"res\":[]},\"code\":\"abcd\",\"status\":\"SUCCESS\"}]}}",
            new String(decrypted, "utf-8")
        );
    }

    /**
     * Test decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testDecryptV1Headers() throws Exception {
        // private key = wgHD33rM, public = 1oujETvr
        String encrypted =
            "1oujETvrm3YgFpuS2m2nR88XrFQPSuWwl6TaxGbmCNzQnkk1UHYfPNd8KiUWzXefrs/BP3rMXVs1behkBZ8KdCz0ORXJaTTpVnED+PBr5U89X4g8PTBUiGEpUbVRHPBVqjmQCKlon7H/ql9An7M2pgk+sZrU/X+hyBALbFkagSrKpB97sIznbctaDkSNVd93n8RrObvcrTd62K5nL9WuIzSTt4kPuOMIWsvcY0oU4GFebyKZZ6bFiI/Q/6uVRIf403pVRj/nWy6dKuP/AehE6lnd8FYEwHWeaB3dlQ0TuHp49TGiEIxceCJujEO+tSbAyNhmJ536SN/zcGF5n1dL/TBAHANRxA==";
        WebserviceCryptor cryptor = new WebserviceCryptor(CryptorFactory.CryptorType.EAS_BASE64);

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getHeaderField(eq("X-Batch-Content-Cipher"))).thenReturn("1");

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);
        byte[] decrypted = cryptor.decryptData(encrypted.getBytes("utf-8"), ws, connection);

        assertNotNull(decrypted);
        assertEquals(
            "{\"header\":{\"version\":\"1.0\",\"status\":\"OK\"},\"body\":{\"queries\":[{\"id\":\"f9e05677-6789-4112-9244-48db00b13744\",\"promo\":{\"tok\":\"t397Yo95\",\"id\":\"promo1\",\"bundles\":[],\"feat\":[{\"id\":\"UNLOCKABLE1\",\"val\":null}],\"res\":[]},\"code\":\"abcd\",\"status\":\"SUCCESS\"}]}}",
            new String(decrypted, "utf-8")
        );
    }

    /**
     * Test encrypt & decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecryptV2() throws Exception {
        String string =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد";
        WebserviceCryptor cryptor = new WebserviceCryptor(CryptorType.EAS_BASE64_GZIP);
        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        byte[] encrypted = cryptor.encryptData(string.getBytes("utf-8"), ws);

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getHeaderField(eq("X-Batch-Content-Cipher"))).thenReturn("2");

        byte[] decrypted = cryptor.decryptData(encrypted, ws, connection);

        assertNotNull(decrypted);
        assertEquals(string, new String(decrypted, "utf-8"));
    }

    /**
     * Test decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testDecryptV2() throws Exception {
        // private key = jgfx4HVJ, public = 1oujETvr
        String encrypted =
            "2prUzTtLfvJoHpf9+kUqhB5RoEmJnu0wnha7VTdojcE38XwE/r+5kS7YeUP2g8xRy9KQsPspda21gsP2lCdh0icqco67u6EkxxMNyl2bdek96HkU37AexcHRHAP2o1GyYjem993nyAj0xn8RF1U8KocCy0SdFryeL/v/RAwmPPaRe11Zs9j4PI7eDZWxPNP7iyqXmogLm9Xu7tftfeJRUjNvHGPLKA6vbLPeHgLsnppKSeJLucgRRdk4FE+vMEyJ+E0WZJHKaf094MpOaxLF5DNocqmCOt36sTyQqQt0xS7t9OvnpvwaLHTjHfsBbF07unoml77PC5lBvIiY1zqJjjAAG2v11w==";
        WebserviceCryptor cryptor = new WebserviceCryptor(CryptorFactory.CryptorType.EAS_BASE64);
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getHeaderField(eq("X-Batch-Content-Cipher"))).thenReturn("2");

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        byte[] decrypted = cryptor.decryptData(encrypted.getBytes("utf-8"), ws, connection);

        assertNotNull(decrypted);
        assertEquals(
            "{\"header\":{\"version\":\"1.0\",\"status\":\"OK\"},\"body\":{\"queries\":[{\"id\":\"f9e05677-6789-4112-9244-48db00b13744\",\"promo\":{\"tok\":\"t397Yo95\",\"id\":\"promo1\",\"bundles\":[],\"feat\":[{\"id\":\"UNLOCKABLE1\",\"val\":null}],\"res\":[]},\"code\":\"abcd\",\"status\":\"SUCCESS\"}]}}",
            new String(decrypted, "utf-8")
        );
    }

    /**
     * Test decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testDecryptV2Headers() throws Exception {
        // private key = jgfx4HVJ, public = 2prUzTtL
        String encrypted =
            "2prUzTtLfvJoHpf9+kUqhB5RoEmJnu0wnha7VTdojcE38XwE/r+5kS7YeUP2g8xRy9KQsPspda21gsP2lCdh0icqco67u6EkxxMNyl2bdek96HkU37AexcHRHAP2o1GyYjem993nyAj0xn8RF1U8KocCy0SdFryeL/v/RAwmPPaRe11Zs9j4PI7eDZWxPNP7iyqXmogLm9Xu7tftfeJRUjNvHGPLKA6vbLPeHgLsnppKSeJLucgRRdk4FE+vMEyJ+E0WZJHKaf094MpOaxLF5DNocqmCOt36sTyQqQt0xS7t9OvnpvwaLHTjHfsBbF07unoml77PC5lBvIiY1zqJjjAAG2v11w==";
        WebserviceCryptor cryptor = new WebserviceCryptor(CryptorFactory.CryptorType.EAS_BASE64);
        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getHeaderField(eq("X-Batch-Content-Cipher"))).thenReturn("2");

        byte[] decrypted = cryptor.decryptData(encrypted.getBytes("utf-8"), ws, connection);

        assertNotNull(decrypted);
        assertEquals(
            "{\"header\":{\"version\":\"1.0\",\"status\":\"OK\"},\"body\":{\"queries\":[{\"id\":\"f9e05677-6789-4112-9244-48db00b13744\",\"promo\":{\"tok\":\"t397Yo95\",\"id\":\"promo1\",\"bundles\":[],\"feat\":[{\"id\":\"UNLOCKABLE1\",\"val\":null}],\"res\":[]},\"code\":\"abcd\",\"status\":\"SUCCESS\"}]}}",
            new String(decrypted, "utf-8")
        );
    }
}
