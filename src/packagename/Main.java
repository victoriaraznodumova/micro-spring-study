package packagename;

import annotations.MyAutowired;
import annotations.MyComponent;
import annotations.MyScope;
import packagename.packagename2.TestClass2;
import packagename.packagename2.TestClass3;


@MyComponent
@MyScope()
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
    public Main(){
    }



    @MyAutowired
    TestClass2 testClass2;

    @MyAutowired
    TestClass3 testClass3;
}