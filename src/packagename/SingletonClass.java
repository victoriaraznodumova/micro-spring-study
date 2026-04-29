package packagename;

import annotations.MyAutowired;
import annotations.MyComponent;
import annotations.MyScope;
import packagename.proxy.threadscope.ThreadBeanInterface;

@MyComponent
@MyScope("singleton")
public class SingletonClass {
    @MyAutowired
    private final ThreadBeanInterface threadBean;

    public SingletonClass(ThreadBeanInterface threadBean) {
        this.threadBean = threadBean;
    }
    public void printThreadBean(){
        System.out.println("Thread бин " + threadBean + " с threadId = " + threadBean.getThreadId());
    }
}
