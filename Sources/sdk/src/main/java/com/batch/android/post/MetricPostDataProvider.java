package com.batch.android.post;

import com.batch.android.metrics.model.Metric;
import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.core.MessagePack;
import java.util.Collection;

public class MetricPostDataProvider extends MessagePackPostDataProvider<Collection<Metric<?>>> {

    private static final String TAG = "DisplayReceiptPostDataProvider";

    private final Collection<Metric<?>> metrics;

    public MetricPostDataProvider(Collection<Metric<?>> metrics) {
        this.metrics = metrics;
    }

    @Override
    public Collection<Metric<?>> getRawData() {
        return metrics;
    }

    @Override
    byte[] pack() throws Exception {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packArrayHeader(metrics.size());
        for (Metric<?> data : metrics) {
            data.pack(packer);
        }
        packer.close();
        return packer.toByteArray();
    }

    @Override
    public boolean isEmpty() {
        return this.metrics.isEmpty();
    }
}
