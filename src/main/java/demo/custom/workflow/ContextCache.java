package demo.custom.workflow;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yangjie
 * @date Created in 2020/2/2 22:02
 * @description 上下文传参
 */
public class ContextCache implements Serializable {

    /**
     * 缓存上下文信息
     */
    private static final ThreadLocal<Map<String, String>> CACHE = new InheritableThreadLocal<>();

    /**
     * 放数据
     * @param sourceKey
     */
    public static void putAttribute(String sourceKey, String value) {
        Map<String,String> cacheMap = CACHE.get();
        if(null == cacheMap){
            cacheMap = Maps.newHashMap();
        }
        cacheMap.put(sourceKey, value);
        CACHE.set(cacheMap);
    }

    /**
     * 拿数据
     * @param sourceKey
     */
    public static String getAttribute(String sourceKey) {
        Map<String,String> cacheMap = CACHE.get();
        if(null == cacheMap){
            return null;
        }
        return cacheMap.get(sourceKey);
    }

    /**
     * 得到 Map
     * @return
     */
    public static Map<String,String> getMap(){
        return CACHE.get();
    }

    /**
     * 设置数据
     * @param map
     */
    public static void putAllAttribute(Map<String,String> map){
        Map<String,String> cacheMap = CACHE.get();
        if(null == cacheMap){
            cacheMap = Maps.newHashMap();
        }
        cacheMap.putAll(map);
        CACHE.set(cacheMap);
    }

    /**
     * 清空ThreadLocal的数据
     */
    public static void clean() {
        CACHE.remove();
    }
}
