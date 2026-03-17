import annotations.MyComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ApplicationContext {
    static ArrayList<Bean> beans = new ArrayList<>();
    public static List<String> scanPackage(String packageName) throws IOException {
        Path targetPath = getTargetPath(packageName);
        List<String> classFilesList;
        try (Stream<Path> filesAndDirs = Files.walk(targetPath)){
            classFilesList = filesAndDirs
                    .filter(Files::isRegularFile)
                    .filter(ApplicationContext::isClassFile)
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

    public static Path getTargetPath(String packageName) throws IOException {
        String projectRoot = System.getProperty("user.dir");
        File outDir = new File(projectRoot, "out");
        Path targetPath;
        try (Stream<Path> path = Files.walk(Path.of(outDir.getPath()))){
            targetPath = path.filter(p ->
                            p.toString().endsWith(packageName.replace('.', File.separatorChar)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Пакет " + packageName + " не найден"));
        }
        System.out.println(targetPath);
        return targetPath;
    }

    public static void addNewBean(String packageName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
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
        for (Class classFile: classesWithAnnotation) {
            Bean newBean = new Bean(classFile.getDeclaredConstructor().newInstance());
            beans.add(newBean);
        }
        System.out.println();
    }

    public static boolean markedWithAnnotation(Class className){
        return className.isAnnotationPresent(MyComponent.class); //подумать, нужно ли передавать аннотацию в параметрах
    }

    public static boolean isClassFile(Path path){
        return path.getFileName().toString().endsWith(".class");
    }

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        //типа тест
        addNewBean("packagename.packagename2");
        addNewBean("annotations");
        addNewBean("packagename");
//        addNewBean("");
        System.out.println("Список бинов в контексте:");
        for (Bean bean: beans) {
            System.out.println(bean.getBeanObject().getClass());
        }
    }
}