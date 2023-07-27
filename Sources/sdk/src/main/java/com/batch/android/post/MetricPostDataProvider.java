package com.batch.android.post;

import com.batch.android.metrics.model.Metric;
import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.core.MessagePack;
import java.io.IOException;
import java.util.Collection;

public class MetricPostDataProvider extends MessagePackPostDataProvider<Collection<Metric<?>>> {

    private final Collection<Metric<?>> metrics;

    public MetricPostDataProvider(Collection<Metric<?>> metrics) {
        this.metrics = metrics;
    }

    @Override
    public Collection<Metric<?>> getRawData() {
        return metrics;
    }

    @Override
    byte[] pack() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packArrayHeader(metrics.size());
        try {
            for (Metric<?> data : metrics) {
                data.pack(packer);
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            packer.close();
        }
        return packer.toByteArray();
    }

    @Override
    public boolean isEmpty() {
        return this.metrics.isEmpty();
    }
}
