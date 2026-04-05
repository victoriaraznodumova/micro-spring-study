import annotations.MyAutowired;
import annotations.MyQualifier;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.stream.Collectors.toList;

public class DependencyInjector {
    private final Map<String, BeanDefinition> beansMap;
    private List<String> sortedBeans = new ArrayList<>(); //для хранения результата топологической сортировки

    public DependencyInjector(Map<String, BeanDefinition> beansMap) {
        this.beansMap = beansMap;
    }

    public Map<String, Set<String>> getDependencyGraph(){
        AtomicInteger number = new AtomicInteger(); //для удобства, потом удалю
        Map<String, Set<String>> graph = new HashMap<>();
        beansMap.forEach((beanId, beanDef) -> {
            if (beanDef == null){
                System.out.println("Бин " + beanId + " содержит значение null");
            }
            else{
                Class<?> clazz = beanDef.getBeanClass();
                Object object = beanDef.getObject();
                number.addAndGet(1);
                List<Field> autowiredFields = Arrays.stream(object.getClass().getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(MyAutowired.class))
                        .collect(toList());
                autowiredFields.forEach(field -> {
                    System.out.println(number + ") В бине типа " + clazz.getSimpleName()
                            + " инжектится поле " + field.getName() + " типа " + field.getType().getSimpleName());
                    if (field.getType() != null) {
                        graph.computeIfAbsent(clazz.getName(), k -> new HashSet<String>()).add(field.getType().getName());
                    }
                });
                if (autowiredFields.isEmpty()) {
                    System.out.println(number + ") Бин " + object + " не имеет зависимостей");
                    System.out.println();
                    return;
                }
                graph.forEach((k, v) -> {
                    System.out.print("Бин типа " + k + " содержит зависимости: ");
                    v.forEach(dep -> System.out.print(dep + " "));
                    System.out.println();
                });
//            findValidDependencyPaths(clazz, dependencyGraph, new ArrayList<>());
                System.out.println();
            }
        });
        return graph;
    }

    public List<String> topologicalSort(Map<String, Set<String>> graph){
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) { //чтобы не посещать одну и ту же несколько раз
                if (hasCycle(node, graph, visited, recursionStack)) {
//                    throw new RuntimeException("Найдена циклическая зависимость в графе бинов");
                    System.out.println("ЛОГ: найдена циклическая зависимость, потом будет реализация трехуровневого кэша");
                    break;
                }
            }
        }
        visited.clear();
        List<String> result = new ArrayList<>();
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                dfs(node, graph, visited, result);
            }
        }
        return result;
    }

    private boolean hasCycle(String node, Map<String, Set<String>> graph,
                             Set<String> visited, Set<String> recursionStack) {
        visited.add(node);
        recursionStack.add(node);
        for (String neighbor : graph.getOrDefault(node, new HashSet<>())) {
            if (!visited.contains(neighbor)) {
                if (hasCycle(neighbor, graph, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                return true;
            }
        }
        recursionStack.remove(node);
        return false;
    }

    private void dfs(String node, Map<String, Set<String>> graph,
                     Set<String> visited, List<String> result) {
        visited.add(node);
        for (String neighbor : graph.getOrDefault(node, new HashSet<>())) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, graph, visited, result);
            }
        }
        result.add(node);
    }

    public void inject(List<String> sortedBeans) throws IllegalAccessException {
        System.out.println("Будет выполняться инъекция зависимостей");
        for(String beanID: sortedBeans) {
            BeanDefinition beanDefinition = beansMap.get(beanID);
            if (beanDefinition == null) {
                System.out.println("Бин " + beanID + " не найден в контексте");
                continue;
            }
            Object obj = beanDefinition.getObject();
            Class<?> clazz = beanDefinition.getBeanClass();
            if ((obj == null) || (clazz == null)){
                System.out.println("Бин " + beanID + " содержит значение null ");
                continue;
            }
            else{
                for (Field field: clazz.getDeclaredFields()){
                    if (!field.isAnnotationPresent(MyAutowired.class)) continue;
                    Class<?> dependencyClass = field.getType();
                    String dependencyID = ApplicationContext.generateBeanId(dependencyClass);
                    BeanDefinition dependencyBeanDefinition = beansMap.get(dependencyID);
                    if (dependencyBeanDefinition == null){
                        System.out.println("Бин " + dependencyID + " не найден в контексте");
                        continue;
                    }
                    Object dependencyObject = dependencyBeanDefinition.getObject();
                    if (dependencyObject == null){
                        System.out.println("Бин " + dependencyID + " содержит null");
                        continue;
                    }
                    field.setAccessible(true);
                    field.set(obj, dependencyObject);
                    System.out.println("В бине " + obj + " в поле " + field.getName() + " только что была выполнена инъекция зависимости " + field.get(obj));
                }
            }
        }
        System.out.println();
    }





//    public List<List<Class<?>>> findValidDependencyPaths(Class<?> clazz, Map<Class<?>, List<Class<?>>> graph,
//                                                         List<Class<?>> path)
//    {
//        path.add(clazz);
//        List<List<Class<?>>> validDependencyPaths = new ArrayList<>();
//        List<Class<?>> dependencies = graph.get(clazz);
//        if (dependencies == null || dependencies.isEmpty()) {
//            validDependencyPaths.add(new ArrayList<>(path));
//            printPathOfDependencies(path);
//            return validDependencyPaths;
//        }
//        for (Class<?> dependency : dependencies) {
//            try {
//                System.out.println(clazz.getSimpleName() + " -> " + dependency.getSimpleName());
//                if (path.contains(dependency)) {
//                    throw new CyclicDependencyException(getCyclicDependencyMessage(path, dependency));
//                }
//                String generatedBeanId = ApplicationContext.generateBeanId(dependency); //как брать бин из мапы, если его айдишник генерируется по-другому....
//                Object dependencyObject = beansMap.get(generatedBeanId).getObject();
//                if (dependencyObject == null) continue;
//                List<Class<?>> nextDependencies = Arrays.stream(dependencyObject.getClass().getDeclaredFields())
//                        .filter(field -> field.isAnnotationPresent(MyAutowired.class))
//                        .map(Field::getType)
//                        .collect(Collectors.toList());
//                if (nextDependencies.isEmpty()) {
//                    List<Class<?>> fullPath = new ArrayList<>(path);
//                    fullPath.add(dependency);
//                    validDependencyPaths.add(fullPath);
//                    printPathOfDependencies(fullPath);
//                    inject(fullPath);
//                } else {
//                    Map<Class<?>, List<Class<?>>> nextGraph = new HashMap<>();
//                    nextGraph.put(dependency, nextDependencies);
//                    List<List<Class<?>>> subPaths = findValidDependencyPaths(dependency, nextGraph, new ArrayList<>(path));
//                    validDependencyPaths.addAll(subPaths);
//                }
//            } catch (CyclicDependencyException e) {
//                System.out.println(e.getMessage());
//            }
//            catch (NullPointerException e){
//                System.out.println("Бин типа " + dependency.getSimpleName() + " не найден в контексте");
//                System.out.println(e.getMessage());
//            }
//        }
//        return validDependencyPaths;
//    }
//    public void printPathOfDependencies(List<Class<?>> path){
//        System.out.print("Полная цепочка зависимостей: ");
//        for (int i = 1; i < path.size(); i++) {
//            System.out.println("бин типа " + path.get(i - 1).getSimpleName() + " зависит от бина типа " + path.get(i).getSimpleName());
//        }
//    }
//    public String getCyclicDependencyMessage(List<Class<?>> path, Class<?> lastDependency){
//        StringBuilder sb = new StringBuilder();
//        sb.append("Найдена циклическая зависимость:\n");
//        for (int i = 1; i < path.size(); i++) {
//            sb.append("бин типа ").append(path.get(i - 1).getSimpleName()).append(" зависит от бина типа ").append(path.get(i).getSimpleName()).append("\n");
//        }
//        sb.append("бин типа " + path.get(path.size() - 1).getSimpleName() + " зависит от бина типа " + lastDependency.getSimpleName()).append("\n");
//        sb.append("Инъекция циклических зависимостей невозможна\n");
//        return sb.toString();
//    }
}
