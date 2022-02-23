package com.batch.android.displayreceipt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.ByteArrayHelper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DisplayReceiptTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testPack() {
        String hexPayload =
            "CE0001E240C21387A174B96A652D737569732D756E2D63616D706169676E2D746F6" +
            "B656EA46E756C6CC0A26469B56A652D737569732D756E2D696E7374616C6C2D6964A2616BB26A65" +
            "2D737569732D756E652D6170696B6579A46C69737495C2A474657374CB4039B28C154C985FCD028" +
            "EC0A36D617085A4626F6F6CC2A46E756C6CC0A5666C6F6174CB4050123D70A3D70AA46C69737495" +
            "C2A474657374CB4039B28C154C985FCD028EC0A3696E74CD028EA16EBA6A652D737569732D756E2" +
            "D6E6F74696669636174696F6E2D696483A165B86A652D737569732D756E2D6578706572696D656E" +
            "742D6964A176B56A652D737569732D756E2D76617269656E742D6964A169B26A652D737569732D7" +
            "56E2D73656E642D6964";

        List<Object> nestedList = new ArrayList<>();
        nestedList.add(false);
        nestedList.add("test");
        nestedList.add(25.69745);
        nestedList.add(654L);
        nestedList.add(null);

        Map<String, Object> nestedOd = new HashMap<>();
        nestedOd.put("bool", false);
        nestedOd.put("int", 654);
        nestedOd.put("float", 64.285);
        nestedOd.put("list", nestedList);
        nestedOd.put("null", null);

        Map<String, Object> od = new HashMap<>();
        od.put("n", "je-suis-un-notification-id");
        od.put("t", "je-suis-un-campaign-token");
        od.put("ak", "je-suis-une-apikey");
        od.put("di", "je-suis-un-install-id");
        od.put("null", null);
        od.put("map", nestedOd);
        od.put("list", nestedList);

        Map<String, Object> ed = new HashMap<>();
        ed.put("i", "je-suis-un-send-id");
        ed.put("e", "je-suis-un-experiment-id");
        ed.put("v", "je-suis-un-varient-id");

        byte[] packedMessage = DisplayReceipt.pack(123456, false, 19, od, ed);
        assertNotNull(packedMessage);
        assertEquals(hexPayload, ByteArrayHelper.bytesToHex(packedMessage));
    }

    @Test
    public void testPackEmptyMap() {
        String hexPayload = "CF0000000F3F02B57DC3CD19B9C0C0";
        byte[] packedMessage = DisplayReceipt.pack(65481651581L, true, 6585, new HashMap<>(), new HashMap<>());
        assertNotNull(packedMessage);
        assertEquals(hexPayload, ByteArrayHelper.bytesToHex(packedMessage));
    }

    @Test
    public void testPackNull() {
        String hexPayload = "CF0000000F3F02B57DC3CD19B9C0C0";
        byte[] packedMessage = DisplayReceipt.pack(65481651581L, true, 6585, null, null);
        assertNotNull(packedMessage);
        assertEquals(hexPayload, ByteArrayHelper.bytesToHex(packedMessage));
    }

    @Test
    public void testUnpack() {
        String hexPayload =
            "CE0001E240C21387A174B96A652D737569732D756E2D63616D706169676E2D746F6" +
            "B656EA46E756C6CC0A26469B56A652D737569732D756E2D696E7374616C6C2D6964A2616BB26A65" +
            "2D737569732D756E652D6170696B6579A46C69737495C2A4796F6C6FCB4039B28C154C985FCD028" +
            "EC0A36D617085A4626F6F6CC2A46E756C6CC0A5666C6F6174CB4050123D70A3D70AA46C69737495" +
            "C2A4796F6C6FCB4039B28C154C985FCD028EC0A3696E74CD028EA16EBA6A652D737569732D756E2" +
            "D6E6F74696669636174696F6E2D696483A165B86A652D737569732D756E2D6578706572696D656E" +
            "742D6964A176B56A652D737569732D756E2D76617269656E742D6964A169B26A652D737569732D7" +
            "56E2D73656E642D6964";

        DisplayReceipt displayReceipt = DisplayReceipt.unpack(ByteArrayHelper.hexToBytes(hexPayload));

        assertNotNull(displayReceipt);
        assertEquals(123456, displayReceipt.getTimestamp());
        assertFalse(displayReceipt.isReplay());
        assertEquals(19, displayReceipt.getSendAttempt());
        assertEquals("je-suis-un-notification-id", displayReceipt.getOd().get("n").toString());
        assertEquals("je-suis-un-campaign-token", displayReceipt.getOd().get("t").toString());
        assertEquals("null", displayReceipt.getOd().get("null").toString());

        assertEquals("je-suis-un-send-id", displayReceipt.getEd().get("i").toString());
        assertEquals("je-suis-un-experiment-id", displayReceipt.getEd().get("e").toString());
        assertEquals("je-suis-un-varient-id", displayReceipt.getEd().get("v").toString());
    }

    @Test
    public void testUnpackNull() {
        String hexPayload = "CF0000000F3F02B57DC3CD19B9C0C0";
        DisplayReceipt displayReceipt = DisplayReceipt.unpack(ByteArrayHelper.hexToBytes(hexPayload));
        assertNotNull(displayReceipt);
        assertNull(displayReceipt.getOd());
        assertNull(displayReceipt.getEd());
    }

    @Test
    public void testUnpackPackUnpack() throws IOException {
        String hexPayload =
            "CE0001E240C21387A174B96A652D737569732D756E2D63616D706169676E2D746F6" +
            "B656EA46E756C6CC0A26469B56A652D737569732D756E2D696E7374616C6C2D6964A2616BB26A65" +
            "2D737569732D756E652D6170696B6579A46C69737495C2A4796F6C6FCB4039B28C154C985FCD028" +
            "EC0A36D617085A4626F6F6CC2A46E756C6CC0A5666C6F6174CB4050123D70A3D70AA46C69737495" +
            "C2A4796F6C6FCB4039B28C154C985FCD028EC0A3696E74CD028EA16EBA6A652D737569732D756E2" +
            "D6E6F74696669636174696F6E2D696483A165B86A652D737569732D756E2D6578706572696D656E" +
            "742D6964A176B56A652D737569732D756E2D76617269656E742D6964A169B26A652D737569732D7" +
            "56E2D73656E642D6964";

        DisplayReceipt displayReceipt = DisplayReceipt.unpack(ByteArrayHelper.hexToBytes(hexPayload));
        assertNotNull(displayReceipt);

        File outputFile = File.createTempFile("tmp", ".bin", appContext.getCacheDir());

        byte[] packedMessage = displayReceipt.packAndWrite(outputFile);
        assertNotNull(packedMessage);
        assertTrue(outputFile.exists());
        assertEquals(hexPayload, ByteArrayHelper.bytesToHex(packedMessage));

        DisplayReceipt displayReceipt2 = DisplayReceipt.unpack(packedMessage);
        assertNotNull(displayReceipt2);
        assertEquals(123456, displayReceipt2.getTimestamp());
        assertFalse(displayReceipt2.isReplay());
        assertEquals(19, displayReceipt2.getSendAttempt());
        assertEquals("je-suis-un-notification-id", displayReceipt2.getOd().get("n").toString());
        assertEquals("je-suis-un-campaign-token", displayReceipt2.getOd().get("t").toString());
        assertEquals("null", displayReceipt2.getOd().get("null").toString());

        assertEquals("je-suis-un-send-id", displayReceipt2.getEd().get("i").toString());
        assertEquals("je-suis-un-experiment-id", displayReceipt2.getEd().get("e").toString());
        assertEquals("je-suis-un-varient-id", displayReceipt2.getEd().get("v").toString());

        outputFile.delete();
    }

    @Test
    public void testUnpackPackUnpackNull() throws IOException {
        String hexPayload = "CF0000000F3F02B57DC3CD19B9C0C0";

        DisplayReceipt displayReceipt = DisplayReceipt.unpack(ByteArrayHelper.hexToBytes(hexPayload));
        assertNotNull(displayReceipt);

        File outputFile = File.createTempFile("tmp", ".bin", appContext.getCacheDir());

        byte[] packedMessage = displayReceipt.packAndWrite(outputFile);
        assertNotNull(packedMessage);
        assertTrue(outputFile.exists());
        assertEquals(hexPayload, ByteArrayHelper.bytesToHex(packedMessage));

        DisplayReceipt displayReceipt2 = DisplayReceipt.unpack(packedMessage);
        assertNotNull(displayReceipt2);
        assertEquals(65481651581L, displayReceipt2.getTimestamp());
        assertTrue(displayReceipt2.isReplay());
        assertEquals(6585, displayReceipt2.getSendAttempt());
        assertNull(displayReceipt2.getOd());
        assertNull(displayReceipt2.getEd());

        outputFile.delete();
    }
}
