package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.WorkshopApi;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 通过反射从宿主程序中扫描并获取 Steamworks4k 实例。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>优先从配置缓存中加载上次扫描到的类名</li>
 *   <li>缓存未命中时，定位宿主 JAR 并遍历其中的类进行反射扫描</li>
 *   <li>扫描成功后将结果写入配置缓存，下次启动可快速加载</li>
 * </ol>
 */
public class SteamworksCatcher {

    private static final String TAG = "[SteamworksCatcher] ";
    private static final String TARGET_CLASS_NAME = "com.xuncorp.steamworks4k.Steamworks4k";

    private static final AtomicReference<Object> cachedInstance = new AtomicReference<>(null);
    private static String currentSpwInfo = "";

    private SteamworksCatcher() {
    }

    public static void init(String spwVersion, String spwChannel) {
        currentSpwInfo = spwVersion + "@" + spwChannel;
    }

    /**
     * 获取 Steamworks4k 实例（懒加载，双重检查锁定）。
     * 优先从配置缓存加载，未命中则触发 JAR 扫描。
     */
    public static Object getSteamworks() {
        Object instance = cachedInstance.get();
        if (instance == null) {
            synchronized (SteamworksCatcher.class) {
                instance = cachedInstance.get();
                if (instance == null) {
                    instance = tryLoadFromCache();
                    if (instance == null) {
                        instance = scanForSteamworks();
                    }
                    cachedInstance.set(instance);
                }
            }
        }
        return instance;
    }

    /**
     * 强制重新扫描并刷新缓存。
     */
    public static void refreshSteamworks() {
        Object instance;
        synchronized (SteamworksCatcher.class) {
            instance = scanForSteamworks();
            cachedInstance.set(instance);
        }
        if (instance != null) {
            System.out.println(TAG + "Steamworks4k instance refreshed successfully");
            WorkshopApi.ui().toast("Steamworks4k instance refreshed successfully", WorkshopApi.Ui.ToastType.Success);
        } else {
            System.err.println(TAG + "Failed to refresh Steamworks4k instance");
            WorkshopApi.ui().toast("Failed to refresh Steamworks4k instance", WorkshopApi.Ui.ToastType.Error);
        }
    }

    /**
     * 通过反射调用实例上的 {@code isInitialized()} 方法。
     */
    public static boolean isInitialized(Object instance) {
        if (instance == null) return false;
        try {
            // Kotlin 属性 isInitialized 编译后可能生成 isInitialized() 或 getInitialized()
            Method method = findMethod(instance.getClass(), "isInitialized");
            if (method == null) {
                method = findMethod(instance.getClass(), "getInitialized");
            }
            if (method != null) {
                method.setAccessible(true);
                return Boolean.TRUE.equals(method.invoke(instance));
            }
        } catch (InvocationTargetException e) {
            logError("Error invoking isInitialized: " + e.getCause());
        } catch (IllegalAccessException e) {
            logError("Illegal access in isInitialized: " + e.getMessage());
        }
        return false;
    }

    /**
     * 通过反射调用 {@code setRichPresence(key, value)}。
     * <p>
     * Kotlin 的 {@code Result} 是 inline class，JVM 层面方法签名会被 mangle，
     * 因此按参数类型 {@code (String, String)} 和方法名前缀匹配。
     *
     * @return true 如果调用成功
     */
    public static boolean setRichPresence(Object instance, String key, String value) {
        if (instance == null) return false;
        try {
            Method method = findSetRichPresenceMethod(instance.getClass());
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(instance, key, value);
                // Result<Boolean> 在 JVM 层面：成功时为 boxed Boolean，失败时为 Result.Failure 包装
                return result instanceof Boolean bool ? bool : result != null;
            }
            logError("setRichPresence method not found");
        } catch (Exception e) {
            logError("Failed to call setRichPresence: " + e.getMessage());
        }
        return false;
    }

    // ==================== 缓存加载 ====================

    /**
     * 尝试从配置文件缓存中加载上次扫描成功的类，避免重复扫描。
     */
    private static Object tryLoadFromCache() {
        Config config = Config.getInstance();
        String cachedSpwInfo = config.getCachedSpwInfo();
        String cachedClassName = config.getCachedSteamworksClassName();

        if (!currentSpwInfo.equals(cachedSpwInfo)
                || cachedClassName == null
                || cachedClassName.isEmpty()) {
            return null;
        }

        try {
            logInfo("Trying to load from cache: " + cachedClassName);
            Class<?> clazz = Class.forName(cachedClassName, false, Thread.currentThread().getContextClassLoader());

            Object result = tryExtractInstance(clazz);
            if (result != null) {
                logInfo("Loaded from cache successfully.");
                return result;
            }
        } catch (Exception e) {
            logError("Failed to load from cache: " + e.getMessage());
        }
        return null;
    }

    // ==================== JAR 扫描 ====================

    /**
     * 通过已知未混淆的目标类，反向定位宿主 JAR 并遍历其中所有类，
     * 寻找持有 Steamworks4k 实例的静态方法或字段。
     */
    private static Object scanForSteamworks() {
        try {
            File hostJar = locateHostJar();
            if (hostJar == null) return null;

            Class<?> knownHostClass = Class.forName(TARGET_CLASS_NAME);
            ClassLoader hostClassLoader = knownHostClass.getClassLoader();

            Object result = scanJarForInstance(hostJar, hostClassLoader);
            if (result != null) return result;

        } catch (Exception e) {
            logError("Exception during scan: " + e.getMessage());
        }

        logError("Failed to find Steamworks4k instance.");
        WorkshopApi.ui().toast(
                "[SaltSteamPlugin] Failed to find Steamworks4k instance.",
                WorkshopApi.Ui.ToastType.Error
        );
        return null;
    }

    /**
     * 定位包含目标类的宿主 JAR 文件路径。
     */
    private static File locateHostJar() {
        Class<?> knownHostClass;
        try {
            knownHostClass = Class.forName(TARGET_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            logError("Known host class not found: " + TARGET_CLASS_NAME);
            return null;
        }

        ClassLoader loader = knownHostClass.getClassLoader();
        logInfo("ClassLoader: " + loader);

        URL location = null;
        var pd = knownHostClass.getProtectionDomain();
        if (pd != null) {
            CodeSource cs = pd.getCodeSource();
            if (cs != null) {
                location = cs.getLocation();
            }
        }

        if (location != null) {
            logInfo("Location from CodeSource: " + location);
            try {
                File hostJar = new File(location.toURI());
                logInfo("Found JAR via CodeSource: " + hostJar.getAbsolutePath());
                return hostJar;
            } catch (URISyntaxException e) {
                logError("Invalid URI syntax for host location: " + e.getMessage());
            }
        }

        // 备用方案：通过 class resource 反推 JAR 路径
        String resourceName = TARGET_CLASS_NAME.replace('.', '/') + ".class";
        URL resource = loader != null ? loader.getResource(resourceName) : ClassLoader.getSystemResource(resourceName);
        logInfo("Class resource: " + resource);

        if (resource == null) {
            logError("Cannot find class resource for " + TARGET_CLASS_NAME);
            return null;
        }

        String spec = resource.toString();
        try {
            // 解析类似 "jar:file:/xxx/app.jar!/com/..." 的路径
            if (spec.startsWith("jar:file:")) {
                int bang = spec.indexOf("!/");
                if (bang > 0) {
                    String jarUrlPart = spec.substring(4, bang); // 剥离最前面的 "jar:"
                    File hostJar = new File(new URI(jarUrlPart));
                    logInfo("Found JAR via resource: " + hostJar.getAbsolutePath());
                    return hostJar;
                }
            } else if (spec.startsWith("file:")) {
                logError("Class is loaded from file directory, not a jar: " + spec);
                return null;
            }
        } catch (URISyntaxException e) {
            logError("Invalid URI syntax for class resource: " + e.getMessage());
        }

        logError("Could not determine host JAR location from CodeSource or resource URL");
        return null;
    }

    /**
     * 遍历 JAR 中的所有 .class 文件，逐个尝试提取 Steamworks4k 实例。
     */
    private static Object scanJarForInstance(File jarFile, ClassLoader classLoader) {
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!isCandidate(entryName)) continue;

                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                Object result = tryLoadAndExtract(className, classLoader);
                if (result != null) return result;
            }
        } catch (Exception e) {
            logError("Failed to scan JAR: " + e.getMessage());
        }
        return null;
    }

    /**
     * 判断 JAR 条目是否为候选扫描类（排除 kotlin 内部类和 META-INF）。
     */
    private static boolean isCandidate(String entryName) {
        return entryName.endsWith(".class")
                && !entryName.startsWith("kotlin/")
                && !entryName.startsWith("META-INF/");
    }

    /**
     * 加载单个类并尝试从中提取 Steamworks4k 实例，成功时更新缓存。
     */
    private static Object tryLoadAndExtract(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);

            Object result = tryExtractInstance(clazz);
            if (result != null) {
                String msg = "Found Steamworks4k in: " + clazz.getName();
                logInfo(msg);
                WorkshopApi.ui().toast(TAG + msg, WorkshopApi.Ui.ToastType.Success);

                Config.getInstance().setCachedSteamworksInfo(clazz.getName(), currentSpwInfo);
                return result;
            }
        } catch (Exception ignored) {
            /* Ignore load error */
        }
        return null;
    }

    // ==================== 实例提取 ====================

    /**
     * 从指定类中尝试提取 Steamworks4k 实例：先尝试静态方法，再尝试静态字段。
     */
    private static Object tryExtractInstance(Class<?> clazz) {
        Object result = tryExtractFromMethods(clazz);
        if (result != null) return result;
        result = tryExtractFromFields(clazz);
        if (result != null) return result;
        return tryExtractFromSingleton(clazz);
    }

    /**
     * 查找 static 无参、返回值为 Steamworks4k 的方法并调用。
     */
    private static Object tryExtractFromMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0 && isSteamworksClass(method.getReturnType())) {
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result != null && isSteamworksClass(result.getClass())) {
                        return result;
                    }
                } catch (Exception ignored) {
                    /* Ignore invocation errors */
                }
            }
        }
        return null;
    }

    /**
     * 查找 static、类型为 Steamworks4k 的字段并提取值。
     */
    private static Object tryExtractFromFields(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && isSteamworksClass(field.getType())) {
                Object result = tryExtractSingleField(field, null);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static Object tryExtractSingleField(Field field, Object instance) {
        try {
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value != null && isSteamworksClass(value.getClass())) {
                return value;
            }
        } catch (Exception ignored) {
            /* Ignore field access error */
        }
        return null;
    }

    /**
     * 从 Kotlin object 单例中提取 Steamworks4k 实例。
     * Kotlin object 编译为 static final INSTANCE 字段，实例持有 val 属性作为字段/getter。
     */
    private static Object tryExtractFromSingleton(Class<?> clazz) {
        try {
            Field instanceField = clazz.getDeclaredField("INSTANCE");
            if (!Modifier.isStatic(instanceField.getModifiers())
                    || !Modifier.isFinal(instanceField.getModifiers())) {
                return null;
            }

            instanceField.setAccessible(true);
            Object singleton = instanceField.get(null);
            if (singleton == null) return null;

            // 尝试单例上的 0 参实例方法（如 getSteamworks4k()）
            Object result = tryExtractFromMethodsOnSingleton(singleton);
            if (result != null) return result;

            // 尝试单例上的实例字段
            return tryExtractFromFieldsOnSingleton(singleton);
        } catch (Exception ignored) {
            /* Ignore singleton error */
        }
        return null;
    }

    private static Object tryExtractFromMethodsOnSingleton(Object singleton) {
        for (Method method : singleton.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && isSteamworksClass(method.getReturnType())) {
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(singleton);
                    if (result != null && isSteamworksClass(result.getClass())) {
                        return result;
                    }
                } catch (Exception ignored) {
                    /* Ignore error */
                }
            }
        }
        return null;
    }

    private static Object tryExtractFromFieldsOnSingleton(Object singleton) {
        for (Field field : singleton.getClass().getDeclaredFields()) {
            if (isSteamworksClass(field.getType())) {
                Object result = tryExtractSingleField(field, singleton);
                if (result != null) return result;
            }
        }
        return null;
    }

    // ==================== 反射工具方法 ====================

    /**
     * 判断某个 Class 是否为 Steamworks4k 或其子类（通过类名匹配，不依赖编译时类型）。
     */
    private static boolean isSteamworksClass(Class<?> clazz) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            if (TARGET_CLASS_NAME.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在类层次中按名称和参数数量查找方法。
     */
    private static Method findMethod(Class<?> clazz, String name) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                for (Method m : current.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == 0) {
                        return m;
                    }
                }
            } catch (Exception ignored) {
                /* Ignore error */
            }
        }
        return null;
    }

    /**
     * 在类层次中查找 setRichPresence 方法（含 Kotlin mangle 后的名称）。
     * 匹配条件：方法名以 "setRichPresence" 开头，接受两个 String 参数。
     */
    private static Method findSetRichPresenceMethod(Class<?> clazz) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                for (Method m : current.getDeclaredMethods()) {
                    if (!m.getName().startsWith("setRichPresence")) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                        return m;
                    }
                }
            } catch (Exception ignored) {
                /* Ignore error */
            }
        }
        return null;
    }

    // ==================== 日志工具 ====================

    private static void logInfo(String message) {
        System.out.println(TAG + message);
    }

    private static void logError(String message) {
        System.err.println(TAG + message);
    }
}
