import annotations.MyScope;

import java.util.Locale;

public class BeanDefinition {
    private Class<?> beanClass;
    private Object object;
    private Enum scope;

    public BeanDefinition(Class<?> beanClass, Object object, Enum scope) {
        this.beanClass = beanClass;
        this.object = object;
        this.scope = scope;
    }

    public BeanDefinition(Class<?> beanClass, Object object) {
        this.beanClass = beanClass;
        this.object = object;
        this.scope = getScopeFromAnnotation();
    }

    public Object getObject() {
        return object;
    }

//    public Object getSingletonOrPrototypeBean() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
//        if (scope == Scope.SINGLETON){
//            return object;
//       }
//        else{
//            return createNewInstance();
//        }
//    }
//    public Object createNewInstance() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        return beanClass.getDeclaredConstructor().newInstance();
//    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    private Scope getScopeFromAnnotation(){
        MyScope scope = beanClass.getDeclaredAnnotation(MyScope.class);
        if (scope != null){
            return Scope.valueOf(scope.value().toUpperCase(Locale.ROOT));
        }
        else{
            return Scope.SINGLETON;
        }
    }

    @Override
    public String toString() {
        return  "тип бина: " + beanClass.getSimpleName() +
                ", ссылка на бин: " + object;
    }
    public enum Scope{
        SINGLETON,
        PROTOTYPE
    }

    public boolean isBeanSingleton(){
        return scope == Scope.SINGLETON;
    }
    public boolean isBeanPrototype(){
        return scope == Scope.PROTOTYPE;
    }

    public Enum getScope() {
        return scope;
    }
}
