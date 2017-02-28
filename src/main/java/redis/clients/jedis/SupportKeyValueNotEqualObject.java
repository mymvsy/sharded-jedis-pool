package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 支持 Test Profile和Online Profile中的key-value对数量不相等的场景
 * 多个master name和sentinel ip使用 | 分隔
 * Created by hongda.liang on 2017/2/28.
 */
public class SupportKeyValueNotEqualObject {

    private static String VERTICAL_SEPARATOR = "\\|";

    private List<String> masters;           // master名称
    private Set<String> sentinels;          // 哨兵ip:port

    public SupportKeyValueNotEqualObject(String master, String sentinel) {
        masters = new ArrayList<>();
        sentinels = new HashSet<>();
        String[] masterArray = master.split(VERTICAL_SEPARATOR);
        String[] sentinelArray = sentinel.split(VERTICAL_SEPARATOR);

        for (String handledMaster : masterArray) {
            masters.add(handledMaster);
        }

        for (String handledSentinel : sentinelArray) {
            sentinels.add(handledSentinel);
        }
    }

    public List<String> getMasters() {
        return masters;
    }

    public void setMasters(List<String> masters) {
        this.masters = masters;
    }

    public Set<String> getSentinels() {
        return sentinels;
    }

    public void setSentinels(Set<String> sentinels) {
        this.sentinels = sentinels;
    }
}
