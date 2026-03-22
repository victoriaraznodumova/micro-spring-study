import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ApplicationRunner {
    public static void main(String[] args) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApplicationContext applicationContext = new ApplicationContext("packagename");
        System.out.println("Список бинов в контексте:");
        applicationContext.beansMap.forEach((clazz, object) -> System.out.println(clazz + ": " + object));
    }
}
