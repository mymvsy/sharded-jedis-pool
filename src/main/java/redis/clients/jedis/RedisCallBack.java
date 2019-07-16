package redis.clients.jedis;

/**
 * 调用Redis底层的代码，被ShardedRedisTemplate执行
 * 通常作为方法实现中的匿名类
 * 通常封装若干个操作顺序执行
 * Created by haoming.liang on 2017/2/8.
 */
public interface RedisCallBack {

    /**
     * 由具有可被使用的Redis链接调用
     * 不需要关心激活或关闭连接或处理异常
     */
    Object doWithRedis(ShardedJedis shardedJedis);
}
