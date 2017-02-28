package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Hashing;

public class ShardedJedisSentinelPoolTest extends TestCase {

    public void testX() throws Exception {

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();

		/* redis master ip地址 */
        List<String> masters = new ArrayList<String>();
        masters.add("shard1");
        masters.add("shard2");

		/* 所有分片的实例的ip:port配置 */
        Set<String> sentinels = new HashSet<String>();
        sentinels.add("192.168.109.212:26379");
        sentinels.add("192.168.109.215:26379");

        ShardedJedisSentinelPool pool = new ShardedJedisSentinelPool(masters, sentinels, config, 60000);

        /** 1.简单示例 **/
        ShardedJedis jedis = pool.getResource();
        try {
            jedis = pool.getResource();
            // do somethind...
            // ...
        } finally {
            if (jedis != null) pool.returnResource(jedis);
            pool.destroy();
        }

        /** 2.示例开始 **/
        ShardedJedis j = null;
        for (int i = 0; i < 100; i++) {
            try {
                j = pool.getResource();
                j.set("KEY: " + i, "" + i);
                System.out.print(i);
                System.out.print(" ");
                Thread.sleep(500);
                pool.returnResource(j);
            } catch (JedisConnectionException e) {
                System.out.print("x");
                i--;
                Thread.sleep(1000);
            }
        }

        // 换行
        System.out.println("");

        for (int i = 0; i < 100; i++) {
            try {
                j = pool.getResource();
                assertEquals(j.get("KEY: " + i), "" + i);
                System.out.print(".");
                Thread.sleep(500);
                pool.returnResource(j);
            } catch (JedisConnectionException e) {
                System.out.print("x");
                i--;
                Thread.sleep(1000);
            }
        }
        /** 2.示例结束 **/
        pool.destroy();
    }

    public void testY() throws Exception {
        JedisShardInfo s1 = new JedisShardInfo("10.165.126.81", 6379, 15000);
        s1.setPassword("BeAv4GZBoI4G");
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(s1);

        ShardedJedis shardedJedis = new ShardedJedis(shards, Hashing.MURMUR_HASH);


        ShardedRedisTemplate shardedRedisTemplate = new ShardedRedisTemplate();

        shardedJedis.set("kkk", "value");


        System.out.println(shardedRedisTemplate.get("kkk"));
    }

    public void testC() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(200);
        config.setMaxTotal(400);
        config.setMinEvictableIdleTimeMillis(300000);
        config.setNumTestsPerEvictionRun(3);
        config.setTimeBetweenEvictionRunsMillis(60000);
        config.setMaxWaitMillis(5000);

		/* redis master ip地址 */
        List<String> masters = new ArrayList<String>();
        masters.add("test6009");

		/* 所有分片的实例的ip:port配置 */
        Set<String> sentinels = new HashSet<String>();
        sentinels.add("10.165.126.195:26009");
        ShardedJedisSentinelPool pool = new ShardedJedisSentinelPool(masters, sentinels, config, "abcd1234");


        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = pool.getResource();

            shardedJedis.set("kkkk", "value");
            System.out.println(shardedJedis.get("kkk"));


        } catch (Exception ep) {
            ep.printStackTrace();
        }

    }

    public void testD() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(200);
        config.setMaxTotal(400);
        config.setMinEvictableIdleTimeMillis(300000);
        config.setNumTestsPerEvictionRun(3);
        config.setTimeBetweenEvictionRunsMillis(60000);
        config.setMaxWaitMillis(5000);

		/* redis master ip地址 */
        StringBuffer m = new StringBuffer();
        m.append("test6009");

		/* 所有分片的实例的ip:port配置 */
        StringBuffer sentinel = new StringBuffer();
        sentinel.append("10.165.126.195:26009");

        /* key-value对不对等 */
        SupportKeyValueNotEqualObject supportKeyValueNotEqualObject = new SupportKeyValueNotEqualObject(m.toString(), sentinel.toString());
        ShardedJedisSentinelPool pool = new ShardedJedisSentinelPool(supportKeyValueNotEqualObject, config, "abcd1234");


        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = pool.getResource();

            shardedJedis.set("testD", "testD");
            System.out.println(shardedJedis.get("testD"));


        } catch (Exception ep) {
            ep.printStackTrace();
        }
    }


}
