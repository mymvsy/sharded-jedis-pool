package redis.clients.jedis.pool;


import org.apache.commons.pool2.ObjectPool;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by hongda.liang on 2017/2/16.
 */
public class ReaderUtil {

    private ObjectPool<StringBuffer> pool;

    public ReaderUtil() {
    }

    public String readToString(Reader in) throws IOException {
        StringBuffer buf = null;

        try {
            buf = pool.borrowObject();
            for (int c = in.read(); c != -1; c = in.read()) {
                buf.append((char) c);
            }
        } catch (Exception ep) {
            ep.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception ep) {
                ep.printStackTrace();
            }
        }

        return buf.toString();
    }
}
