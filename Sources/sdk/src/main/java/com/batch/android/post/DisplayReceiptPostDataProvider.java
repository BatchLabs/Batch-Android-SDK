package com.batch.android.post;

import com.batch.android.core.Logger;
import com.batch.android.displayreceipt.DisplayReceipt;
import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.core.MessagePack;
import java.util.Collection;

public class DisplayReceiptPostDataProvider
  implements PostDataProvider<Collection<DisplayReceipt>> {

  private static final String TAG = "DisplayReceiptPostDataProvider";
  private Collection<DisplayReceipt> receipts;

  public DisplayReceiptPostDataProvider(Collection<DisplayReceipt> receipts) {
    this.receipts = receipts;
  }

  @Override
  public Collection<DisplayReceipt> getRawData() {
    return receipts;
  }

  private byte[] pack() throws Exception {
    MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

    packer.packArrayHeader(receipts.size());
    for (DisplayReceipt data : receipts) {
      data.writeTo(packer);
    }
    packer.close();
    return packer.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (receipts == null || receipts.size() == 0) {
      return new byte[0];
    }

    try {
      return pack();
    } catch (Exception e) {
      Logger.internal(TAG, "Could not pack receipt list", e);
      return new byte[0];
    }
  }

  public boolean isEmpty() {
    return this.receipts.isEmpty();
  }

  @Override
  public String getContentType() {
    return "application/msgpack";
  }
}
