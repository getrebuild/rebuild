/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 动态类加载器，从 RB 数据目录下的 <code>_classes</code> 目录加载外部类。
 * <p>
 * 扫描目录下所有 <b>.class</b> 文件，通过 ASM 读取字节码中的真实类名，
 * 无需按包结构放置文件。例如 <code>_classes/MyAction.class</code> 可以对应类名
 * <code>com.example.MyAction</code>（由字节码内部声明决定）。
 * <p>
 * 使用方式：
 * <pre>
 * // 获取已加载的类
 * Class&lt;?&gt; clazz = DynamicClassLoader.getInstance().getLoadedClass("com.example.Foo");
 *
 * // 获取某接口/父类的所有实现
 * Collection&lt;Class&lt;? extends Bar&gt;&gt; classes = DynamicClassLoader.getInstance().getClassesOfType(Bar.class);
 * </pre>
 *
 * @author devezhao
 * @see DynamicClassLoadingInitializer
 * @since 2026/7/28
 */
@Slf4j
public class DynamicClassLoader extends ClassLoader {

    /** 外部类目录名（位于 RB 数据目录下） */
    public static final String CLASSES_DIR = "_classes";

    private static volatile DynamicClassLoader instance;

    // 已加载的类: className -> Class
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    private DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * 初始化类加载器并扫描目录中的所有类
     *
     * @param rootDir
     * @return
     */
    public static synchronized DynamicClassLoader init(File rootDir) {
        if (instance != null) return instance;

        instance = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
        instance.scanClasses(rootDir);
        return instance;
    }

    /**
     * 获取实例（可能为 null，如果未初始化）
     *
     * @return
     */
    public static DynamicClassLoader getInstance() {
        return instance;
    }

    /**
     * 是否已初始化
     *
     * @return
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * 扫描目录中的所有 .class 文件，通过 ASM 读取真实类名并加载
     *
     * @param rootDir
     */
    private void scanClasses(File rootDir) {
        Path rootPath = rootDir.toPath();
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> !p.getFileName().toString().startsWith("package-info")
                            && !p.getFileName().toString().startsWith("module-info"))
                    .forEach(p -> {
                        try {
                            byte[] bytes = Files.readAllBytes(p);
                            String className = extractClassName(bytes);

                            if (className == null) {
                                log.warn("Cannot extract class name from: {}", p);
                                return;
                            }

                            if (loadedClasses.containsKey(className)) {
                                log.warn("Duplicate external class (ignored): {}", className);
                                return;
                            }

                            Class<?> clazz = defineClass(className, bytes, 0, bytes.length);
                            loadedClasses.put(className, clazz);
                            log.info("Loaded external class: {}", className);
                        } catch (Throwable e) {
                            log.warn("Cannot load external class: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Cannot scan classes directory: {}", rootDir, e);
        }

        if (loadedClasses.isEmpty()) {
            log.info("No external classes loaded from: {}", rootDir);
        } else {
            log.info("Loaded {} external classes from: {}", loadedClasses.size(), rootDir);
        }
    }

    /**
     * 从 .class 字节码中提取全限定类名
     *
     * @param classBytes
     * @return e.g. "com.example.Foo"，解析失败返回 null
     */
    private String extractClassName(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        return cr.getClassName().replace('/', '.');
    }

    /**
     * 获取已加载的类
     *
     * @param className 全限定类名
     * @return 可能返回 null
     */
    public Class<?> getLoadedClass(String className) {
        return loadedClasses.get(className);
    }

    /**
     * 获取所有已加载的类
     *
     * @return
     */
    public Collection<Class<?>> getLoadedClasses() {
        return Collections.unmodifiableCollection(loadedClasses.values());
    }

    /**
     * 获取指定类型的所有子类/实现类
     *
     * @param type 父类或接口
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<Class<? extends T>> getClassesOfType(Class<T> type) {
        List<Class<? extends T>> result = new ArrayList<>();
        for (Class<?> clazz : loadedClasses.values()) {
            if (!clazz.equals(type) && !clazz.isInterface() && type.isAssignableFrom(clazz)) {
                result.add((Class<? extends T>) clazz);
            }
        }
        return result;
    }

    /**
     * 实例化指定类型的所有类（使用无参构造）
     *
     * @param type 父类或接口
     * @return
     */
    public <T> List<T> newInstancesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Class<? extends T> clazz : getClassesOfType(type)) {
            try {
                result.add(clazz.getDeclaredConstructor().newInstance());
            } catch (Throwable e) {
                log.warn("Cannot instantiate: {}", clazz.getName(), e);
            }
        }
        return result;
    }
}
