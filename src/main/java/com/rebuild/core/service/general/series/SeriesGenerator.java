/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.DisplayType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动编号
 *
 * @author devezhao
 * @since 12/24/2018
 */
public class SeriesGenerator {

    final private Field field;
    final private JSONObject config;

    /**
     * @param field
     */
    public SeriesGenerator(EasyField field) {
        this.field = field.getRawMeta();
        this.config = field.getExtraAttrs(true);
    }

    /**
     * @param field
     * @param config
     */
    public SeriesGenerator(Field field, JSONObject config) {
        this.field = field;
        this.config = config;
    }

    /**
     * @return
     */
    public String generate() {
        String seriesFormat = config.getString("seriesFormat");
        if (StringUtils.isBlank(seriesFormat)) {
            seriesFormat = DisplayType.SERIES.getDefaultFormat();
        }

        List<SeriesVar> vars = explainVars(seriesFormat);
        for (SeriesVar var : vars) {
            seriesFormat = seriesFormat.replace("{" + var.getSymbols() + "}", var.generate());
        }
        return seriesFormat;
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");

    /**
     * @param format
     * @return
     */
    protected List<SeriesVar> explainVars(String format) {
        List<SeriesVar> vars = new ArrayList<>();

        Matcher varMatcher = VAR_PATTERN.matcher(format);
        while (varMatcher.find()) {
            String s = varMatcher.group(1);
            if ("0".equals(s.substring(0, 1))) {
                vars.add(new IncreasingVar(s, field, config.getString("seriesZero")));
            } else {
                vars.add(new TimeVar(s));
            }
        }
        return vars;
    }
}
