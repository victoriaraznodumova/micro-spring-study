import annotations.MyAutowired;
import annotations.MyComponent;
import annotations.MyQualifier;
import annotations.MyScope;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ApplicationContext {
    private Map<String, BeanDefinition> beansMap = new ConcurrentHashMap<>(); //айдишник и биндефиниш
    private List<String> sortedBeans = new ArrayList<>(); //для хранения результата топологической сортировки

    public Map<String, BeanDefinition> getBeansMap() {
        return beansMap;
    }

    public ApplicationContext(String packageName) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException {
        createBeans(packageName);
    }

    public Path getTargetPathToPackage(String packageName) throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource(packageName.replace('.', '/'));
        if (url != null) {
            return Paths.get(url.toURI());
        }
        else{
            throw new RuntimeException("Невозможно получить корректный путь к пакету " + packageName);
        }
    }

    public List<String> scanPackage(String packageName) throws URISyntaxException {
        Path targetPath = getTargetPathToPackage(packageName);
        List<String> classFilesList;
        try (Stream<Path> filesAndDirs = Files.walk(targetPath)){
            classFilesList = filesAndDirs
                    .filter(Files::isRegularFile)
                    .filter(this::isClassFile)
                    .map(path -> {
                        Path relativePath = targetPath.relativize(path);
                        String relativeClassName = relativePath.toString()
                                .replace(File.separator, ".")
                                .replace(".class", "");
                        String fullClassName;
                        if (relativeClassName.isEmpty()) {
                            fullClassName = packageName;
                        } else {
                            fullClassName = packageName + "." + relativeClassName;
                        }
                        if (fullClassName.startsWith(".")){
                            fullClassName = fullClassName.substring(1);
                        }
                        return fullClassName;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            System.out.println("Список файлов .class для проверки на наличие аннотации в пакете " + packageName + ":");
            classFilesList.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
            classFilesList = List.of();
        }
        System.out.println();
        return classFilesList;
    }

    public void createBeans(String packageName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException {
        List<String> classFilesNames = scanPackage(packageName);
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
        validateQualifiers();

        System.out.println("\nпостроение графа зависимостей:");
        DependencyInjector dependencyInjector = new DependencyInjector(beansMap);
        Map<String, Set<String>> dependencyGraph = dependencyInjector.getDependencyGraph();
        System.out.println("граф зависимостей:");
        dependencyGraph.forEach((k, v) -> System.out.println("Узел: " + k + ", прямые зависимости: " + v));

        System.out.println("\nвыполнение топологической сортировки:");
        sortedBeans = dependencyInjector.topologicalSort(dependencyGraph);
        System.out.println("отсортированные бины, готовые к инъекции: ");
        System.out.println(sortedBeans);
        System.out.println("\nвнедрение зависимостей:");
        dependencyInjector.inject(sortedBeans);
    }

    public boolean markedWithAnnotation(Class clazz){
        return clazz.isAnnotationPresent(MyComponent.class);
    }

    public boolean isClassFile(Path path){
        return path.getFileName().toString().endsWith(".class");
    }
    public static String generateBeanId(Class<?> clazz){ //пока непонятно, как генерировать айдишник бина???
        String simpleClassName = clazz.getSimpleName();
        String className = clazz.getName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    public static String resolveDependencyId(Field field) {
        if (field.isAnnotationPresent(MyQualifier.class)) {
            return field.getAnnotation(MyQualifier.class).beanId();
        }
        return ApplicationContext.generateBeanId(field.getType());
    }

    private void validateQualifiers(){
        System.out.println("Валидация аннотации @MyQualifier:");
        for (String beanId: beansMap.keySet()) {
            BeanDefinition def = beansMap.get(beanId);
            Class<?> beanClass = def.getBeanClass();
            for (Field field: beanClass.getDeclaredFields()){
                if (field.isAnnotationPresent(MyQualifier.class)){
                    String qualifierBeanId = field.getAnnotation(MyQualifier.class).beanId();
                    System.out.println("Бин " + beanId + " содержит зависимость " + qualifierBeanId + " в поле " + field.getName() + " через аннотацию @MyQualifier");
                    BeanDefinition dependencyBeanDefinition = beansMap.get(qualifierBeanId);
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

    public BeanDefinition getBeanDefinition(Class<?> beanType) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
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
        if (bean == null){
            throw new RuntimeException("Бин типа " + beanType.getName() + " не найден в контейнере");
        }
        return beanDefinition;
    }
}
