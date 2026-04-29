package packagename.testclasses;

import annotations.MyAutowired;
import annotations.MyComponent;

@MyComponent
public class TestClass2 implements ITestClass {
    @MyAutowired
    TestClass1 testClass1;
}
