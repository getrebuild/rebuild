/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 12/24/2018
 */
class SeriesGeneratorTest extends TestSupport {

    @Test
    void testTimeVar() {
        for (int i = 0; i < 100; i++) {
            String r = new TimeVar("YYMMDD,SSS,ww,WW").generate();
            System.out.println(r);
        }
    }

    @Test
    void testIncrementVar() {
        IncreasingVar var = new IncreasingVar("0000", getSeriesField(), null);
        System.out.println(var.generate());
        System.out.println(var.generate());
        System.out.println(var.generate());
    }

    @Test
    void testIncrementVarNThreads() {
        final IncreasingVar var = new IncreasingVar("0000", getSeriesField(), "Y");
        final Set<String> set = Collections.synchronizedSet(new HashSet<>());
        final int N = 1000;
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
                String s = var.generate();
                set.add(s);
                System.out.println(s + " << " + Thread.currentThread().getName());
            }).start();
        }
        ThreadPool.waitFor(1200);
        Assertions.assertEquals(set.size(), N);
    }

    @Test
    void testGenerate() {
        Map<String, String> config = new HashMap<>();
        config.put("seriesFormat", "Y-{YYYYMMDD}-{0000}");
        config.put("seriesZero", "M");

        SeriesGenerator generator = new SeriesGenerator(getSeriesField(), (JSONObject) JSON.toJSON(config));
        System.out.println(generator.generate());
        System.out.println(generator.generate());
        System.out.println(generator.generate());
    }

    /**
     * @return
     * @see DisplayType#SERIES
     */
    private Field getSeriesField() {
        return MetadataHelper.getField(TestAllFields, "series");
    }
}
