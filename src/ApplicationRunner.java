import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class ApplicationRunner {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException {
        ApplicationContext applicationContext = new ApplicationContext("packagename");
    }
}
