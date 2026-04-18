import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface BeanFactory {
    BeanDefinition getBeanDefinition(Class<?> beanType) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;
    Map<String,BeanDefinition> getBeansMap();
    void createBeans(List<String> scanPackage) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;
}
