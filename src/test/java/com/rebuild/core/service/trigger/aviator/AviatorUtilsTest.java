/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2021/04/12
 */
class AviatorUtilsTest {

    @Test
    void eval() {
        System.out.println(AviatorUtils.evalQuietly("123*123"));

        System.out.println(AviatorUtils.eval(
                "abc12_.abc+123", Collections.singletonMap("abc12_.abc", 100), true));
    }

    @Test
    void validate() {
        System.out.println(AviatorUtils.validate("1 + The bad syntax"));
        System.out.println(AviatorUtils.validate("'2021-01-01 16:17:00' + 1"));
    }

    @Test
    void func() {
        Object result = AviatorUtils.evalQuietly("DATEDIFF('2021-01-01 18:17:00', '2021-01-01 16:17:00', 'H')");
        System.out.println(result);

        result = AviatorUtils.evalQuietly("DATEADD('2021-01-01 18:17:00', '2H')");
        System.out.println(result);

        result = AviatorUtils.evalQuietly("DATESUB('2021-01-01 18:17:00', '1')");
        System.out.println(result);
    }

    @Test
    void funcComplex() {
        Object result = AviatorUtils.evalQuietly("100 + DATEDIFF('2021-01-01 18:17:00', '2021-01-01 16:17:00', 'H')");
        System.out.println(result);

        result = AviatorUtils.evalQuietly("DATEADD(DATEADD('2021-01-01 18:17:00', '2H'), '1D')");
        System.out.println(result);
    }
}