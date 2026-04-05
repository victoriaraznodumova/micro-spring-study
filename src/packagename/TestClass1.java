package packagename;


import annotations.MyAutowired;
import annotations.MyComponent;
import annotations.MyQualifier;
import packagename.packagename2.ITestClass;

@MyComponent
public class TestClass1 {
    public static void main(String[] args) {
        System.out.println("TestClass1 check");
    }

    @MyAutowired
    private Main main;

//    @MyAutowired
////    @MyQualifier(beanId = "testClass3")
//    private ITestClass iTestClass;
}
