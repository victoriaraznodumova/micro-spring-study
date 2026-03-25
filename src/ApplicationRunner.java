import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class ApplicationRunner {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException {
        ApplicationContext applicationContext = new ApplicationContext("packagename");
        System.out.println("Список бинов в контексте:");;
        applicationContext.getNewBeansMap().forEach((k, v) -> System.out.println("id бина: " + k + ", bean definition: " + v.toString()));
        System.out.println("\nИнъекция зависимостей для созданных бинов:");
        DependencyInjector dependencyInjector = new DependencyInjector(applicationContext.getNewBeansMap());
        dependencyInjector.dependencyInjection();
    }
}
