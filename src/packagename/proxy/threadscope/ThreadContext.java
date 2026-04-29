package packagename.proxy.threadscope;

import java.util.HashMap;
import java.util.Map;

public class ThreadContext {
//    private static final ThreadLocal<ThreadClass> currentThread = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Object>> currentThread = ThreadLocal.withInitial(HashMap::new);

    public static void set(String beanId, Object threadBean){
        currentThread.get().put(beanId, threadBean);
    }
    public static Object get(String beanId){
        return currentThread.get().get(beanId);
    }
    public static void clear(){
        currentThread.remove();
    }
}
