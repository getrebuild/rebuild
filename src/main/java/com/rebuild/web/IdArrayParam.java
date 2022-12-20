/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author devezhao
 * @since 2022/12/20
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdArrayParam {

    /**
     * Alias for {@link #name}.
     */
    @AliasFor("name")
    String value() default "ids";

    /**
     * 参数名
     *
     * @return
     */
    @AliasFor("value")
    String name() default "ids";

    /**
     * 是否必须
     *
     * @return
     */
    boolean required() default true;

}
