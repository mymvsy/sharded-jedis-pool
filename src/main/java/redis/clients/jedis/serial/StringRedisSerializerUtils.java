package redis.clients.jedis.serial;

import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 对Spring提供的StringRedisSerializer封装
 * Created by hongda.liang on 2017/2/8.
 */
public class StringRedisSerializerUtils  {

//    @Resource(name = "stringRedisSerializer")
    private static StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    public static String deserialize(byte[] bytes) {
        return stringRedisSerializer.deserialize(bytes);
    }

    public static byte[] serialize(String string) {
        return stringRedisSerializer.serialize(string);
    }

}
