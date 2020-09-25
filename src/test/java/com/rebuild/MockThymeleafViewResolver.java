/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

/**
 * @see com.rebuild.web.RebuildWebConfigurer
 */
@Component("thymeleafViewResolver")
public class MockThymeleafViewResolver extends ThymeleafViewResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MockThymeleafViewResolver.class);

    public MockThymeleafViewResolver() {
        super();
        LOG.warn("Mock `thymeleafViewResolver` has been enabled");
    }
}
