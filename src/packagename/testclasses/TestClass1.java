package packagename.testclasses;


import annotations.MyAutowired;
import annotations.MyComponent;
import annotations.MyQualifier;
import annotations.MyScope;

@MyComponent
@MyScope("prototype")
public class TestClass1 {
    public TestClass1() {
    }

    public static void main(String[] args) {
        System.out.println("TestClass1 check");
    }

    @MyAutowired
    private Main main;

    @MyAutowired
    @MyQualifier(beanId = "packagename.testclasses.TestClass3")
    private ITestClass iTestClass;
}
