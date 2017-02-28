package redis.clients.jedis;

import org.springframework.stereotype.Component;
import redis.clients.util.Hashing;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 帮助使简化Redis数据访问
 * <p/>
 * 在给定对象和基础二进制直接做自动序列化转换，key和value的序列化分别采用Spring StringSerializer和Protostuff
 * <p/>
 * <p/>
 * Created by hongda.liang on 2017/2/9.
 */
@Component("shardedRedisTemplate")
public class ShardedRedisTemplate<K extends String, V> {






    /**
     * 执行具体的redis操作
     *
     * @param callback 回调执行类
     * @return 执行后返回的值
     */
    private Object execute(RedisCallBack callback) {
        JedisShardInfo s1 = new JedisShardInfo("10.165.126.81", 6379, 15000);
        s1.setPassword("BeAv4GZBoI4G");
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(s1);

        ShardedJedis shardedJedis = new ShardedJedis(shards, Hashing.MURMUR_HASH);

        try {
            return callback.doWithRedis(shardedJedis);
        } finally {

        }
    }

    public Object get(final String key) {

        return (Object) execute(new RedisCallBack() {
            @Override
            public Object doWithRedis(ShardedJedis shardedJedis) {
                return shardedJedis.get(key);
            }
        });
    }






}
