
import packagename.ThreadClass;
import packagename.SingletonClass;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class ApplicationRunner {
    public static void main(String[] args) throws URISyntaxException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {
//        ApplicationContext applicationContext = new ApplicationContext("");
        ApplicationContext applicationContext = new ApplicationContext("packagename");
//        System.out.println("проверим контекст:");
//        applicationContext.getBeansMap().forEach((k, v) -> System.out.println("имя бина " + k + " является ли бин синглтоном: " + v.isBeanSingleton() + ", экземпляр бина из контекста: " + v.getObject()));
        getBeansFromContext(applicationContext);
        System.out.println();

        System.out.println("Запросили у контейнера singleton bean packagename.SingletonClass: ");
        SingletonClass singleton = (SingletonClass) applicationContext
                .getBeanDefinition(Class.forName("packagename.SingletonClass"))
                .getObject();
        Runnable task = singleton::printThreadBean;
        System.out.println();
        new Thread(task).start();
        new Thread(task).start();
    }

    public static void getBeansFromContext(ApplicationContext applicationContext) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        System.out.println("\nЗапросили у контейнера singleton bean packagename.testclasses.Main: ");
        BeanDefinition singletonBean1 = applicationContext.getBeanDefinition(Class.forName("packagename.testclasses.Main"));
        System.out.println("Полученный бин: " + singletonBean1.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.testclasses.TestClass3: ");
        BeanDefinition prototypeBean1 = applicationContext.getBeanDefinition(Class.forName("packagename.testclasses.TestClass3"));
        System.out.println("Полученный бин: " + prototypeBean1.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.testclasses.TestClass3: ");
        BeanDefinition prototypeBean2 = applicationContext.getBeanDefinition(Class.forName("packagename.testclasses.TestClass3"));
        System.out.println("Полученный бин: " + prototypeBean2.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.testclasses.TestClass1: ");
        BeanDefinition prototypeBean3 = applicationContext.getBeanDefinition(Class.forName("packagename.testclasses.TestClass1"));
        System.out.println("Полученный бин: " + prototypeBean3.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.testclasses.TestClass1: ");
        BeanDefinition prototypeBean4 = applicationContext.getBeanDefinition(Class.forName("packagename.testclasses.TestClass1"));
//        Object bean = applicationContext.getBean(Class.forName("packagename.testclasses.TestClass1"));
        System.out.println("Полученный бин: " + prototypeBean4.getObject());

        System.out.println("\nЗапросили у контейнера singleton bean packagename.testclasses.TestClass2: ");
        BeanDefinition singletonBean2 = applicationContext.getBeanDefinition(Class.forName("packagename.testclasses.TestClass2"));
        System.out.println("Полученный бин: " + singletonBean2.getObject());

//        System.out.println("\nЗапросили у контейнера singleton bean packagename.packagename2.ITestClass: ");
//        BeanDefinition singletonBean3 = applicationContext.getBeanDefinition(Class.forName("packagename.packagename2.ITestClass"));
//        System.out.println("Полученный бин: " + singletonBean3.getObject());

        System.out.println("\nЗапросили у контейнера thread bean packagename.ThreadClass: ");
        BeanDefinition threadBean1 = applicationContext.getBeanDefinition(Class.forName("packagename.ThreadClass"));
        System.out.println("Полученный бин: " + threadBean1.getObject());
        Object threadBean2 = applicationContext.getBean(ThreadClass.class);
        System.out.println(threadBean2);
    }
}
