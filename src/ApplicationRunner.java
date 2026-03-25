import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class ApplicationRunner {
    public static void main(String[] args) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException {
        ApplicationContext applicationContext = new ApplicationContext("packagename");
        System.out.println("Список бинов в контексте:");
        applicationContext.getBeansMap().forEach((clazz, object) -> System.out.println("Тип бина: " + clazz.getSimpleName() + ", ссылка на бин: " + object));
        System.out.println("\nИнъекция зависимостей для созданных бинов:");
        DependencyInjector dependencyInjector = new DependencyInjector(applicationContext.getBeansMap());
        dependencyInjector.dependencyInjection();
    }
}
