package packagename;


import annotations.MyAutowired;
import annotations.MyComponent;

@MyComponent
public class TestClass1 {
    public static void main(String[] args) {
        System.out.println("TestClass1 check");
    }

    @MyAutowired
    private Main main;
}
