public class Bean {
    private Object beanObject;

    public Object getBeanObject() {
        return beanObject;
    }
    public void setBeanObject(Object beanObject) {
        this.beanObject = beanObject;
    }
    public Bean(Object beanObject) {
        System.out.println("Бин успешно создан");
        this.beanObject = beanObject;
    }
    public void test(){
        System.out.println("test is successful");
    }
}
