/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 在 Spring Bean 定义阶段加载 <code>_classes</code> 目录中的外部类，
 * 并将带有 Spring 注解（@Controller @Service @Component 等）的类注册为 Bean。
 * 这样外部类可以被 Spring 正常管理，包括 Controller 路由映射、依赖注入等。
 *
 * @author devezhao
 * @see DynamicClassLoader
 * @since 2026/7/28
 */
@Slf4j
@Component
public class DynamicClassRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        File classesDir = RebuildConfiguration.getFileOfData(DynamicClassLoader.CLASSES_DIR);
        if (!classesDir.exists() || !classesDir.isDirectory()) {
            log.info("No external classes directory found: {}", classesDir);
            return;
        }

        log.info("Scanning external classes from: {}", classesDir);
        DynamicClassLoader.init(classesDir);

        int registered = 0;
        for (Class<?> clazz : DynamicClassLoader.getInstance().getLoadedClasses()) {
            // @Component 是 @Controller @Service @Repository @Configuration 的元注解
            if (!AnnotatedElementUtils.hasAnnotation(clazz, Component.class)) continue;

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            registry.registerBeanDefinition(clazz.getName(), builder.getBeanDefinition());
            log.info("Registered external Spring bean: {}", clazz.getName());
            registered++;
        }

        log.info("Registered {} external Spring beans", registered);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Nothing
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
