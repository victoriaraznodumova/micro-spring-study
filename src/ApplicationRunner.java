
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class ApplicationRunner {
    public static void main(String[] args) throws URISyntaxException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        ApplicationContext applicationContext = new ApplicationContext("packagename");
//        System.out.println("проверим контекст");
//        applicationContext.getBeansMap().forEach((k, v) -> System.out.println("имя бина " + k + " является ли бин синглтоном " + v.isBeanSingleton()));
        getBeans(applicationContext);
    }

    public static void getBeans(ApplicationContext applicationContext) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        System.out.println("\nЗапросили у контейнера singleton bean packagename.Main: ");
        BeanDefinition singletonBean1 = applicationContext.getBeanDefinition(Class.forName("packagename.Main"));
        System.out.println("Полученный бин: " + singletonBean1.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.packagename2.TestClass3: ");
        BeanDefinition prototypeBean1 = applicationContext.getBeanDefinition(Class.forName("packagename.packagename2.TestClass3"));
        System.out.println("Полученный бин: " + prototypeBean1.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.packagename2.TestClass3: ");
        BeanDefinition prototypeBean2 = applicationContext.getBeanDefinition(Class.forName("packagename.packagename2.TestClass3"));
        System.out.println("Полученный бин: " + prototypeBean2.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.TestClass1: ");
        BeanDefinition prototypeBean3 = applicationContext.getBeanDefinition(Class.forName("packagename.TestClass1"));
        System.out.println("Полученный бин: " + prototypeBean3.getObject());

        System.out.println("\nЗапросили у контейнера prototype bean packagename.TestClass1: ");
        BeanDefinition prototypeBean4 = applicationContext.getBeanDefinition(Class.forName("packagename.TestClass1"));
        System.out.println("Полученный бин: " + prototypeBean4.getObject());

        System.out.println("\nЗапросили у контейнера singleton bean packagename.packagename2.TestClass2: ");
        BeanDefinition singletonBean2 = applicationContext.getBeanDefinition(Class.forName("packagename.packagename2.TestClass2"));
        System.out.println("Полученный бин: " + singletonBean2.getObject());

        System.out.println("\nЗапросили у контейнера singleton bean packagename.packagename2.ITestClass: ");
        BeanDefinition singletonBean3 = applicationContext.getBeanDefinition(Class.forName("packagename.packagename2.ITestClass"));
        System.out.println("Полученный бин: " + singletonBean3.getObject());
    }
}
