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
                            String name = new ClassReader(bs).getClassName().replace('/', '.');
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
        return loadedClasses.get(className);
    }

    /**
     * @return
     */
    public Collection<Class<?>> getLoadedClasses() {
        return loadedClasses.values();
    }
}
