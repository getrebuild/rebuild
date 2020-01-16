/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2020 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 归集计算
 *
 * @author devezhao
 * @since 2020/1/16
 */
public class FormulaEvaluator {

    private static final Log LOG = LogFactory.getLog(FieldAggregation.class);

    private static final Pattern FIELD_PATT = Pattern.compile("\\{(.*?)}");

    private static AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.newInstance();
    static {
        // 强制使用 BigDecimal/BigInteger 运算
        AVIATOR.setOption(Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL, true);
    }

    final private Entity sourceEntity;
    final private JSONObject item;
    final private String followSourceField;
    final private String filterSql;

    /**
     * @param item
     * @param sourceEntity
     * @param followSourceField
     * @param filterSql
     */
    protected FormulaEvaluator(JSONObject item, Entity sourceEntity, String followSourceField, String filterSql) {
        this.sourceEntity = sourceEntity;
        this.item = item;
        this.followSourceField = followSourceField;
        this.filterSql = filterSql;
    }

    /**
     * @param triggerRecord
     * @return
     */
    public Object eval(ID triggerRecord) {
        String calcMode = item.getString("calcMode");
        if ("FORMULA".equalsIgnoreCase(calcMode)) {
            return evalFormula(triggerRecord);
        }

        String sourceField = item.getString("sourceField");
        if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
            return null;
        }

        boolean direct = calcMode.equalsIgnoreCase("DIRECT");
        String funcAndField = direct ? sourceField : String.format("%s(%s)", calcMode, sourceField);
        String sql = String.format("select %s from %s where %s = ?", funcAndField, sourceEntity.getName(), followSourceField);
        if (filterSql != null) {
            sql += " and " + filterSql;
        }
        // 最近一条
        if (direct) {
            sql += " order by " + (sourceEntity.containsField(EntityHelper.ModifiedOn) ? EntityHelper.ModifiedOn : EntityHelper.CreatedOn) + " desc";
        }

        Object[] o = Application.createQueryNoFilter(sql).setParameter(1, triggerRecord).unique();
        return o == null || o[0] == null ? 0 : o[0];
    }

    /**
     * @param triggerRecord
     * @return
     */
    private Object evalFormula(ID triggerRecord) {
        String formula = item.getString("sourceFormula");
        Matcher m = FIELD_PATT.matcher(formula);

        Set<String> fields = new HashSet<>();
        while (m.find()) {
            String fieldName = m.group(1);
            if (MetadataHelper.getLastJoinField(sourceEntity, fieldName) != null) {
                fields.add(fieldName);
            }
        }
        if (fields.isEmpty()) {
            return null;
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(fields.iterator(), ","), sourceEntity.getName(), followSourceField);
        if (filterSql != null) {
            sql += " and " + filterSql;
        }

        Record o = Application.createQueryNoFilter(sql).setParameter(1, triggerRecord).record();
        if (o == null) {
            return null;
        }

        String newFormual = formula.toUpperCase()
                .replace("×", "*")
                .replace("÷", "/");
        for (String field : fields) {
            Object v = o.getObjectValue(field);
            newFormual = newFormual.replace("{" + field.toUpperCase() + "}", v == null ? "0" : v.toString());
        }

        try {
            return AVIATOR.execute(newFormual);
        } catch (ExpressionSyntaxErrorException ex) {
            LOG.error("Bad formula : " + formula + " > " + newFormual, ex);
            return null;
        }
    }
}
