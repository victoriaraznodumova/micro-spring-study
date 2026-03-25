import annotations.MyAutowired;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;

public class DependencyInjector {
    private final Map<Class<?>, Object> beansMap;
    public DependencyInjector(Map<Class<?>, Object> beansMap) {
        this.beansMap = beansMap;
    }

    public void dependencyInjection() {
        AtomicInteger number = new AtomicInteger(); //для удобства, потом удалю
        beansMap.forEach((clazz, object) -> {
            number.addAndGet(1);
            Map<Class<?>, List<Class<?>>> dependencyGraph = new HashMap<>();
            List<Field> autowiredFields = Arrays.stream(object.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(MyAutowired.class))
                    .collect(toList());
            autowiredFields.forEach(field -> {
                System.out.println(number + ") В бине типа " + clazz.getSimpleName()
                        + " инжектится поле " + field.getName() + " типа " + field.getType().getSimpleName());
                if (field.getType() != null) {
                    dependencyGraph.computeIfAbsent(clazz, k -> new ArrayList<>()).add(field.getType());
                }
            });
            if (autowiredFields.isEmpty()) { //или если dependencyGraph пустой
                System.out.println(number + ") Бин " + object + " не имеет зависимостей");
                System.out.println();
                return;
            }
            dependencyGraph.forEach((k, v) -> {
                System.out.print("Бин типа " + k.getSimpleName() + " содержит зависимости: ");
                v.forEach(dep -> System.out.print(dep.getSimpleName() + " "));
                System.out.println();
            });
            List<List<Class<?>>> validDependencyPaths = findValidDependencyPaths(clazz, dependencyGraph, new ArrayList<>());
//            if (!validDependencyPaths.isEmpty()) {
//                System.out.println("Будет выполняться инъекция зависимостей в бин типа " + clazz.getSimpleName());
//                for (List<Class<?>> validPath: validDependencyPaths ) {
//                    printPathOfDependencies(validPath, null, false);
//                    inject(validPath);
//                }
//            }
            System.out.println();
        });
    }

    public List<List<Class<?>>> findValidDependencyPaths(Class<?> clazz, Map<Class<?>, List<Class<?>>> dependencyGraph,
                                               List<Class<?>> path)
    {
        path.add(clazz);
        List<List<Class<?>>> validDependencyPaths = new ArrayList<>();
        List<Class<?>> dependencies = dependencyGraph.get(clazz);
        if (dependencies == null || dependencies.isEmpty()) {
            validDependencyPaths.add(new ArrayList<>(path));
            printPathOfDependencies(path);
            return validDependencyPaths;
        }
        for (Class<?> dependency : dependencies) {
            try {
                System.out.println(clazz.getSimpleName() + " -> " + dependency.getSimpleName());
                if (path.contains(dependency)) {
//                    System.out.println("\nНайдена циклическая зависимость");
//                    printPathOfDependencies(path, dependency, true);
//                    continue;
                    throw new CyclicDependencyException(getCyclicDependencyMessage(path, dependency));
                }
                Object dependencyObject = beansMap.get(dependency);
                if (dependencyObject == null) continue;
                List<Class<?>> nextDependencies = Arrays.stream(dependencyObject.getClass().getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(MyAutowired.class))
                        .map(Field::getType)
                        .collect(Collectors.toList());
                if (nextDependencies.isEmpty()) {
                    List<Class<?>> fullPath = new ArrayList<>(path);
                    fullPath.add(dependency);
                    validDependencyPaths.add(fullPath);
                    printPathOfDependencies(fullPath);
                    inject(fullPath);
                } else {
                    Map<Class<?>, List<Class<?>>> nextGraph = new HashMap<>();
                    nextGraph.put(dependency, nextDependencies);
                    List<List<Class<?>>> subPaths = findValidDependencyPaths(dependency, nextGraph, new ArrayList<>(path));
                    validDependencyPaths.addAll(subPaths);
                }
            } catch (CyclicDependencyException e) {
                    System.out.println(e.getMessage());
            }
        }
        return validDependencyPaths;
    }

    public void inject(List<Class<?>> validPath){
        System.out.println("Будет выполняться инъекция зависимостей");
        for(int i = validPath.size() - 2; i >= 0; i--) {
            Class<?> currentClass = validPath.get(i);
            Class<?> dependencyClass = validPath.get(i + 1);
            Object currentObject = beansMap.get(currentClass);
            Object dependencyObject = beansMap.get(dependencyClass);
            if (currentObject == null) System.out.println("Бина типа " + currentClass.getSimpleName() + " нет в контексте");
            if (dependencyObject == null) System.out.println("Бина типа " + dependencyClass.getSimpleName() + " нет в контексте");
            for (Field field: currentClass.getDeclaredFields()){
                if (!field.isAnnotationPresent(MyAutowired.class)) continue;
                if (!field.getType().equals(dependencyClass)) continue;
                try{
                    field.setAccessible(true);
                    Object existingValue = field.get(currentObject);
                    if (existingValue != null) {
                        System.out.println("В бине " + currentObject + " в поле " + field.getName()
                                + " уже инъектирована зависимость " + existingValue);
                        continue;
                    }
                    field.set(currentObject, dependencyObject);
//                    System.out.println("инжектнули " + dependencyClass.getSimpleName() + " в " + currentClass.getSimpleName());
                    System.out.println("В бине " + currentObject + " в поле " + field.getName() + " только что была выполнена инъекция зависимости " + field.get(currentObject));
                }
                catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
        System.out.println();
    }

    public void printPathOfDependencies(List<Class<?>> path){
        System.out.print("Полная цепочка зависимостей: ");
        for (int i = 1; i < path.size(); i++) {
            System.out.println("бин типа " + path.get(i - 1).getSimpleName() + " зависит от бина типа " + path.get(i).getSimpleName());
        }
    }

    public String getCyclicDependencyMessage(List<Class<?>> path, Class<?> lastDependency){
        StringBuilder sb = new StringBuilder();
        sb.append("Найдена циклическая зависимость:\n");
        for (int i = 1; i < path.size(); i++) {
            sb.append("бин типа ").append(path.get(i - 1).getSimpleName()).append(" зависит от бина типа ").append(path.get(i).getSimpleName()).append("\n");
        }
        sb.append("бин типа " + path.get(path.size() - 1).getSimpleName() + " зависит от бина типа " + lastDependency.getSimpleName()).append("\n");
        sb.append("Инъекция циклических зависимостей невозможна\n");
        return sb.toString();
    }
}
