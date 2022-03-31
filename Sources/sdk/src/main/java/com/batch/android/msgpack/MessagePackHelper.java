package com.batch.android.msgpack;

import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.value.Value;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class MessagePackHelper {

    @SuppressWarnings("unchecked")
    public static void packObject(MessageBufferPacker packer, Object object) throws Exception {
        if (object == null) {
            packer.packNil();
        } else if (object instanceof Byte) {
            packer.packByte((Byte) object);
        } else if (object instanceof Short) {
            packer.packShort((Short) object);
        } else if (object instanceof Integer) {
            packer.packInt((Integer) object);
        } else if (object instanceof Long) {
            packer.packLong((Long) object);
        } else if (object instanceof BigInteger) {
            packer.packBigInteger((BigInteger) object);
        } else if (object instanceof Float) {
            packer.packFloat((Float) object);
        } else if (object instanceof Double) {
            packer.packDouble((Double) object);
        } else if (object instanceof Boolean) {
            packer.packBoolean((Boolean) object);
        } else if (object instanceof String) {
            packer.packString((String) object);
        } else if (object instanceof URI) {
            packer.packString(object.toString());
        } else if (object instanceof Value) {
            ((Value) object).writeTo(packer);
        } else if (object instanceof Map) {
            packMap(packer, (Map) object);
        } else if (object instanceof List) {
            packList(packer, (List) object);
        } else {
            throw new Exception("Object type cannot be serialize");
        }
    }

    public static void packMap(MessageBufferPacker packer, Map<Object, Object> map) throws Exception {
        packer.packMapHeader(map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            packObject(packer, entry.getKey());
            packObject(packer, entry.getValue());
        }
    }

    public static void packList(MessageBufferPacker packer, List<Object> list) throws Exception {
        packer.packArrayHeader(list.size());
        for (Object object : list) {
            packObject(packer, object);
        }
    }
}
