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
 * 在 Spring Bean 定义阶段加载 <code>_classes</code>
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
        File classesd = RebuildConfiguration.getFileOfData(DynamicClassLoader.CLASSES_DIR);
        if (!classesd.exists() || !classesd.isDirectory()) {
            return;
        }

        DynamicClassLoader.init(classesd);

        int registered = 0;
        for (Class<?> clazz : DynamicClassLoader.getInstance().getLoadedClasses()) {
            // @Component @Controller @Service @Repository @Configuration
            if (!AnnotatedElementUtils.hasAnnotation(clazz, Component.class)) continue;

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            registry.registerBeanDefinition(clazz.getName(), builder.getBeanDefinition());
            log.info("Registered external Spring bean: {}", clazz.getName());
            registered++;
        }
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
