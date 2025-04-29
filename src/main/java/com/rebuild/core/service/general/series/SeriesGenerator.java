/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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

    final private static String CHECKSUM = "{X}";

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
        return generate(null);
    }

    /**
     * @param record
     * @return
     */
    public String generate(Record record) {
        String seriesFormat = config.getString("seriesFormat");
        if (StringUtils.isBlank(seriesFormat)) {
            seriesFormat = DisplayType.SERIES.getDefaultFormat();
        }

        try {
            List<SeriesVar> vars = explainVars(seriesFormat, record);
            for (SeriesVar var : vars) {
                seriesFormat = seriesFormat.replace("{" + var.getSymbols() + "}", var.generate());
            }

            if (seriesFormat.contains(CHECKSUM)) {
                seriesFormat = seriesFormat.replace(CHECKSUM, String.valueOf(mod10(seriesFormat)));
            }
            return seriesFormat;

        } catch (Exception ex) {
            throw new DefinedException(Language.L("自动编号规则无效") + "(" + seriesFormat + ")");
        }
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(@?[\\w.]+)}");

    /**
     * @param format
     * @param record
     * @return
     */
    protected List<SeriesVar> explainVars(String format, Record record) {
        List<SeriesVar> vars = new ArrayList<>();

        Matcher varMatcher = VAR_PATTERN.matcher(format);
        while (varMatcher.find()) {
            String s = varMatcher.group(1);
            if ("X".equals(s)) continue;
            if ("0".equals(s.substring(0, 1))) {
                vars.add(new IncreasingVar(s, field, config.getString("seriesZero")));
            } else if (s.startsWith(FieldVar.PREFIX)) {
                // {@FIELD}
                vars.add(new FieldVar(s, record));
            } else {
                vars.add(new TimeVar(s));
            }
        }
        return vars;
    }

    /**
     * {X} 校验位。数字位相加 mod 10
     *
     * @param series
     * @return
     */
    private int mod10(String series) {
        int sum = 0;
        for (String ch : series.split("")) {
            if (NumberUtils.isDigits(ch)) sum += NumberUtils.toByte(ch);
        }
        return sum % 10;
    }
}
