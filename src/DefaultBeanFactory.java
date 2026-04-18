import annotations.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

public class DefaultBeanFactory implements BeanFactory{
    private DependencyInjector dependencyInjector;

    private Map<String, BeanDefinition> beansMap = new ConcurrentHashMap<>(); //айдишник и биндефиниш
    private List<String> sortedBeans = new ArrayList<>(); //для хранения результата топологической сортировки

    public Map<String, BeanDefinition> getBeansMap() {
        return beansMap;
    }

    public List<String> getSortedBeans() {
        return sortedBeans;
    }
    public void createBeans(List<String> classFilesNames) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        createBeansWithoutInitialization(classFilesNames);
        validateQualifiers();
        dependencyInjector = new DependencyInjector(beansMap);
        System.out.println("\nпостроение графа зависимостей:");
        Map<String, Set<String>> dependencyGraph = getDependencyGraph();
        System.out.println("граф зависимостей:");
        dependencyGraph.forEach((k, v) -> System.out.println("Узел: " + k + ", прямые зависимости: " + v));
        System.out.println("\nвыполнение топологической сортировки:");
        processTopologicalSort();
        System.out.println("отсортированные бины, готовые к инъекции: ");
        System.out.println(getSortedBeans());
        System.out.println("\nвнедрение зависимостей:");
        inject();
    }

    private Map<String, BeanDefinition> createBeansWithoutInitialization(List<String> classFilesNames) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Class<?>> classFiles = classFilesNames.stream().map(classFile -> {
            try {
                return Class.forName(classFile);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(toList());
        List<Class<?>> classesWithAnnotation = new ArrayList<>();
        System.out.println("Проверка на наличие аннотации @MyComponent:");
        for (Class classFile: classFiles) {
            if (markedWithAnnotation(classFile)){
                classesWithAnnotation.add(classFile);
                System.out.println("Класс " + classFile.getSimpleName() + " содержит аннотацию @MyComponent");
            }
        }
        System.out.println("\nсоздание бинов без инициализации: ");
        for (Class<?> classFile: classesWithAnnotation) {
            Object newBean = null;
            MyScope annotationScope = classFile.getAnnotation(MyScope.class);
            String scopeValue;
            if (annotationScope == null){
                scopeValue = "singleton";
//                throw new RuntimeException("Для типа " + classFile + " не указан scope");
            }
            else{
                scopeValue = annotationScope.value();
            }
            BeanDefinition.Scope beanDefinitonScope = BeanDefinition.Scope.valueOf(scopeValue.toUpperCase(Locale.ROOT));
            if (beanDefinitonScope == BeanDefinition.Scope.SINGLETON){
                newBean = classFile.getDeclaredConstructor().newInstance();
                System.out.println("Добавление singleton бина " + newBean + " в контекст");
            }
            else if (beanDefinitonScope == BeanDefinition.Scope.PROTOTYPE){
                System.out.println("Регистрация prototype бина " + classFile.getName());
            }
            beansMap.put(generateBeanId(classFile), new BeanDefinition(classFile, newBean, beanDefinitonScope));
        }
        System.out.println();
        return beansMap;
    }

    public boolean markedWithAnnotation(Class clazz){
        return clazz.isAnnotationPresent(MyComponent.class);
    }

    public static String generateBeanId(Class<?> clazz){ //пока непонятно, как генерировать айдишник бина???
        String simpleClassName = clazz.getSimpleName();
        String className = clazz.getName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private void validateQualifiers(){
        System.out.println("Валидация аннотации @MyQualifier:");
        for (String beanId: getBeansMap().keySet()) {
            BeanDefinition def = getBeansMap().get(beanId);
            Class<?> beanClass = def.getBeanClass();
            for (Field field: beanClass.getDeclaredFields()){
                if (field.isAnnotationPresent(MyQualifier.class)){
                    String qualifierBeanId = field.getAnnotation(MyQualifier.class).beanId();
                    System.out.println("Бин " + beanId + " содержит зависимость " + qualifierBeanId + " в поле " + field.getName() + " через аннотацию @MyQualifier");
                    BeanDefinition dependencyBeanDefinition = getBeansMap().get(qualifierBeanId);
                    if (dependencyBeanDefinition == null){
                        throw new RuntimeException("Бин с id " + qualifierBeanId + " не найден. " +
                                "Поле '" + field.getName() + "' класса '" +
                                beanClass.getSimpleName() + "' требует этот бин");
                    }
                }
            }
        }
        System.out.println("Валидация @MyQualifier пройдена успешно");
    }

    public Map<String, Set<String>> getDependencyGraph(){
        Map<String, Set<String>> dependencyGraph = dependencyInjector.getDependencyGraph();
        return dependencyGraph;
    }

    public void processTopologicalSort(){
        sortedBeans = dependencyInjector.topologicalSort(getDependencyGraph());
        System.out.println("отсортированные бины, готовые к инъекции: ");
        System.out.println(sortedBeans);
    }

    public void inject() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        dependencyInjector.inject(sortedBeans);
    }

    @Override
    public BeanDefinition getBeanDefinition(Class<?> beanType) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Object bean = null;
        if (beanType == null){
            throw new RuntimeException("Для поиска бина передан тип null");
        }
        BeanDefinition beanDefinition = null;
        for (Map.Entry<String, BeanDefinition> entry: beansMap.entrySet()){
            beanDefinition = entry.getValue();
            Class<?> existingType = beanDefinition.getBeanClass();
            if (beanType.equals(existingType)){
                System.out.println("Проверка, значение бина: " + beanDefinition.getObject() + ", тип бина: " + beanDefinition.getBeanClass() + ", область видимости бина: " + beanDefinition.getScope());
                MyScope scope = existingType.getAnnotation(MyScope.class);
                return resolveByScope(beanDefinition, scope);
            }
        }
        if (bean == null){
            throw new RuntimeException("Бин типа " + beanType.getName() + " не найден в контейнере");
        }
        return beanDefinition;
    }

    private BeanDefinition resolveByScope(BeanDefinition beanDefinition, MyScope scope) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String scopeValue;
        if (scope == null){
//                    throw new RuntimeException("Для найденного бина типа " + beanType + " не указан scope");
            scopeValue = "singleton";
        }
        else{
            scopeValue = scope.value();
        }
        if (scopeValue.equals("singleton")){
//                    bean = beanDefinition.getObject();
            return beanDefinition;
        }
        if (scopeValue.equals("prototype")){
//                    bean = beanDefinition.createNewInstance();
            DependencyInjector dependencyInjector = new DependencyInjector(beansMap);
            return new BeanDefinition(beanDefinition.getBeanClass(), dependencyInjector.getBeanInstance(beanDefinition));
        }
        if (scopeValue.equals("thread")){

        }
        return beanDefinition;
    }
}
