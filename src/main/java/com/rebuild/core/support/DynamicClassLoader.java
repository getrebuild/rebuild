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
 * 动态类加载器，从数据目录下 <code>_classes</code> 加载外部 <b>.class</b> 文件。
 * 通过 ASM 读取字节码中的真实类名，无需按包结构放置文件。
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

    public static synchronized DynamicClassLoader init(File rootDir) {
        if (instance != null) return instance;

        instance = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
        instance.scan(rootDir);
        return instance;
    }

    public static DynamicClassLoader getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private void scan(File rootDir) {
        try (Stream<Path> paths = Files.walk(rootDir.toPath())) {
            paths.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try {
                            byte[] bytes = Files.readAllBytes(p);
                            String name = new ClassReader(bytes).getClassName().replace('/', '.');

                            if (loadedClasses.containsKey(name)) return;

                            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
                            loadedClasses.put(name, clazz);
                            log.info("Loaded external class: {}", name);
                        } catch (Throwable e) {
                            log.warn("Cannot load external class: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Cannot scan classes directory: {}", rootDir, e);
        }

        log.info("Loaded {} external classes from: {}", loadedClasses.size(), rootDir);
    }

    public Class<?> getLoadedClass(String className) {
        return loadedClasses.get(className);
    }

    public Collection<Class<?>> getLoadedClasses() {
        return loadedClasses.values();
    }
}
