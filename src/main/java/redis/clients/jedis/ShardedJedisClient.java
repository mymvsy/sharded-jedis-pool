package redis.clients.jedis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.serial.ProtoStuffUtils;
import redis.clients.jedis.serial.StringRedisSerializerUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Haoming.Liang on 2019/7/16.
 */
public class ShardedJedisClient<K extends String, V> {


    protected final Logger LOGGER = Logger.getLogger(getClass().getName());

    private ExecutorService COMMAND_MGET_THREAD_POOL = Executors.newCachedThreadPool();             // 异步mget命令执行线程池
    private ExecutorService ASYN_DESERIALIZE_THREAD_POOL = Executors.newCachedThreadPool();

    private static final ShardedJedisSentinelPool shardedJedisPool;

    private ShardedJedis getShardedJedis() {
        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        // 用于shardedJedis.close()方法回还borrow的对象
        shardedJedis.setDataSource(shardedJedisPool);
        return shardedJedis;
    }

    static {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();

        /* redis master ip地址 */
        List<String> masters = new ArrayList<String>();
        masters.add("shard1");
        masters.add("shard2");

		/* 所有分片的实例的ip:port配置 */
        Set<String> sentinels = new HashSet<String>();
        sentinels.add("192.168.109.212:26379");
        sentinels.add("192.168.109.215:26379");

        shardedJedisPool = new ShardedJedisSentinelPool(masters, sentinels, config, 60000);
    }

    /**
     * 执行具体的redis操作
     *
     * @param callback 回调执行类
     * @return 执行后返回的值
     */
    private Object execute(RedisCallBack callback) {
        ShardedJedis shardedJedis = getShardedJedis();
        try {
            return callback.doWithRedis(shardedJedis);
        } finally {
            shardedJedis.close();
        }
    }

    /**
     * 原生ShardedJedis不支持mget指令
     * 开发者自行封装 性能相较于for循环有很大提升
     */
    @SuppressWarnings("unchecked")
    public List<V> multiGet(final Collection<String> keys) {
        LOGGER.info(">>> Collection<String> keys size is " + keys.size());
        if (keys.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        return (List<V>) execute(new RedisCallBack() {
            public Object doWithRedis(ShardedJedis shardedJedis) {
                // clustering and record index
                int count = 0;
                Multimap<String, Integer> indexMap = ArrayListMultimap.create();
                final Multimap<Jedis, String> jedisMultimap = ArrayListMultimap.create();
                for (String key : keys) {
                    indexMap.put(key, count);
                    jedisMultimap.put(shardedJedis.getShard(StringRedisSerializerUtils.serialize(key)), key);
                    count++;
                }

                // concurrency excutive mget
                Set<Jedis> jedisSet = jedisMultimap.keySet();
                Map<Jedis, Future<List<byte[]>>> futureMap = new HashMap<>();
                for (final Jedis jedis : jedisSet) {
                    Future<List<byte[]>> future = COMMAND_MGET_THREAD_POOL.submit(new Callable<List<byte[]>>() {
                        @Override
                        public List<byte[]> call() throws Exception {
                            Collection<String> keyCollection = jedisMultimap.get(jedis);
                            // to byte[][]
                            int index = 0;
                            byte[][] rawKeyArray = new byte[keyCollection.size()][];
                            for (String rawKey : keyCollection) {
                                rawKeyArray[index] = StringRedisSerializerUtils.serialize(rawKey);
                                index++;
                            }

                            return jedis.mget(rawKeyArray);
                        }
                    });

                    futureMap.put(jedis, future);
                }

                return doAsynDeserialize(futureMap, keys.size(), jedisMultimap, indexMap);
            }
        });
    }


    /**
     * 获取入参key在列表中的原始索引index
     * 相同的key下面存储的是索引列表 获取索引列表中的索引后 删除使用过的索引
     *
     * @param indexMap
     * @return
     */
    public int getOriginKeyIndex(Multimap<String, Integer> indexMap, String key, Integer indexCount) {
        int index = 0;
        try {
            int i = 0;
            Collection<Integer> originIndexCol = indexMap.get(key);
            if (!originIndexCol.isEmpty()) {
                for (Integer oIndex : originIndexCol) {
                    if (i == indexCount) {
                        index = oIndex;
                        break;
                    }
                    i++;
                }

            }
        } catch (Exception ep) {
            LOGGER.severe("parse index faild");
            ep.printStackTrace();
        }
        return index;
    }

    public List<V> doAsynDeserialize(final Map<Jedis, Future<List<byte[]>>> futureMap, int keysSize, Multimap<
            Jedis, String> jedisMultimap, Multimap<String, Integer> indexMap) {

        try {
            Map<Jedis, Future<List<V>>> bridgeMap = new HashMap<>();
            // 线程池异步序列化逻辑
            Set<Jedis> futureJedisSet = futureMap.keySet();
            for (final Jedis jedis : futureJedisSet) {
                Future<List<V>> future = ASYN_DESERIALIZE_THREAD_POOL.submit(new Callable<List<V>>() {
                    @Override
                    public List<V> call() throws Exception {
                        List<byte[]> bridgeByteList = futureMap.get(jedis).get();
                        // 序列化
                        List<V> bridgeResult = new ArrayList<V>();
                        for (byte[] rawValue : bridgeByteList) {
                            bridgeResult.add((V) ProtoStuffUtils.deserialize(rawValue));
                        }
                        return bridgeResult;
                    }
                });
                bridgeMap.put(jedis, future);
            }

            // 序列化后的数据按照入库key的顺序进行重排序
            V[] resultBA = (V[]) new Object[keysSize];
            Set<Jedis> bridgeJedistSet = bridgeMap.keySet();
            for (Jedis jedis : bridgeJedistSet) {
                Collection<String> keyCollection = jedisMultimap.get(jedis);
                List<V> singleList = bridgeMap.get(jedis).get();

                int keyIndex = 0;
                Map<String, Integer> indexCount = Maps.newHashMap();
                for (String key : keyCollection) {
                    Integer index = indexCount.get(key);
                    if (index == null) {
                        index = 0;
                    } else {
                        index++;
                    }
                    indexCount.put(key, index);
                    int originKeyIndex = getOriginKeyIndex(indexMap, key, index);
                    resultBA[originKeyIndex] = singleList.get(keyIndex);
                    keyIndex++;
                }
            }

            // 泛型数组迭代为List返回
            List<V> result = new ArrayList<V>();
            for (Object object : resultBA) {
                result.add((V) object);
            }

            return result;
        } catch (Exception ep) {
            LOGGER.log(Level.ALL, "parse futureMap failed {}", ep);
            return new ArrayList<>();
        }
    }
}
