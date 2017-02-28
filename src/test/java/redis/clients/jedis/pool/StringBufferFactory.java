package redis.clients.jedis.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Created by hongda.liang on 2017/2/16.
 */
public class StringBufferFactory extends BasePooledObjectFactory<StringBuffer>{
    @Override
    public StringBuffer create() throws Exception {
        return new StringBuffer();
    }

    @Override
    public PooledObject<StringBuffer> wrap(StringBuffer stringBuffer) {
        return new DefaultPooledObject<>(stringBuffer);
    }

    @Override
    public void passivateObject(PooledObject<StringBuffer> p) throws Exception {
        p.getObject().setLength(0);
    }
}
