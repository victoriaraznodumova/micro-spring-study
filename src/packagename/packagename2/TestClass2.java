package packagename.packagename2;

import annotations.MyAutowired;
import annotations.MyComponent;
import packagename.TestClass1;

@MyComponent
public class TestClass2 {
    @MyAutowired
    TestClass1 testClass1;
}
