package com.batch.android.displayreceipt;

import com.batch.android.core.Logger;
import com.batch.android.msgpack.MessagePackHelper;
import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.core.MessagePack;
import com.batch.android.msgpack.core.MessageUnpacker;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DisplayReceipt {

    private static final String TAG = "DisplayReceipt";

    private long timestamp; // in seconds !
    private boolean replay;
    private int sendAttempt;
    private Map<String, Object> od;
    private Map<String, Object> ed;

    private DisplayReceipt(
        long timestamp,
        boolean replay,
        int sendAttempt,
        Map<String, Object> od,
        Map<String, Object> ed
    ) {
        this.timestamp = timestamp;
        this.replay = replay;
        this.sendAttempt = sendAttempt;
        this.od = od;
        this.ed = ed;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }

    public void incrementSendAttempt() {
        this.sendAttempt += 1;
    }

    public Map<String, Object> getOd() {
        return od;
    }

    public Map<String, Object> getEd() {
        return ed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isReplay() {
        return replay;
    }

    public int getSendAttempt() {
        return sendAttempt;
    }

    public byte[] packAndWrite(File outputFile) {
        byte[] data = pack(timestamp, replay, sendAttempt, od, ed);
        if (data != null && CacheHelper.write(outputFile, data)) {
            return data;
        }
        return null;
    }

    public void writeTo(MessageBufferPacker packer) throws Exception {
        pack(packer, timestamp, replay, sendAttempt, od, ed);
    }

    private static void pack(
        MessageBufferPacker packer,
        long timestamp,
        boolean replay,
        int sendAttempt,
        Map<String, Object> od,
        Map<String, Object> ed
    ) throws Exception {
        packer.packLong(timestamp);
        packer.packBoolean(replay);
        packer.packInt(sendAttempt);

        if (od != null && od.size() <= 0) {
            // If map is empty, we pack null
            od = null;
        }
        MessagePackHelper.packObject(packer, od);

        if (ed != null && ed.size() <= 0) {
            // If map is empty, we pack null
            ed = null;
        }
        MessagePackHelper.packObject(packer, ed);
    }

    public static byte[] pack(
        long timestamp,
        boolean replay,
        int sendAttempt,
        Map<String, Object> od,
        Map<String, Object> ed
    ) {
        try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            pack(packer, timestamp, replay, sendAttempt, od, ed);
            packer.flush();
            return packer.toByteArray();
        } catch (Exception e) {
            Logger.internal(TAG, "Could not pack display receipt", e);
        }

        return null;
    }

    public static DisplayReceipt unpack(byte[] data) {
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
            long timestamp = unpacker.unpackLong();
            boolean replay = unpacker.unpackBoolean();
            int sendAttempt = unpacker.unpackInt();

            Map<String, Object> od = null;
            if (!unpacker.tryUnpackNil()) {
                // od is not null
                int odSize = unpacker.unpackMapHeader();
                od = new HashMap<>();
                for (int i = 0; i < odSize; ++i) {
                    String key = unpacker.unpackString();
                    Object value = unpacker.unpackValue();
                    od.put(key, value);
                }
            }

            Map<String, Object> ed = null;
            if (!unpacker.tryUnpackNil()) {
                // ed is not null
                int edSize = unpacker.unpackMapHeader();
                ed = new HashMap<>();
                for (int i = 0; i < edSize; ++i) {
                    String key = unpacker.unpackString();
                    Object value = unpacker.unpackValue();
                    ed.put(key, value);
                }
            }

            return new DisplayReceipt(timestamp, replay, sendAttempt, od, ed);
        } catch (Exception e) {
            Logger.internal(TAG, "Could not unpack display receipt", e);
            return null;
        }
    }
}
