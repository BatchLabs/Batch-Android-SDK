package com.batch.android.messaging.model;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.Size2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case that checks that all the message formats are serializable
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ModelSerializationTest {

    @Test
    public void testAlertMessage() throws JSONException {
        AlertMessage m = new AlertMessage();
        fillMessage(m);
        m.titleText = "foo";
        m.cancelButtonText = "bar";
        m.acceptCTA = makeCTA();
        serialize(m);
    }

    @Test
    public void testBannerMessage() throws JSONException {
        BannerMessage m = new BannerMessage();
        fillMessage(m);
        fillBaseBannerMessage(m);

        serialize(m);
    }

    @Test
    public void testImageMessage() throws JSONException {
        ImageMessage m = new ImageMessage();
        fillMessage(m);
        m.css = "test";
        m.globalTapAction = makeAction();
        m.globalTapDelay = 500;
        m.allowSwipeToDismiss = false;
        m.imageURL = "https://batch.com";
        m.imageDescription = "foobar";
        m.imageSize = new Size2D(2, 3);
        m.autoCloseDelay = 300;
        m.isFullscreen = true;
        serialize(m);
    }

    @Test
    public void testModalMessage() throws JSONException {
        ModalMessage m = new ModalMessage();
        fillMessage(m);
        fillBaseBannerMessage(m);
    }

    @Test
    public void testUniversalMessage() throws JSONException {
        UniversalMessage m = new UniversalMessage();
        fillMessage(m);
        m.css = "test";
        m.headingText = "test";
        m.titleText = "test";
        m.subtitleText = null;
        m.ctas = makeCTAs();
        m.heroImageURL = "https://google.fr";
        m.videoURL = "https://google.fr";
        m.heroDescription = "truc";
        m.showCloseButton = true;
        m.attachCTAsBottom = true;
        m.stackCTAsHorizontally = false;
        m.stretchCTAsHorizontally = false;
        m.flipHeroHorizontal = true;
        m.flipHeroVertical = false;
        m.heroSplitRatio = 200.42;
        m.autoCloseDelay = 10;
        serialize(m);
    }

    @Test
    public void testWebViewMessage() throws JSONException {
        WebViewMessage m = new WebViewMessage();
        fillMessage(m);
        m.css = "test";
        m.timeout = 7000;
        m.url = "https://batch.com/";
        serialize(m);
    }

    private void fillBaseBannerMessage(BaseBannerMessage m) throws JSONException {
        m.css = "foo";
        m.titleText = "foo";
        m.globalTapAction = makeAction();
        m.globalTapDelay = 100;
        m.allowSwipeToDismiss = true;
        m.imageURL = "https://batch.com";
        m.imageDescription = "foo";
        m.ctas = makeCTAs();
        m.showCloseButton = false;
        m.autoCloseDelay = 300;
        m.ctaDirection = BaseBannerMessage.CTADirection.VERTICAL;
        serialize(m);
    }

    private void fillMessage(Message m) throws JSONException {
        m.messageIdentifier = "foo";
        m.devTrackingIdentifier = "bar";
        m.bodyText = "foobar";
        m.bodyRawHtml = "<html><b>foo</b></html>";
        m.eventData = new JSONObject("{'foo':'bar'}");
        m.source = Message.Source.LANDING;
    }

    private Action makeAction() throws JSONException {
        return new Action("foo", new JSONObject("{'foo':'bar'}"));
    }

    private CTA makeCTA() throws JSONException {
        return new CTA("foo", "foo", new JSONObject("{'foo':'bar'}"));
    }

    private List<CTA> makeCTAs() throws JSONException {
        List<CTA> ctas = new ArrayList<>(2);
        ctas.add(makeCTA());
        ctas.add(makeCTA());
        return ctas;
    }

    private void serialize(Serializable s) {
        Bundle b = new Bundle();
        b.putSerializable("test", s);
    }
}
