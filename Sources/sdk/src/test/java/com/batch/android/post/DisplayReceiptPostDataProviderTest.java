package com.batch.android.post;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.batch.android.displayreceipt.DisplayReceipt;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Test for DisplayReceiptPostDataProviderTest
 */
public class DisplayReceiptPostDataProviderTest {

    @Test
    public void testData() {
        List<DisplayReceipt> receiptList = new ArrayList<>();
        // prettier-ignore
        receiptList.add(
            DisplayReceipt.unpack(new byte[]{-50, 0, 1, -30, 64, -62, 19, -121, -95, 116, -71, 106, 101,
                    45, 115, 117, 105, 115, 45, 117, 110, 45, 99, 97, 109, 112, 97, 105, 103, 110, 45,
                    116, 111, 107, 101, 110, -92, 110, 117, 108, 108, -64, -94, 100, 105, -75, 106, 101,
                    45, 115, 117, 105, 115, 45, 117, 110, 45, 105, 110, 115, 116, 97, 108, 108, 45, 105,
                    100, -94, 97, 107, -78, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 101, 45, 97,
                    112, 105, 107, 101, 121, -92, 108, 105, 115, 116, -107, -62, -92, 121, 111, 108,
                    111, -53, 64, 57, -78, -116, 21, 76, -104, 95, -51, 2, -114, -64, -93, 109, 97, 112,
                    -107, -62, -92, 121, 111, 108, 111, -53, 64, 57, -78, -116, 21, 76, -104, 95, -51,
                    2, -114, -64, -95, 110, -70, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 45,
                    110, 111, 116, 105, 102, 105, 99, 97, 116, 105, 111, 110, 45, 105, 100, -125, -95,
                    101, -72, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 45, 101, 120, 112, 101,
                    114, 105, 109, 101, 110, 116, 45, 105, 100, -95, 118, -75, 106, 101, 45, 115, 117,
                    105, 115, 45, 117, 110, 45, 118, 97, 114, 105, 101, 110, 116, 45, 105, 100, -95,
                    105, -78, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 45, 115, 101, 110, 100,
                    45, 105, 100}
                )
        );

        // prettier-ignore
        byte[] body = new byte[]{-111, -50, 0, 1, -30, 64, -62, 19, -121, -95, 116, -71, 106, 101,
                45, 115, 117, 105, 115, 45, 117, 110, 45, 99, 97, 109, 112, 97, 105, 103, 110, 45,
                116, 111, 107, 101, 110, -92, 110, 117, 108, 108, -64, -94, 100, 105, -75, 106, 101,
                45, 115, 117, 105, 115, 45, 117, 110, 45, 105, 110, 115, 116, 97, 108, 108, 45, 105,
                100, -94, 97, 107, -78, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 101, 45, 97,
                112, 105, 107, 101, 121, -92, 108, 105, 115, 116, -107, -62, -92, 121, 111, 108,
                111, -53, 64, 57, -78, -116, 21, 76, -104, 95, -51, 2, -114, -64, -93, 109, 97, 112,
                -107, -62, -92, 121, 111, 108, 111, -53, 64, 57, -78, -116, 21, 76, -104, 95, -51,
                2, -114, -64, -95, 110, -70, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 45,
                110, 111, 116, 105, 102, 105, 99, 97, 116, 105, 111, 110, 45, 105, 100, -125, -95,
                101, -72, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 45, 101, 120, 112, 101,
                114, 105, 109, 101, 110, 116, 45, 105, 100, -95, 118, -75, 106, 101, 45, 115, 117,
                105, 115, 45, 117, 110, 45, 118, 97, 114, 105, 101, 110, 116, 45, 105, 100, -95,
                105, -78, 106, 101, 45, 115, 117, 105, 115, 45, 117, 110, 45, 115, 101, 110, 100,
                45, 105, 100};

        DisplayReceiptPostDataProvider provider = new DisplayReceiptPostDataProvider(receiptList);
        assertEquals("application/msgpack", provider.getContentType());
        assertArrayEquals(receiptList.toArray(), provider.getRawData().toArray());
        assertArrayEquals(body, provider.getData());
    }

    @Test
    public void testIsEmpty() {
        List<DisplayReceipt> receiptList = new ArrayList<>();
        DisplayReceiptPostDataProvider provider = new DisplayReceiptPostDataProvider(receiptList);
        assertTrue(provider.isEmpty());

        receiptList.add(null);
        provider = new DisplayReceiptPostDataProvider(receiptList);
        assertFalse(provider.isEmpty());
    }
}
