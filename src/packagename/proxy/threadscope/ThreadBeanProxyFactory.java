package packagename.proxy.threadscope;

import packagename.ThreadClass;
import java.lang.reflect.Proxy;

public class ThreadBeanProxyFactory {
    public static ThreadBeanInterface createThreadProxy(String beanId){
        return (ThreadBeanInterface) Proxy.newProxyInstance(
                ThreadBeanInterface.class.getClassLoader(),
                new Class[]{ThreadBeanInterface.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return "beanId = " + beanId;
                    }
                    ThreadClass realThreadBean = (ThreadClass) ThreadContext.get(beanId);
                    if (realThreadBean == null){
                        realThreadBean = new ThreadClass(Thread.currentThread().getName());
                        ThreadContext.set(beanId, realThreadBean);
                    }
                    return method.invoke(realThreadBean, args);
                }
        );
    }
}
