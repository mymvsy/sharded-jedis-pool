package redis.clients.jedis;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import redis.clients.jedis.exceptions.JedisConnectionException;

public class ShardedJedisSentinelPoolSpringTest extends TestCase {

	public void testX() throws Exception {

		ApplicationContext ac = new ClassPathXmlApplicationContext("redis.xml");
		ShardedJedisSentinelPool pool = (ShardedJedisSentinelPool) ac.getBean("shardedJedisPool");

		ShardedJedis j = null;
		try {
			j = pool.getResource();

			j.set("test_key_value_not_equal_key", "test_key_value_not_equal_value");

			System.out.println(j.get("test_key_value_not_equal_key"));
		}catch (Exception ep) {
		    ep.printStackTrace();
		}
		pool.destroy();
  	}
}