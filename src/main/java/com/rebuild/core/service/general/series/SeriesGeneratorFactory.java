/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

/**
 * 自动编号工厂类
 *
 * @author devezhao
 * @since 12/24/2018
 */
public class SeriesGeneratorFactory {

    /**
     * @param field
     * @return
     */
    public static SeriesGenerator create(Field field) {
        return new SeriesGenerator(EasyMetaFactory.valueOf(field));
    }

    /**
     * 生成
     *
     * @param field
     * @return
     */
    public static String generate(Field field) {
        return generate(field, null);
    }

    /**
     * 生成
     *
     * @param field
     * @param record
     * @return
     */
    public static String generate(Field field, Record record) {
        return create(field).generate(record);
    }

    /**
     * 预览
     *
     * @param config
     * @return
     */
    public static String preview(JSONObject config) {
        return new SeriesGenerator(null, config).generate();
    }

    /**
     * 重置序号
     *
     * @param field
     */
    public static void zero(Field field) {
        new IncreasingVar(field).clean();
    }
}
