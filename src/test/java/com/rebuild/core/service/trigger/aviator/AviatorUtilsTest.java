/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.googlecode.aviator.AviatorEvaluator;
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
        System.out.println(AviatorUtils.eval("123*123"));

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
        AviatorUtils.eval("p(DATEDIFF('2021-03-04 00:00:00', '2022-03-05 23:59:59', 'D'))");
        AviatorUtils.eval("p(DATEDIFF('2021-03-04 00:00:00', '2022-03-05 23:59:59', 'M'))");
        AviatorUtils.eval("p(DATEDIFF('2021-03-04 00:00:00', '2022-03-09 23:59:59', 'Y'))");

        AviatorUtils.eval("p(DATEADD('2021-01-01 18:17:00', '2H'))");

        AviatorUtils.eval("p(DATESUB('2021-01-01 18:17:00', '1'))");
    }

    @Test
    void funcComplex() {
        AviatorUtils.eval("p(100 + DATEDIFF('2021-01-01 18:17:00', '2021-01-01 16:17:00', 'H'))");
        AviatorUtils.eval("p(DATEADD(DATEADD('2021-01-01 18:17:00', '2H'), '1D'))");
    }

    @Test
    void funcRequestFunction() {
        AviatorUtils.eval("p(REQUEST('https://www.baidu.com/'))");
        AviatorUtils.eval("p(REQUEST('https://www.google.com/', 'imdefault'))");
    }

    @Test
    void funcLocationDistanceFunction() {
        AviatorUtils.eval("p(LOCATIONDISTANCE('123.456789,123.456789', '地址$$$$123.456789,123.456789'))");
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
        AviatorUtils.eval("p(StringUtils.upperCase('abcd'));");
    }

    @Test
    void testDateOp() {
        Map<String, Object> env = AviatorEvaluator.newEnv("date1", CalendarUtils.now());

        AviatorUtils.eval("p(date1 + 8)", env, true);
        AviatorUtils.eval("p(date1 - 8)", env, true);
        AviatorUtils.eval("p(date1 - date1)", env, true);
        AviatorUtils.eval("p(1 + 1)", env, true);
        AviatorUtils.eval("p(1 - 1)", env, true);

        // BAD
        Assertions.assertThrows(ExpressionRuntimeException.class,
                () -> AviatorUtils.eval("date1 + date1", env, false));
    }

    @Test
    void testDateCompare() {
        Map<String, Object> env = AviatorEvaluator.newEnv(
                "date1", CalendarUtils.now(),
                "date2", CalendarUtils.addDay(1));

        Assertions.assertTrue((Boolean) AviatorUtils.eval("date1 == date1", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("date1 != date2", env));
        Assertions.assertFalse((Boolean) AviatorUtils.eval("date1 > date1", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("date1 >= date1", env));
        Assertions.assertFalse((Boolean) AviatorUtils.eval("date1 < date1", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("date1 <= date1", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("date1 == date1", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("date1 != date2", env));

        Assertions.assertTrue((Boolean) AviatorUtils.eval("1.0 == 1", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("1 != 2", env));
        Assertions.assertFalse((Boolean) AviatorUtils.eval("1 > 2", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("2 >= 2", env));
        Assertions.assertFalse((Boolean) AviatorUtils.eval("3.1 < 2", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("4.56 <= 4.56", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("12.34560 == 12.3456", env));
        Assertions.assertTrue((Boolean) AviatorUtils.eval("1 != 2", env));
    }

    @Test
    void intdiv() {
        System.out.println(AviatorUtils.eval("1/2.333"));
        System.out.println(AviatorUtils.eval("1/3"));
    }

    @Test
    void testArray() {
        String code = "let array = tuple(tuple(1,2,3), tuple(4,5,6)); print(array[1]); ";
        AviatorUtils.eval(code);
    }

    @Test
    void testEquals() {
        System.out.println(AviatorUtils.eval("'张三' == '张三'"));
        System.out.println(AviatorUtils.eval("1 == 1"));
        System.out.println(AviatorUtils.eval("0.0001 == 0.0001"));
    }

    @Test
    void testHanlp() throws Exception {
        System.out.println(AviatorUtils.eval("HANLPPINY('1张2三3', true)"));
        System.out.println(AviatorUtils.eval("HANLPPINY('1张2三3', false)"));
    }
}