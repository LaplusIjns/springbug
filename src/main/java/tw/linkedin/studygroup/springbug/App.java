package tw.linkedin.studygroup.springbug;

import tw.linkedin.studygroup.springbug.util.BugMap;
import tw.linkedin.studygroup.springbug.util.FixedMap;

/**
 * 原始 bug
 * 基於 SpringFramework 4.3.5 之前 LinkedCaseInsensitiveMap
 * 使用 putIfAbsent 會無法刪除
 *
 * 原因
 * putIfAbsent 由不同 class 實現
 * 4.0.0 由 HashMap 實現會導致bug
 * 4.3.6 由 Map 預設方法實現不會導致bug
 * 
 * 註: 當前最新 6.0 版本 覆寫所有 Map 定義方法  
 * https://github.com/spring-projects/spring-framework/blob/6.0.x/spring-core/src/main/java/org/springframework/util/LinkedCaseInsensitiveMap.java
 * 多數轉到到 targetMap 參數 由 targetMap 實現
 * 如 @Override
 *    public void forEach(BiConsumer<? super String, ? super V> action) {
 *        this.targetMap.forEach(action);
 *    }
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println("== BugMap ==");
        /**
         * LinkedCaseInsensitiveMap 4.0.0 版本
         * https://github.com/spring-projects/spring-framework/blob/4.0.x/spring-core/src/main/java/org/springframework/util/LinkedCaseInsensitiveMap.java
         * 以下部分地方簡稱 LCIM
         */
        BugMap<Object> bm = new BugMap<Object>();
        /**
         * putIfAbsent 由 LinkedCaseInsensitiveMap extends LinkedHashMap
         * LinkedHashMap extends HashMap 由 HashMap 實際實現 putIfAbsent 方法
         * 因此直接操作底層 Node 並未經過 LinkedCaseInsensitiveMap 中間層 caseInsensitiveKeys 參數判斷
         */
        System.out.println("成功放入得到null: " + bm.putIfAbsent("BUG_EXIST", "HELLO_BUG"));
        /**
         * 使用 get ， remove 方法 由 LCIM 改寫
         * LCIM 會先查看 內部Map caseInsensitiveKeys 
         * ( 
         *  caseInsensitiveKeys 為中間層 實現忽略大小寫 
         *  當 LCIM.put K,V  caseInsensitiveKeys 會保存一組K,V key 為全轉小寫的K value為原始K 
         *  例如 LCIM.put("ABC",123)
         *  caseInsensitiveKeys 會保存一組 (K,V) ("abc","ABC")
         * )
         * 
         * 如果 caseInsensitiveKeys 沒有值
         * 會當作不存在 跳過操作 直接回傳 null
         * 然而這是錯誤的 實際值是存在的 沒被正確刪除 也沒正確 get
         */
        System.out.println("get K,V 得到null(正確會得到放入的value HELLO_BUG): " + bm.get("BUG_EXIST"));
        System.out.println("remove K,V 得到null(正確移除會得到放入的value HELLO_BUG): " + bm.remove("BUG_EXIST"));
        /**
         * 使用 forEeach 跟 size 
         * 可確認值未被正確刪除
         * (此兩方法未被 LCIM 改寫 故能讀到值)
         */
        System.out.println("當前 BugMap 含有K,V數量: : " + bm.size());
        System.out.println("forEach 迴圈");
        bm.forEach((K,V)->{
          System.out.println("Key: "+K+" Value: "+V);
        });
        
        System.out.println("");
        System.out.println("== FixedMap ==");
        /**
         * LinkedCaseInsensitiveMap 4.3.6 版本
         * https://github.com/spring-projects/spring-framework/blob/4.3.x/spring-core/src/main/java/org/springframework/util/LinkedCaseInsensitiveMap.java
         * 不再繼承 LinkedHashMap 而是實作 Map 
         * 並且內部有LinkedHashMap targetMap 存K,V 實現原本 4.3.5 繼承的 LinkedHashMap
         * putIfAbsent 由 Map 預作方法實現
         * 
         * default V putIfAbsent(K key, V value) {
         *     V v = get(key);
         *     if (v == null) {
         *     v = put(key, value);
         *     }
         *
         *    return v;
         *   }
         *   
         *   由於是透過 get put 操作
         *   又由於 get put 是 LCIM 實現
         *   故中間層 caseInsensitiveKeys 有正確保存 LCIM 的 key
         *   能正確 get remove
         */
        FixedMap fm = new FixedMap<Object>();
        System.out.println("成功放入得到null: " + fm.putIfAbsent("BUG_EXIST", "HELLO_BUG"));
        System.out.println("get K,V 得到HELLO_BUG (正確會得到放入的value HELLO_BUG): " + fm.get("BUG_EXIST"));
        System.out.println("remove K,V 得到HELLO_BUG (正確移除會得到放入的value HELLO_BUG): " + fm.remove("BUG_EXIST"));
        System.out.println("當前 FixedMap 含有K,V數量: : " + fm.size());
        System.out.println("forEach 迴圈 (無值底下無印出)");
        fm.forEach((K,V)->{
          System.out.println("Key: "+K+" Value: "+V);
        });
        
    }
}
