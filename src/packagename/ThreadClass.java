package packagename;

import annotations.MyComponent;
import annotations.MyScope;
import packagename.proxy.threadscope.ThreadBeanInterface;

@MyComponent
@MyScope("thread")
public class ThreadClass implements ThreadBeanInterface {
    private final String threadId;

    public ThreadClass(String threadId) {
        this.threadId = threadId;
    }

    public String getThreadId() {
        return threadId;
    }
}
