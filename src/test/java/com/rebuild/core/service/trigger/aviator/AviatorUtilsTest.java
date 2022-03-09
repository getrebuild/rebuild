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
        AviatorUtils.evalQuietly("p(DATEDIFF('2022-03-03 17:31:24', '2022-03-04 17:30:24', 'D'))");

        AviatorUtils.evalQuietly("p(DATEADD('2021-01-01 18:17:00', '2H'))");

        AviatorUtils.evalQuietly("p(DATESUB('2021-01-01 18:17:00', '1'))");
    }

    @Test
    void funcComplex() {
        AviatorUtils.evalQuietly("p(100 + DATEDIFF('2021-01-01 18:17:00', '2021-01-01 16:17:00', 'H'))");
        AviatorUtils.evalQuietly("p(DATEADD(DATEADD('2021-01-01 18:17:00', '2H'), '1D'))");
    }

    @Test
    void funcRequestFunction() {
        AviatorUtils.evalQuietly("p(REQUEST('https://www.baidu.com/'))");
        AviatorUtils.evalQuietly("p(REQUEST('https://www.google.com/', 'imdefault'))");
    }

    @Test
    void funcLocationDistanceFunction() {
        AviatorUtils.evalQuietly("p(LOCATIONDISTANCE('123.456789,123.456789', '地址$$$$123.456789,123.456789'))");
    }
}