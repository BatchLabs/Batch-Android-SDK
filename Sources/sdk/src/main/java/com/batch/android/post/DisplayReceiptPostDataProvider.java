package com.batch.android.post;

import com.batch.android.displayreceipt.DisplayReceipt;
import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.core.MessagePack;
import java.util.Collection;

public class DisplayReceiptPostDataProvider extends MessagePackPostDataProvider<Collection<DisplayReceipt>> {

    private static final String TAG = "DisplayReceiptPostDataProvider";

    private final Collection<DisplayReceipt> receipts;

    public DisplayReceiptPostDataProvider(Collection<DisplayReceipt> receipts) {
        this.receipts = receipts;
    }

    @Override
    public Collection<DisplayReceipt> getRawData() {
        return receipts;
    }

    @Override
    byte[] pack() throws Exception {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        packer.packArrayHeader(receipts.size());
        for (DisplayReceipt data : receipts) {
            data.writeTo(packer);
        }
        packer.close();
        return packer.toByteArray();
    }

    @Override
    public boolean isEmpty() {
        return this.receipts.isEmpty();
    }
}
