public class BeanDefinition {
    private Class<?> beanClass;
    private Object object;

    public BeanDefinition(Class<?> beanClass, Object object) {
        this.beanClass = beanClass;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public String toString() {
        return  "тип бина: " + beanClass.getSimpleName() +
                ", ссылка на бин: " + object;
    }
}
