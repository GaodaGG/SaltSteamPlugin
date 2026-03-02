package com.gg.SaltSteamPlugin;

import com.xuncorp.spw.workshop.api.WorkshopApi;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
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

    private static volatile Object cachedInstance = null;
    private static volatile Class<?> cachedClass = null;
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
        if (cachedInstance == null) {
            synchronized (SteamworksCatcher.class) {
                if (cachedInstance == null) {
                    cachedInstance = tryLoadFromCache();
                    if (cachedInstance == null) {
                        cachedInstance = scanForSteamworks();
                    }
                }
            }
        }
        return cachedInstance;
    }

    /**
     * 获取缓存中的 Steamworks4k Class 对象。
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
     * 通过反射调用实例上的 {@code isInitialized()} 方法。
     */
    public static boolean isInitialized(Object instance) {
        if (instance == null) return false;
        try {
            // Kotlin 属性 isInitialized 编译后可能生成 isInitialized() 或 getInitialized()
            Method method = findMethod(instance.getClass(), "isInitialized", 0);
            if (method == null) {
                method = findMethod(instance.getClass(), "getInitialized", 0);
            }
            if (method != null) {
                method.setAccessible(true);
                return Boolean.TRUE.equals(method.invoke(instance));
            }
        } catch (Throwable e) {
            logError("Failed to check isInitialized: " + e.getMessage());
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
                return result instanceof Boolean ? (Boolean) result : result != null;
            }
            logError("setRichPresence method not found");
        } catch (Throwable e) {
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
                cachedClass = result.getClass();
                logInfo("Loaded from cache successfully.");
                return result;
            }
        } catch (Throwable e) {
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
    private static File locateHostJar() throws Exception {
        Class<?> knownHostClass = Class.forName(TARGET_CLASS_NAME);
        URL location = knownHostClass.getProtectionDomain().getCodeSource().getLocation();

        if (location == null) {
            logError("Could not determine host JAR location");
            return null;
        }

        File hostJar = new File(location.toURI());
        logInfo("Found JAR: " + hostJar.getAbsolutePath());
        return hostJar;
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
                cachedClass = result.getClass();

                String msg = "Found Steamworks4k in: " + clazz.getName();
                logInfo(msg);
                WorkshopApi.ui().toast(TAG + msg, WorkshopApi.Ui.ToastType.Success);

                Config.getInstance().setCachedSteamworksInfo(clazz.getName(), currentSpwInfo);
                return result;
            }
        } catch (Throwable ignored) {
            // 忽略加载单个类时的错误
        }
        return null;
    }

    // ==================== 实例提取 ====================

    /**
     * 从指定类中尝试提取 Steamworks4k 实例：先尝试静态方法，再尝试静态字段。
     */
    private static Object tryExtractInstance(Class<?> clazz) {
        Object result = tryExtractFromMethods(clazz);
        return result != null ? result : tryExtractFromFields(clazz);
    }

    /**
     * 查找 static 无参、返回值为 Steamworks4k 的方法并调用。
     */
    private static Object tryExtractFromMethods(Class<?> clazz) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                try {
                    if (!Modifier.isStatic(method.getModifiers())) continue;
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
     * 查找 static、类型为 Steamworks4k 的字段并提取值。
     */
    private static Object tryExtractFromFields(Class<?> clazz) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    if (!Modifier.isStatic(field.getModifiers())) continue;
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
    private static Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                for (Method m : current.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                        return m;
                    }
                }
            } catch (Throwable ignored) {
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
            } catch (Throwable ignored) {
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

