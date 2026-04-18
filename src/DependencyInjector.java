import annotations.MyAutowired;
import annotations.MyQualifier;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DependencyInjector {
    private final Map<String, BeanDefinition> beansMap;
    private List<String> sortedBeans = new ArrayList<>(); //для хранения результата топологической сортировки

    public DependencyInjector(Map<String, BeanDefinition> beansMap) {
        this.beansMap = beansMap;
    }

    public Map<String, Set<String>> getDependencyGraph() {
        AtomicInteger number = new AtomicInteger(); //для удобства, потом удалю
        Map<String, Set<String>> graph = new HashMap<>();
        beansMap.forEach((beanId, beanDef) -> {
            if (beanDef == null){
                System.out.println("Бин " + beanId + " содержит значение null");
            }
            else{
                Class<?> clazz = beanDef.getBeanClass();
                number.addAndGet(1);
                List<Field> autowiredFields = new ArrayList<>();
                List<Field> qualifierFields = new ArrayList<>();
                Arrays.stream(clazz.getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(MyAutowired.class))
                        .forEach((field) -> {
                            if (field.isAnnotationPresent(MyQualifier.class)){
                                qualifierFields.add(field);
                            }
                            else{
                                autowiredFields.add(field);
                            }});
                autowiredFields.forEach(field -> {
                    System.out.println(number + ") В бине типа " + clazz.getSimpleName()
                            + " инжектится поле " + field.getName() + " типа " + field.getType().getSimpleName());
                    if (field.getType() != null) {
                        graph.computeIfAbsent(clazz.getName(), k -> new HashSet<String>()).add(field.getType().getName());
                    }
                });
                qualifierFields.forEach(field -> {
                    System.out.println(number + ") В бине типа " + clazz.getSimpleName()
                            + " инжектится @MyQualifier поле " + field.getName() + " типа " + field.getType().getSimpleName()
                            + ", нужен бин " + field.getDeclaredAnnotation(MyQualifier.class).beanId());
                    if (field.getType() != null) {
                        graph.computeIfAbsent(clazz.getName(), k -> new HashSet<String>()).add(field.getDeclaredAnnotation(MyQualifier.class).beanId());
                    }
                });
                if (autowiredFields.isEmpty() && qualifierFields.isEmpty()) {
                    System.out.println(number + ") Бин " + beanId + " не содержит зависимости");
                    System.out.println();
                    return;
                }
                graph.forEach((k, v) -> {
                    System.out.print("Бин типа " + k + " содержит зависимости: ");
                    v.forEach(dep -> System.out.print(dep + " "));
                    System.out.println();
                });
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

    public void inject(List<String> sortedBeans) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        System.out.println("Будет выполняться инъекция зависимостей");
        for(String beanID: sortedBeans) {
            BeanDefinition beanDefinition = beansMap.get(beanID);
            if (beanDefinition == null) {
                System.out.println("Бин " + beanID + " содержит значение null");
                continue;
            }
            Object obj = beanDefinition.getObject();
            Class<?> clazz = beanDefinition.getBeanClass();
            if ((obj == null) || (clazz == null)){
//                System.out.println("Бин " + beanID + " содержит значение null ");
                continue;
            }
            else{
                for (Field field: clazz.getDeclaredFields()) {
                    boolean hasAutowired = field.isAnnotationPresent(MyAutowired.class);
                    boolean hasQualifier = field.isAnnotationPresent(MyQualifier.class);
                    if (!hasAutowired && !hasQualifier) continue;
                    Class<?> dependencyClass = field.getType();
                    try {
                        field.setAccessible(true);
                        Object existingValue = field.get(obj);
                        if (existingValue != null) {
                            System.out.println("В бине " + obj + " в поле " + field.getName() + " уже инъектирована зависимость " + existingValue);
                            continue;
                        }
                        String dependencyBeanID;
                        if (hasQualifier) {
                            dependencyBeanID = field.getAnnotation(MyQualifier.class).beanId();
                        } else {
                            dependencyBeanID = ApplicationContext.generateBeanId(dependencyClass);
                        }
                        BeanDefinition dependencyBeanDefinition = beansMap.get(dependencyBeanID);
                        if (dependencyBeanDefinition == null) {
                            System.out.println("Бин с id " + dependencyBeanID + " не найден для инъекции в поле '"
                                    + field.getName() + "' класса '" + clazz.getSimpleName());
                            continue;
                        }
                        Object dependencyObject = getBeanInstance(dependencyBeanDefinition);
//                        if (dependencyBeanDefinition.isBeanSingleton()){
//                            dependencyObject = dependencyBeanDefinition.getObject();
//                        } else if (dependencyBeanDefinition.isBeanPrototype()){
//                            dependencyObject = dependencyBeanDefinition.createNewInstance();
//                        }
                        if (dependencyObject == null) {
                            System.out.println("Не удалось создать бин " + dependencyBeanID);
                            continue;
                        }
                        field.set(obj, dependencyObject);
                        System.out.println("В бине " + obj + " в поле " + field.getName() +
                                " только что была выполнена инъекция зависимости " + field.get(obj));
                    }
                    catch (IllegalAccessException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println();
    }

    public Object getBeanInstance(BeanDefinition beanDefinition) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (beanDefinition.isBeanSingleton()){
            return beanDefinition.getObject();
        } else if (beanDefinition.isBeanPrototype()){
            return initializePrototypeBean(beanDefinition);
        }
        return null;
    }

    private Object initializePrototypeBean(BeanDefinition beanDefinition) {
        try {
            Object object = beanDefinition.getBeanClass().getDeclaredConstructor().newInstance();
//            Object object = beanDefinition.createNewInstance();
            for (Field field : beanDefinition.getBeanClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(MyAutowired.class) &&
                        !field.isAnnotationPresent(MyQualifier.class)) {
                    continue;
                }
                String dependencyId = ApplicationContext.resolveDependencyId(field);
                BeanDefinition dependencyBeanDefinition = beansMap.get(dependencyId);
                if (dependencyBeanDefinition == null) continue;
                Object dependency = getBeanInstance(dependencyBeanDefinition);
                field.setAccessible(true);
                field.set(object, dependency);
                System.out.println("В бине " + object + " в поле " + field.getName() +
                        " только что была выполнена инъекция зависимости " + field.get(object));
            }
            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
