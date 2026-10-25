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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 动态类加载器，从数据目录下 <code>_classes</code>
 *
 * @author devezhao
 * @see DynamicClassRegistrar
 * @since 2026/7/28
 */
@Slf4j
public class DynamicClassLoader extends ClassLoader {

    public static final String CLASSES_DIR = "_classes";
    public static final String PROXYTRIGGER_CLASSES = "PROXYTRIGGER_CLASSES";

    private static volatile DynamicClassLoader instance;

    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    private DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * @param rootd
     * @return
     */
    public static synchronized DynamicClassLoader init(File rootd) {
        if (instance != null) return instance;

        instance = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
        instance.scan(rootd);
        return instance;
    }

    /**
     * @return
     */
    public static DynamicClassLoader getInstance() {
        return instance;
    }

    /**
     * @param rootd
     */
    private void scan(File rootd) {
        try (Stream<Path> paths = Files.walk(rootd.toPath())) {
            paths.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try {
                            byte[] bs = Files.readAllBytes(p);
                            String name = readClassName(bs);
                            if (loadedClasses.containsKey(name)) return;

                            Class<?> clazz = defineClass(name, bs, 0, bs.length);
                            loadedClasses.put(name, clazz);
                            log.info("Loaded external class: {}", name);

                        } catch (Throwable e) {
                            log.warn("Cannot load external class: {}", p, e);
                        }
                    });

        } catch (IOException e) {
            log.error("Cannot scan classes directory: {}", rootd, e);
        }

        log.info("Loaded {} external classes from: {}", loadedClasses.size(), rootd);
    }

    /**
     * @param className
     * @return
     */
    public Class<?> getLoadedClass(String className) {
        Class<?> clazz = loadedClasses.get(className);
        if (clazz == null) clazz = loadFromProxyTriggerDir(className);
        return clazz;
    }

    /**
     * 未命中时按需从上传目录加载
     *
     * @param className
     * @return
     */
    private synchronized Class<?> loadFromProxyTriggerDir(String className) {
        Class<?> clazz = loadedClasses.get(className);
        if (clazz != null) return clazz;

        File dir = RebuildConfiguration.getFileOfData("rb/" + PROXYTRIGGER_CLASSES);
        if (!dir.exists()) return null;

        byte[] bs = null;
        File matched = null;
        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            for (Path p : (Iterable<Path>) paths.filter(x -> x.toString().endsWith(".class"))::iterator) {
                try {
                    File f = p.toFile();
                    if (matched != null && f.lastModified() <= matched.lastModified()) continue;

                    byte[] bs2 = Files.readAllBytes(p);
                    String name = readClassName(bs2);
                    if (className.equals(name)) {
                        bs = bs2;
                        matched = f;
                    }

                } catch (Throwable e) {
                    log.warn("Cannot read external class: {}", p, e);
                }
            }

        } catch (IOException e) {
            log.error("Cannot scan classes directory: {}", dir, e);
            return null;
        }

        if (matched == null) return null;

        try {
            clazz = defineClass(className, bs, 0, bs.length);
            loadedClasses.put(className, clazz);
            log.info("Loaded external class: {} from {}", className, matched);

        } catch (Throwable e) {
            log.warn("Cannot load external class: {}", matched, e);
        }
        return clazz;
    }

    /**
     * @param bs
     * @return
     */
    public static String readClassName(byte[] bs) {
        return new ClassReader(bs).getClassName().replace('/', '.');
    }

    /**
     * @return
     */
    public Collection<Class<?>> getLoadedClasses() {
        return loadedClasses.values();
    }
}
