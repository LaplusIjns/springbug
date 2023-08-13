package tw.linkedin.studygroup.springbug;

import tw.linkedin.studygroup.springbug.util.LinkedCaseInsensitiveMapVersion4_0_0;
import tw.linkedin.studygroup.springbug.util.LinkedCaseInsensitiveMapVersion4_3_6;
import java.util.Map;

/**
 * 原始 bug
 * 基於 SpringFramework 4.3.5 之前 LinkedCaseInsensitiveMap
 * 使用 putIfAbsent 後 remove方法失效
 *
 * 原因
 * putIfAbsent 由不同 class 實現
 * 4.0.0 由 HashMap 實現會導致bug
 * 4.3.6 由 Map 預設方法實現不會導致bug
 */
public class App
{
    public static void main( String[] args )
    {
        /**
         * LinkedCaseInsensitiveMap 4.0.0 版本
         * 1. LinkedCaseInsensitiveMap(LCIM) 裡有個caseInsensitiveKeys的Map中間層，
         * 當LCIM.put("ABC", 123)時，caseInsensitiveKeys 會保存一組 (K,V) ("abc","ABC") <==== K為小寫後的"ABC"，V為原始"ABC"，
         * 接著透過這個中間層實現忽略大小寫的Map操作，比如:
         *     @Override
         *     public V remove(Object key) {
         *         if (key instanceof String ) {
         *             return super.remove(this.caseInsensitiveKeys.remove(convertKey((String) key)));
         *         }
         *         else {
         *             return null;
         *         }
         *     }
         *
         *  2. 由於4.0.0 版本中的putIfAbsent方法是由HashMap實作，並未存到caseInsensitiveKeys裡，
         *  因此在remove caseInsensitiveKeys時它回傳了null，再把null傳進super.remove()裏，自然沒有刪除任何東西
         *  (LinkedCaseInsensitiveMapVersion extends LinkedHashMap, LinkedHashMap extends HashMap)
         */
        System.out.println("== LinkedCaseInsensitiveMap 4.0.0 ==");
        LinkedCaseInsensitiveMapVersion4_0_0<Object> lcim4_0_0 = new LinkedCaseInsensitiveMapVersion4_0_0<>();
        seeKeyValueAfterRemovingValueFromPutIfAbsent((lcim4_0_0));

        /**
         * LinkedCaseInsensitiveMap 4.3.6 版本
         * 不再繼承LinkedHashMap 而是實作 Map，因此使用Map.putIfAbsent預設方法時，
         * 會去調用LinkedCaseInsensitiveMap複寫後的get put方法，從而將值放入caseInsensitiveKeys裡
         * (LinkedCaseInsensitiveMapVersion implements Map，內部另有LinkedHashMap targetMap 存K,V 實現原本 4.3.5 繼承的 LinkedHashMap)
         *
         * default V putIfAbsent(K key, V value) {
         *     V v = get(key);
         *     if (v == null) {
         *          v = put(key, value);
         *     }
         *    return v;
         * }
         *
         */
        System.out.println("\n== LinkedCaseInsensitiveMap 4.3.6 ==");
        LinkedCaseInsensitiveMapVersion4_3_6<Object> lcim4_3_6 = new LinkedCaseInsensitiveMapVersion4_3_6<>();
        seeKeyValueAfterRemovingValueFromPutIfAbsent(lcim4_3_6);

        /**
         * 當前最新 6.0 版本 覆寫所有 Map 定義方法
         * <a href="https://github.com/spring-projects/spring-framework/blob/6.0.x/spring-core/src/main/java/org/springframework/util/LinkedCaseInsensitiveMap.java">...</a>
         * 多數方法轉到 targetMap 裏實現
         * 如 @Override
         *    public void forEach(BiConsumer<? super String, ? super V> action) {
         *        this.targetMap.forEach(action);
         *    }
         */
    }

    private static void seeKeyValueAfterRemovingValueFromPutIfAbsent(Map map) {
        System.out.println("map.putIfAbsent(\"KEY\", \"VALUE\")"); map.putIfAbsent("KEY", "VALUE");
        System.out.println("map.get(\"KEY\"): " + map.get("KEY"));
        System.out.println("map.remove(\"KEY\") then returns: " + map.remove("KEY"));
        System.out.println("map.size(): " + map.size());
        StringBuilder mapForEach = new StringBuilder();
        map.forEach((K,V)-> mapForEach.append("Key: " + K + " Value: " + V));
        System.out.println("map.forEach(K,V)=> " + mapForEach);
    }
}
