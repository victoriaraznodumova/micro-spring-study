import annotations.MyComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ApplicationContext {
    Map<Class<?>, Object> beansMap = new HashMap<>();

    public ApplicationContext(String packageName) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        addNewBean(packageName);
    }

    public List<String> scanPackage(String packageName) throws IOException {
        Path targetPath = getTargetPath(packageName);
        List<String> classFilesList;
        try (Stream<Path> filesAndDirs = Files.walk(targetPath)){
            classFilesList = filesAndDirs
                    .filter(Files::isRegularFile)
                    .filter(path1 -> isClassFile(path1))
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
        return classFilesList;
    }

    public Path getTargetPath(String packageName) throws IOException {
        String projectRoot = System.getProperty("user.dir");
        File outDir = new File(projectRoot, "out");
        Path targetPath;
        try (Stream<Path> path = Files.walk(Path.of(outDir.getPath()))){
            targetPath = path.filter(p ->
                            p.toString().endsWith(packageName.replace('.', File.separatorChar)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Пакет " + packageName + " не найден"));
        }
//        System.out.println(targetPath);
        return targetPath;
    }

    public void addNewBean(String packageName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        List<String> classFilesList = scanPackage(packageName);
        List<Class<?>> classFiles = classFilesList.stream().map(classFile -> {
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
