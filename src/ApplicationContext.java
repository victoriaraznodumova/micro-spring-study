import annotations.MyComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ApplicationContext {
    private Map<Class<?>, Object> beansMap = new HashMap<>();

    public Map<Class<?>, Object> getBeansMap() {
        return beansMap;
    }

    public ApplicationContext(String packageName) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, URISyntaxException {
        addNewBean(packageName);
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

    public void addNewBean(String packageName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException {
        List<String> classFilesNames = scanPackage(packageName);
        List<Class<?>> classFiles = classFilesNames.stream().map(classFile -> {
            try {
                return Class.forName(classFile);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(toList());
        List<Class<?>> classesWithAnnotation = new ArrayList<>();
        System.out.println("Проверка на наличие аннотации:");
        for (Class classFile: classFiles) {
            if (markedWithAnnotation(classFile)){
                classesWithAnnotation.add(classFile);
                System.out.println("Класс " + classFile.getSimpleName() + " содержит аннотацию @MyComponent");
            }
        }
        for (Class<?> classFile: classesWithAnnotation) {
            Object newBean = classFile.getDeclaredConstructor().newInstance();
            System.out.println("Добавление бина " + newBean + " в контекст");
            beansMap.put(newBean.getClass(), newBean);
        }
        System.out.println();
    }

    public boolean markedWithAnnotation(Class clazz){
        return clazz.isAnnotationPresent(MyComponent.class); //подумать, нужно ли передавать аннотацию в параметрах
    }

    public boolean isClassFile(Path path){
        return path.getFileName().toString().endsWith(".class");
    }
}
