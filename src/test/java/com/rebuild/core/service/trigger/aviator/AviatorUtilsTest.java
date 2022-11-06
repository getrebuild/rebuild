/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.exception.ExpressionRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

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
        AviatorUtils.evalQuietly("p(DATEDIFF('2021-03-04 00:00:00', '2022-03-05 23:59:59', 'D'))");
        AviatorUtils.evalQuietly("p(DATEDIFF('2021-03-04 00:00:00', '2022-03-05 23:59:59', 'M'))");
        AviatorUtils.evalQuietly("p(DATEDIFF('2021-03-04 00:00:00', '2022-03-09 23:59:59', 'Y'))");

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

    @Test
    void testNull() {
        Map<String, Object> envMap = Collections.singletonMap("num", 4);
        AviatorUtils.eval("p(2.5*num)", envMap, true);

        envMap = Collections.singletonMap("num", null);
        Map<String, Object> finalEnvMap = envMap;
        Assertions.assertThrows(ExpressionRuntimeException.class,
                () -> AviatorUtils.eval("p(2*num)", finalEnvMap, false));
    }

    @Test
    void testJava() {
        AviatorUtils.evalQuietly("p(StringUtils.upperCase('abcd'));");
    }
}