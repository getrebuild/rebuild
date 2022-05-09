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

import java.util.*;

/**
 * @author devezhao
 * @since 12/24/2018
 */
public class SeriesGeneratorTest extends TestSupport {

    @Test
    public void testTimeVar() {
        String r = new TimeVar("YYMMDD").generate();
        System.out.println(r);
    }

    @Test
    public void testIncrementVar() {
        IncreasingVar var = new IncreasingVar("0000", getSeriesField(), null);
        System.out.println(var.generate());
        System.out.println(var.generate());
        System.out.println(var.generate());
    }

    @Test
    public void testIncrementVarNThreads() {
        final IncreasingVar var = new IncreasingVar("0000", getSeriesField(), "Y");
        final Set<String> set = Collections.synchronizedSet(new HashSet<>());
        final int N = 100;
        for (int i = 0; i < N; i++) {
            ThreadPool.exec(() -> {
                String s = var.generate();
                set.add(s);
                System.out.print(s + " ");
            });
        }
        ThreadPool.waitFor(2000);
        Assertions.assertEquals(set.size(), N);
    }

    @Test
    public void testGenerate() {
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
