import annotations.MyAutowired;
import annotations.MyComponent;
import annotations.MyQualifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            Object newBean = classFile.getDeclaredConstructor().newInstance();
            System.out.println("Добавление бина " + newBean + " в контекст");
            beansMap.put(generateBeanId(classFile), new BeanDefinition(classFile, newBean));
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

    private void validateQualifiers(){
        System.out.println("Валидация аннотации @MyQualifier:");
        for (String beanId: beansMap.keySet()) {
            BeanDefinition def = beansMap.get(beanId);
            Class<?> beanClass = def.getBeanClass();
            for (Field field: beanClass.getDeclaredFields()){
                if (field.isAnnotationPresent(MyQualifier.class)){
                    String qualifierBeanId = field.getAnnotation(MyQualifier.class).beanId();
                    System.out.println("Бин " + beanId + " содержит зависимость " + qualifierBeanId + " в поле " + field.getName() + " через аннотацию @MyQualifier");
                    if (!beansMap.containsKey(qualifierBeanId)){
                        throw new RuntimeException("Бин с id " + qualifierBeanId + " не найден. " +
                                "Поле '" + field.getName() + "' класса '" +
                                beanClass.getSimpleName() + "' требует этот бин");
                    }
                }
            }
        }
        System.out.println("Валидация @MyQualifier пройдена успешно");
    }
}