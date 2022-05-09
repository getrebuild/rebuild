/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

/**
 * @see com.rebuild.web.RebuildWebConfigurer
 */
@Slf4j
@Component("thymeleafViewResolver")
public class MockThymeleafViewResolver extends ThymeleafViewResolver {

    public MockThymeleafViewResolver() {
        super();
        log.warn("Mock `thymeleafViewResolver` has been enabled");
    }
}
