package redis.clients.jedis.serial; /**
 * Copyright 2014-2015, NetEase, Inc. All Rights Reserved.
 *
 * Date: Nov 16, 2015
 */


import io.protostuff.*;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protostuff序列化的工具类
 *
 * @author shenjiang<hzshenjiang@corp.netease.com>
 * @since Nov 16, 2015
 */
public final class ProtoStuffUtils {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();


    private static Schema<WrapObject> wrapSchema;

    public static final Delegate<Timestamp> TIMESTAMP_DELEGATE = new Delegate<Timestamp>()
    {

        public WireFormat.FieldType getFieldType()
        {
            return WireFormat.FieldType.FIXED64;
        }

        public Class<?> typeClass()
        {
            return Timestamp.class;
        }

        public Timestamp readFrom(Input input) throws IOException
        {
            return new Timestamp(input.readFixed64());
        }

        public void writeTo(Output output, int number, Timestamp value, boolean repeated)
                throws IOException
        {
            output.writeFixed64(number, value.getTime(), repeated);
        }

        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated)
                throws IOException
        {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

    };

    private ProtoStuffUtils() {
    }

    static {
        DefaultIdStrategy dis = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
    }

    /**
     * 序列化（对象 -> 字节数组）
     *
     * @param obj
     * @return
     * @author shenjiang<hzshenjiang@corp.netease.com>
     * @since Nov 16, 2015
     */
    public static <T> byte[] serialize(T obj) {

        if(obj == null){
            return null;
        }

        WrapObject wrapObject = new WrapObject();
        wrapObject.setObject(obj);
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<WrapObject> schema = getSchema();
            return ProtostuffIOUtil.toByteArray(wrapObject, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（字节数组 -> 对象）
     *
     * @param data
     * @return
     * @author shenjiang<hzshenjiang@corp.netease.com>
     * @since Nov 16, 2015
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data) {


        if(data == null){
            return null;
        }


        WrapObject wrap = new WrapObject();
        Schema<WrapObject> schema = getSchema();
        ProtostuffIOUtil.mergeFrom(data, wrap, schema);
        return (T)wrap.getObject();
    }


    /**
     * 序列化（对象 -> 输出流）
     *
     * @param obj
     * @param outputStream
     * @throws java.io.IOException
     * @author shenjiang<hzshenjiang@corp.netease.com>
     * @since Dec 22, 2015
     */
    public static <T> void serialize(T obj, OutputStream outputStream) throws IOException {


        if(obj == null){
            return;
        }


        WrapObject wrapObject = new WrapObject();
        wrapObject.setObject(obj);
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<WrapObject> schema = getSchema();
            ProtostuffIOUtil.writeTo(outputStream, wrapObject, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（输入流  -> 对象）
     *
     * @param inputStream
     * @return
     * @throws java.io.IOException
     * @author shenjiang<hzshenjiang@corp.netease.com>
     * @since Dec 22, 2015
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(InputStream inputStream) throws IOException {

        WrapObject wrap = new WrapObject();
        Schema<WrapObject> schema = getSchema();
        ProtostuffIOUtil.mergeFrom(inputStream, wrap, schema);
        return (T) wrap.getObject();
    }

    private static Schema<WrapObject> getSchema() {

        if (wrapSchema == null) {
            wrapSchema = RuntimeSchema.createFrom(WrapObject.class);
        }
        return wrapSchema;
    }

    /**
     * 为了支持直接序列化List和Map对象，使用这个类将需要序列化的对象包装下
     * 这样就可以直接使用Protostuff中的ObjectSchema，而不用自己实现CollectionSchema和MapSchema
     *
     * @author shenjiang<hzshenjiang@corp.netease.com>
     * @since Dec 10, 2015
     */
    public static class WrapObject implements Serializable {

        private static final long serialVersionUID = 462267161432782660L;

        private Object object;

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

    }


}
