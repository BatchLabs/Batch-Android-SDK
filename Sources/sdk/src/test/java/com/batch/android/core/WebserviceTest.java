package com.batch.android.core;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.post.PostDataProvider;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * Webservice tests
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WebserviceTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testRequestSignature() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept-Encoding", Collections.singletonList("gzip"));
        headers.put("Accept-Language", Collections.singletonList("en-US"));
        headers.put("Content-Type", Collections.singletonList("application/octet-stream"));
        headers.put(
            "UserAgent",
            Collections.singletonList(
                "com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)"
            )
        );
        headers.put("X-Batch-Accept-Cipher", Collections.singletonList("2"));
        headers.put("X-Batch-Content-Cipher", Collections.singletonList("2"));
        headers.put("Content-SHA1", Collections.singletonList("pG+tIWKFrPjoZ4RHGLE4/mQllCE="));

        URL url = new URL("https://batch.com/le/testy/test?hip=hop&hop=hip");
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getRequestMethod()).thenReturn("Post");
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getRequestProperties()).thenReturn(headers);
        Mockito
            .when(connection.getRequestProperty(Mockito.anyString()))
            .then(
                (Answer<String>) invocation -> {
                    String key = invocation.getArgument(0);
                    List<String> prop = headers.get(key);
                    if (prop != null) {
                        return prop.get(0);
                    }
                    Assert.fail();
                    return null;
                }
            );

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);
        byte[] body =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد".getBytes();

        ws.addRequestSignatures(connection, body);
        Mockito.verify(connection, Mockito.times(2)).setRequestProperty(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(connection).setRequestProperty("Content-SHA1", "pG+tIWKFrPjoZ4RHGLE4/mQllCE=\n");
        Mockito
            .verify(connection)
            .setRequestProperty(
                "X-Batch-Signature",
                "SHA256 accept-encoding,accept-language,content-sha1,content-type,useragent,x-batch-accept-cipher,x-batch-content-cipher Sl1aV6+G2kq8oSUxp/NFYrBaRor2mVaGBaBVDobt9As=\n"
            );
    }

    @Test
    public void testRequestSignatureNoBody() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept-Encoding", Collections.singletonList("gzip"));
        headers.put("Accept-Language", Collections.singletonList("en-US"));
        headers.put("Content-Type", Collections.singletonList("application/octet-stream"));
        headers.put(
            "UserAgent",
            Collections.singletonList(
                "com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)"
            )
        );
        headers.put("X-Batch-Accept-Cipher", Collections.singletonList("2"));
        headers.put("X-Batch-Content-Cipher", Collections.singletonList("2"));

        URL url = new URL("https://batch.com/le/testy/test?hip=hop&hop=hip");
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getRequestMethod()).thenReturn("Post");
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getRequestProperties()).thenReturn(headers);
        Mockito
            .when(connection.getRequestProperty(Mockito.anyString()))
            .then(
                (Answer<String>) invocation -> {
                    String key = invocation.getArgument(0);
                    List<String> prop = headers.get(key);
                    if (prop != null) {
                        return prop.get(0);
                    }
                    Assert.fail();
                    return null;
                }
            );

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        ws.addRequestSignatures(connection, null);
        Mockito.verify(connection, Mockito.times(1)).setRequestProperty(Mockito.anyString(), Mockito.anyString());
        Mockito
            .verify(connection)
            .setRequestProperty(
                "X-Batch-Signature",
                "SHA256 accept-encoding,accept-language,content-type,useragent,x-batch-accept-cipher,x-batch-content-cipher PZkPIgKkZxjFi9herfVlH197Mn56/SlCgl2hbftUGEI=\n"
            );
    }

    @Test
    public void testRequestSignatureNoHeaders() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();

        URL url = new URL("https://batch.com/foo/BAR?param=value");
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getRequestMethod()).thenReturn("Post");
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getRequestProperties()).thenReturn(headers);
        Mockito
            .when(connection.getRequestProperty(Mockito.anyString()))
            .then(
                (Answer<String>) invocation -> {
                    return null;
                }
            );

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        ws.addRequestSignatures(connection, null);
        Mockito.verify(connection, Mockito.times(1)).setRequestProperty(Mockito.anyString(), Mockito.anyString());
        Mockito
            .verify(connection)
            .setRequestProperty("X-Batch-Signature", "SHA256 qNS6Dgmo088IDzJWgiDoCVeLeViBP/Sa7g02UHXd75o=\n");
    }

    @Test
    public void testRootURL() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/octet-stream"));
        headers.put(
            "UserAgent",
            Collections.singletonList(
                "com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)"
            )
        );

        URL url = new URL("https://batch.com");
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getRequestMethod()).thenReturn("Post");
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getRequestProperties()).thenReturn(headers);
        Mockito
            .when(connection.getRequestProperty(Mockito.anyString()))
            .then(
                (Answer<String>) invocation -> {
                    String key = invocation.getArgument(0);
                    List<String> prop = headers.get(key);
                    if (prop != null) {
                        return prop.get(0);
                    }
                    Assert.fail();
                    return null;
                }
            );

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        ws.addRequestSignatures(connection, null);
        Mockito.verify(connection, Mockito.times(1)).setRequestProperty(Mockito.anyString(), Mockito.anyString());
        Mockito
            .verify(connection)
            .setRequestProperty(
                "X-Batch-Signature",
                "SHA256 content-type,useragent joPlMyjyBzT2zIiLgo5P3LHmeB9+EraC9xZZz1CITJw=\n"
            );
    }

    @Test
    public void testBodySignature() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/octet-stream"));
        headers.put(
            "UserAgent",
            Collections.singletonList(
                "com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)"
            )
        );

        URL url = new URL("https://batch.com");
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getRequestMethod()).thenReturn("Post");
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getRequestProperties()).thenReturn(headers);
        Mockito
            .when(connection.getRequestProperty(Mockito.anyString()))
            .then(
                (Answer<String>) invocation -> {
                    String key = invocation.getArgument(0);
                    List<String> prop = headers.get(key);
                    if (prop != null) {
                        return prop.get(0);
                    }
                    Assert.fail();
                    return null;
                }
            );

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        List<String> keys = new ArrayList<>();
        keys.add("Content-Type");
        keys.add("UserAgent");

        Assert.assertEquals(
            "POST /\n" +
            "content-type: application/octet-stream\n" +
            "useragent: com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)",
            ws.getSignatureBody(connection, keys)
        );

        Assert.assertEquals(2, keys.size());
        Assert.assertEquals("Content-Type", keys.get(0));
        Assert.assertEquals("UserAgent", keys.get(1));
    }

    @Test
    public void testBodySignatureWithInvalidHeaders() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(null, null);
        headers.put("NullHeader", Collections.singletonList(null));
        headers.put("EmptyHeader", Collections.singletonList(""));
        headers.put("Content-Type", Collections.singletonList("application/octet-stream"));
        headers.put(
            "UserAgent",
            Collections.singletonList(
                "com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)"
            )
        );

        URL url = new URL("https://batch.com");
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getRequestMethod()).thenReturn("Post");
        Mockito.when(connection.getURL()).thenReturn(url);
        Mockito.when(connection.getRequestProperties()).thenReturn(headers);
        Mockito
            .when(connection.getRequestProperty(Mockito.anyString()))
            .then(
                (Answer<String>) invocation -> {
                    String key = invocation.getArgument(0);
                    List<String> prop = headers.get(key);
                    if (prop != null) {
                        return prop.get(0);
                    }
                    Assert.fail();
                    return null;
                }
            );

        Webservice ws = new WebserviceTest.GeneralWebservice(appContext);

        List<String> keys = new ArrayList<>();
        keys.add("NullHeader");
        keys.add("Content-Type");
        keys.add(null);
        keys.add("UserAgent");
        keys.add("EmptyHeader");

        Assert.assertEquals(
            "POST /\n" +
            "content-type: application/octet-stream\n" +
            "useragent: com.batch.android/1.15.2 com.batch.android.sample.gradle/3.4.0 (Android SDK built for x86;Android 10)",
            ws.getSignatureBody(connection, keys)
        );

        Assert.assertEquals(2, keys.size());
        Assert.assertEquals("Content-Type", keys.get(0));
        Assert.assertEquals("UserAgent", keys.get(1));
    }

    public static class GeneralWebservice extends Webservice {

        protected GeneralWebservice(Context context) throws MalformedURLException {
            super(context, RequestType.POST, "http://google.com");
        }

        @Override
        protected PostDataProvider<?> getPostDataProvider() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getURLSorterPatternParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getCryptorTypeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getCryptorModeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getPostCryptorTypeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getReadCryptorTypeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getSpecificConnectTimeoutKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getSpecificReadTimeoutKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getSpecificRetryCountKey() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
