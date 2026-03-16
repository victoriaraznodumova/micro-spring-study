package packagename;

import annotations.MyComponent;

@MyComponent
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
    public Main(){
        System.out.println("Объект создан успешно");
    }
}