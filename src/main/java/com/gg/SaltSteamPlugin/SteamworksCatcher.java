package com.gg.SaltSteamPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * 通过反射从宿主程序中扫描并获取 Steamworks4k 实例。
 */
public class SteamworksCatcher {
    private static final String TARGET_CLASS_NAME = "com.xuncorp.steamworks4k.Steamworks4k";

    private static volatile Object cachedInstance = null;
    private static volatile Class<?> cachedClass = null;

    /**
     * 获取缓存的 Steamworks4k 实例（Object 类型），如果尚未获取则触发扫描。
     */
    public static Object getSteamworks() {
        if (cachedInstance == null) {
            synchronized (SteamworksCatcher.class) {
                if (cachedInstance == null) {
                    cachedInstance = scanForSteamworks();
                }
            }
        }
        return cachedInstance;
    }

    /**
     * 获取运行时的 Steamworks4k Class 对象。
     */
    public static Class<?> getSteamworksClass() {
        return cachedClass;
    }

    /**
     * 强制重新扫描并刷新缓存。
     */
    public static Object refreshSteamworks() {
        synchronized (SteamworksCatcher.class) {
            cachedInstance = scanForSteamworks();
        }
        return cachedInstance;
    }

    /**
     * 通过反射调用实例上的 isInitialized() 方法。
     */
    public static boolean isInitialized(Object instance) {
        if (instance == null) return false;
        try {
            // Kotlin 属性 isInitialized 编译后生成 isInitialized() 或 getInitialized() 方法
            Method method = findMethod(instance.getClass(), "isInitialized", 0);
            if (method == null) {
                method = findMethod(instance.getClass(), "getInitialized", 0);
            }
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(instance);
                return Boolean.TRUE.equals(result);
            }
        } catch (Throwable e) {
            System.err.println("[SteamworksCatcher] Failed to check isInitialized: " + e.getMessage());
        }
        return false;
    }

    /**
     * 通过反射调用 setRichPresence(key, value)。
     * Kotlin 的 Result 是 inline class，JVM 层面方法签名会被 mangle，
     * 因此按参数类型(String, String)和方法名前缀匹配。
     *
     * @return true 如果调用成功
     */
    public static boolean setRichPresence(Object instance, String key, String value) {
        if (instance == null) return false;
        try {
            // 尝试查找 setRichPresence 方法
            // Kotlin Result 返回值的方法名会被 mangle 为 setRichPresence-<hash>(String, String)
            Method method = findSetRichPresenceMethod(instance.getClass());
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(instance, key, value);
                // Result<Boolean> 在 JVM 层面是 Object:
                //   成功时为 boxed Boolean，失败时为 Result.Failure 包装
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                // 如果返回值不是 Boolean（可能是其他包装），视为成功
                return result != null;
            }
            System.err.println("[SteamworksCatcher] setRichPresence method not found");
        } catch (Throwable e) {
            System.err.println("[SteamworksCatcher] Failed to call setRichPresence: " + e.getMessage());
        }
        return false;
    }

    /**
     * 在类层次中查找 setRichPresence 方法（含 mangle 后的名称）。
     * 匹配条件：方法名以 "setRichPresence" 开头，接受两个 String 参数。
     */
    private static Method findSetRichPresenceMethod(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                for (Method m : current.getDeclaredMethods()) {
                    if (!m.getName().startsWith("setRichPresence")) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2
                            && params[0] == String.class
                            && params[1] == String.class) {
                        return m;
                    }
                }
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 在类层次中按名称和参数数量查找方法。
     */
    private static Method findMethod(Class<?> clazz, String name, int paramCount) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                for (Method m : current.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                        return m;
                    }
                }
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    // ======================== 扫描逻辑 ========================

    /**
     * 判断某个 Class 是否为 Steamworks4k 或其子类（通过类名匹配，不依赖编译时类型）。
     */
    private static boolean isSteamworksClass(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null) {
            if (TARGET_CLASS_NAME.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    /**
     * 执行扫描，优先通过静态方法获取，其次通过静态字段获取。
     */
    private static Object scanForSteamworks() {
        Set<Class<?>> classes = collectLoadedClasses();

        // 第一轮：优先寻找静态无参方法（返回类型为 Steamworks4k 或其子类）
        for (Class<?> clazz : classes) {
            try {
                Object result = tryExtractFromMethods(clazz);
                if (result != null) {
                    cachedClass = result.getClass();
                    System.out.println("[SteamworksCatcher] Found Steamworks4k via method in: " + clazz.getName());
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        // 第二轮：回退到静态字段
        for (Class<?> clazz : classes) {
            try {
                Object result = tryExtractFromFields(clazz);
                if (result != null) {
                    cachedClass = result.getClass();
                    System.out.println("[SteamworksCatcher] Found Steamworks4k via field in: " + clazz.getName());
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        System.err.println("[SteamworksCatcher] Failed to find Steamworks4k instance.");
        return null;
    }

    /**
     * 收集可扫描的类集合。
     */
    private static Set<Class<?>> collectLoadedClasses() {
        Set<Class<?>> classes = new HashSet<>();

        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        ClassLoader appCl = SteamworksCatcher.class.getClassLoader();
        ClassLoader sysCl = ClassLoader.getSystemClassLoader();

        Set<ClassLoader> loaders = new HashSet<>();
        if (contextCl != null) loaders.add(contextCl);
        if (appCl != null) loaders.add(appCl);
        if (sysCl != null) loaders.add(sysCl);
        for (ClassLoader cl : new HashSet<>(loaders)) {
            ClassLoader parent = cl;
            while (parent != null) {
                loaders.add(parent);
                parent = parent.getParent();
            }
        }

        // 通过反射获取 ClassLoader 内部已加载的 classes
        for (ClassLoader loader : loaders) {
            classes.addAll(getLoadedClasses(loader));
        }

        // 尝试直接加载已知可能的混淆包路径下的类
        String[] candidateClassNames = guessCandidateClassNames();
        for (String className : candidateClassNames) {
            for (ClassLoader loader : loaders) {
                try {
                    Class<?> clazz = Class.forName(className, false, loader);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                }
            }
        }

        return classes;
    }

    /**
     * 通过反射访问 ClassLoader 内部的 classes 字段，获取已加载的类列表。
     */
    private static Set<Class<?>> getLoadedClasses(ClassLoader loader) {
        Set<Class<?>> result = new HashSet<>();
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            Object classesObj = classesField.get(loader);
            if (classesObj instanceof java.util.Collection) {
                for (Object obj : ((java.util.Collection<?>) classesObj).toArray()) {
                    if (obj instanceof Class<?>) {
                        result.add((Class<?>) obj);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    /**
     * 猜测可能的混淆类全限定名。
     */
    private static String[] guessCandidateClassNames() {
        String[] packages = {
                "androidx.compose.foundation",
                "androidx.compose.foundation.layout",
                "androidx.compose.runtime",
                "androidx.compose.ui",
                "androidx.compose.material",
                "androidx.compose.material3",
                "androidx.lifecycle",
                "androidx.activity",
        };
        java.util.List<String> candidates = new java.util.ArrayList<>();
        for (String pkg : packages) {
            for (char c = 'a'; c <= 'z'; c++) {
                candidates.add(pkg + "." + c);
            }
            for (char c = 'A'; c <= 'Z'; c++) {
                candidates.add(pkg + "." + c);
            }
            for (char c1 = 'a'; c1 <= 'z'; c1++) {
                for (char c2 = 'a'; c2 <= 'z'; c2++) {
                    candidates.add(pkg + "." + c1 + c2);
                }
            }
        }
        return candidates.toArray(new String[0]);
    }

    /**
     * 在指定类中查找 static、无参、返回值为 Steamworks4k 的方法并调用。
     */
    private static Object tryExtractFromMethods(Class<?> clazz) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                try {
                    int mod = method.getModifiers();
                    if (!Modifier.isStatic(mod)) continue;
                    if (method.getParameterCount() != 0) continue;
                    if (!isSteamworksClass(method.getReturnType())) continue;

                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result != null && isSteamworksClass(result.getClass())) {
                        return result;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * 在指定类中查找 static、类型为 Steamworks4k 的字段并提取值。
     */
    private static Object tryExtractFromFields(Class<?> clazz) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    int mod = field.getModifiers();
                    if (!Modifier.isStatic(mod)) continue;
                    if (!isSteamworksClass(field.getType())) continue;

                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null && isSteamworksClass(value.getClass())) {
                        return value;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}

