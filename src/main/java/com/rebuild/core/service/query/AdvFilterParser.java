/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.util.Assert;

import java.util.*;

import static cn.devezhao.commons.CalendarUtils.addDay;
import static cn.devezhao.commons.CalendarUtils.addMonth;
import static cn.devezhao.commons.DateFormatUtils.getUTCDateFormat;

/**
 * 高级查询解析器
 * <pre>
 * {
 *     [entity]: 'xxx',
 *     [type]: 'xxx',
 *     [equation]: 'xxx',
 *     items: [
 *       { field:'xxx', op: 'xxx', value: 'xxx' },
 *       ...
 *     ],
 *     [values]: [
 *       'xxx', ...
 *     ]
 * }
 * </pre>
 *
 * @author devezhao
 * @since 09/29/2018
 */
@Slf4j
public class AdvFilterParser extends SetUser {

    private JSONObject filterExp;
    private Entity rootEntity;

    private Set<String> includeFields = null;

    /**
     * @param filterExp
     */
    public AdvFilterParser(JSONObject filterExp) {
        this(MetadataHelper.getEntity(filterExp.getString("entity")), filterExp);
    }

    /**
     * @param rootEntity
     * @param filterExp
     */
    public AdvFilterParser(Entity rootEntity, JSONObject filterExp) {
        this.rootEntity = rootEntity;
        this.filterExp = filterExp;
    }

    /**
     * @return
     */
    public String toSqlWhere() {
        if (filterExp == null || filterExp.isEmpty()) {
            return null;
        }

        this.includeFields = new HashSet<>();

        // 快速搜索模式，自动确定查询项
        if ("QUICK".equalsIgnoreCase(filterExp.getString("type"))) {
            JSONArray quickItems = buildQuickFilterItems(filterExp.getString("quickFields"));
            this.filterExp.put("items", quickItems);
        }

        JSONArray items = filterExp.getJSONArray("items");
        items = items == null ? JSONUtils.EMPTY_ARRAY : items;

        JSONObject values = filterExp.getJSONObject("values");
        values = values == null ? JSONUtils.EMPTY_OBJECT : values;

        String equation = StringUtils.defaultIfBlank(filterExp.getString("equation"), "OR");

        Map<Integer, String> indexItemSqls = new LinkedHashMap<>();
        int incrIndex = 1;
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            Integer index = item.getInteger("index");
            if (index == null) {
                index = incrIndex++;
            }

            String itemSql = parseItem(item, values);
            if (itemSql != null) {
                indexItemSqls.put(index, itemSql.trim());
                this.includeFields.add(item.getString("field"));
            }
        }
        if (indexItemSqls.isEmpty()) {
            return null;
        }

        String equationHold = equation;
        if ((equation = validEquation(equation)) == null) {
            throw new FilterParseException(Language.L("无效的高级表达式 : %s", equationHold));
        }

        if ("OR".equalsIgnoreCase(equation)) {
            return "( " + StringUtils.join(indexItemSqls.values(), " or ") + " )";
        } else if ("AND".equalsIgnoreCase(equation)) {
            return "( " + StringUtils.join(indexItemSqls.values(), " and ") + " )";
        } else {
            // 高级表达式 eg. (1 AND 2) or (3 AND 4)
            String[] tokens = equation.toLowerCase().split(" ");
            List<String> itemSqls = new ArrayList<>();
            for (String token : tokens) {
                if (StringUtils.isBlank(token)) {
                    continue;
                }

                boolean hasRP = false;  // the `)`
                if (token.length() > 1) {
                    if (token.startsWith("(")) {
                        itemSqls.add("(");
                        token = token.substring(1);
                    } else if (token.endsWith(")")) {
                        hasRP = true;
                        token = token.substring(0, token.length() - 1);
                    }
                }

                if (NumberUtils.isDigits(token)) {
                    String itemSql = StringUtils.defaultIfBlank(indexItemSqls.get(Integer.valueOf(token)), "(9=9)");
                    itemSqls.add(itemSql);
                } else if ("(".equals(token) || ")".equals(token) || "or".equals(token) || "and".equals(token)) {
                    itemSqls.add(token);
                } else {
                    log.warn("Invalid equation token : " + token);
                }

                if (hasRP) {
                    itemSqls.add(")");
                }
            }
            return "( " + StringUtils.join(itemSqls, " ") + " )";
        }
    }

    /**
     * 过滤器中包含的字段。必须先执行 toSqlWhere 方法
     *
     * @return
     */
    public Set<String> getIncludeFields() {
        Assert.notNull(includeFields, "Calls #toSqlWhere first");
        return includeFields;
    }

    /**
     * 解析查询项为 SQL
     *
     * @param item
     * @param values
     * @return
     */
    private String parseItem(JSONObject item, JSONObject values) {
        String field = item.getString("field");
        boolean hasNameFlag = field.startsWith("&");
        if (hasNameFlag) {
            field = field.substring(1);
        }

        Field fieldMeta = MetadataHelper.getLastJoinField(rootEntity, field);
        if (fieldMeta == null) {
            log.warn("Unknown field '" + field + "' in '" + rootEntity.getName() + "'");
            return null;
        }

        DisplayType dt = EasyMetaFactory.getDisplayType(fieldMeta);
        if (dt == DisplayType.CLASSIFICATION
                || (dt == DisplayType.PICKLIST && hasNameFlag) /* 快速查询 */) {
            field = "&" + field;
        } else if (hasNameFlag) {
            if (dt != DisplayType.REFERENCE) {
                log.warn("Non reference-field '" + field + "' in '" + rootEntity.getName() + "'");
                return null;
            }

            // 转化为名称字段
            fieldMeta = fieldMeta.getReferenceEntity().getNameField();
            dt = EasyMetaFactory.getDisplayType(fieldMeta);
            field += "." + fieldMeta.getName();
        }

        String op = item.getString("op");
        String value = item.getString("value");
        String valueEnd = null;

        // 根据字段类型转换 `op`

        // 日期时间
        if (dt == DisplayType.DATETIME || dt == DisplayType.DATE) {
            if (ParseHelper.TDA.equalsIgnoreCase(op) || ParseHelper.YTA.equalsIgnoreCase(op)
                    || ParseHelper.TTA.equalsIgnoreCase(op)) {
                value = getUTCDateFormat().format(CalendarUtils.now());
                if (ParseHelper.YTA.equalsIgnoreCase(op)) {
                    value = getUTCDateFormat().format(addDay(-1));
                } else if (ParseHelper.TTA.equalsIgnoreCase(op)) {
                    value = getUTCDateFormat().format(addDay(1));
                }

                if (dt == DisplayType.DATETIME) {
                    op = ParseHelper.BW;
                    valueEnd = parseValue(value, op, fieldMeta, true);
                }

            } else if (ParseHelper.CUW.equalsIgnoreCase(op) || ParseHelper.CUM.equalsIgnoreCase(op)
                    || ParseHelper.CUQ.equalsIgnoreCase(op) || ParseHelper.CUY.equalsIgnoreCase(op)) {
                Date begin = Moment.moment().startOf(op.substring(2)).date();
                value = CalendarUtils.getUTCDateFormat().format(begin);

                Date end = Moment.moment(begin).endOf(op.substring(2)).date();
                valueEnd = CalendarUtils.getUTCDateFormat().format(end);

                if (dt == DisplayType.DATETIME) {
                    value += ParseHelper.ZERO_TIME;
                    valueEnd += ParseHelper.FULL_TIME;
                }
                op = ParseHelper.BW;

            } else if (ParseHelper.EQ.equalsIgnoreCase(op)
                    && dt == DisplayType.DATETIME && StringUtils.length(value) == 10) {
                op = ParseHelper.BW;
                valueEnd = parseValue(value, op, fieldMeta, true);
            }

        } else if (dt == DisplayType.MULTISELECT) {
            // 多选的包含/不包含要按位计算
            if (ParseHelper.IN.equalsIgnoreCase(op) || ParseHelper.NIN.equalsIgnoreCase(op)) {
                op = ParseHelper.IN.equalsIgnoreCase(op) ? ParseHelper.BAND : ParseHelper.NBAND;

                long maskValue = 0;
                for (String s : value.split("\\|")) {
                    maskValue += ObjectUtils.toLong(s);
                }
                value = maskValue + "";
            }
        }

        StringBuilder sb = new StringBuilder(field)
                .append(' ')
                .append(ParseHelper.convetOperation(op));
        // 无需值
        if (ParseHelper.NL.equalsIgnoreCase(op) || ParseHelper.NT.equalsIgnoreCase(op)) {
            return sb.toString();
        }

        sb.append(' ');

        // 自定义函数

        if (ParseHelper.BFD.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addDay(-NumberUtils.toInt(value))) + ParseHelper.FULL_TIME;
        } else if (ParseHelper.BFM.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addMonth(-NumberUtils.toInt(value))) + ParseHelper.FULL_TIME;
        } else if (ParseHelper.BFY.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addMonth(-NumberUtils.toInt(value) * 12)) + ParseHelper.FULL_TIME;
        } else if (ParseHelper.AFD.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addDay(NumberUtils.toInt(value))) + ParseHelper.ZERO_TIME;
        } else if (ParseHelper.AFM.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addMonth(NumberUtils.toInt(value))) + ParseHelper.ZERO_TIME;
        } else if (ParseHelper.AFY.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addMonth(NumberUtils.toInt(value) * 12)) + ParseHelper.ZERO_TIME;
        } else if (ParseHelper.RED.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addDay(-NumberUtils.toInt(value))) + ParseHelper.FULL_TIME;
        } else if (ParseHelper.REM.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addMonth(-NumberUtils.toInt(value))) + ParseHelper.FULL_TIME;
        } else if (ParseHelper.REY.equalsIgnoreCase(op)) {
            value = getUTCDateFormat().format(addMonth(-NumberUtils.toInt(value) * 12)) + ParseHelper.FULL_TIME;
        } else if (ParseHelper.SFU.equalsIgnoreCase(op)) {
            value = getUser().toLiteral();
        } else if (ParseHelper.SFB.equalsIgnoreCase(op)) {
            Department dept = UserHelper.getDepartment(getUser());
            if (dept != null) {
                value = dept.getIdentity().toString();
                int ref = fieldMeta.getReferenceEntity().getEntityCode();
                if (ref == EntityHelper.User) {
                    sb.insert(sb.indexOf(" "), ".deptId");
                } else if (ref == EntityHelper.Department) {
                    // NOOP
                } else {
                    value = null;
                }
            }
        } else if (ParseHelper.SFD.equalsIgnoreCase(op)) {
            Department dept = UserHelper.getDepartment(getUser());
            if (dept != null) {
                int refe = fieldMeta.getReferenceEntity().getEntityCode();
                if (refe == EntityHelper.Department) {
                    value = StringUtils.join(UserHelper.getAllChildren(dept), "|");
                }
            }
        } else if (ParseHelper.SFT.equalsIgnoreCase(op)) {
            // In Sql
            value = String.format(
                    "( select userId from TeamMember where teamId in ('%s') )",
                    StringUtils.join(value.split("\\|"), "', '"));
        }

        if (StringUtils.isBlank(value)) {
            log.warn("No search value defined : " + item.toJSONString());
            return null;
        }

        // 快速搜索的占位符 `{1}`
        if (value.matches("\\{\\d+}")) {
            if (values == null || values.isEmpty()) {
                return null;
            }

            String valHold = value.replaceAll("[{}]", "");
            value = parseValue(values.get(valHold), op, fieldMeta, false);
        } else {
            value = parseValue(value, op, fieldMeta, false);
        }

        // No value for search
        if (value == null) {
            return null;
        }

        // 区间
        final boolean isBetween = op.equalsIgnoreCase(ParseHelper.BW);
        if (isBetween && valueEnd == null) {
            valueEnd = parseValue(item.getString("value2"), op, fieldMeta, true);
            if (valueEnd == null) {
                valueEnd = value;
            }
        }

        // IN
        if (op.equalsIgnoreCase(ParseHelper.IN) || op.equalsIgnoreCase(ParseHelper.NIN)
                || op.equalsIgnoreCase(ParseHelper.SFD) || op.equalsIgnoreCase(ParseHelper.SFT)) {
            sb.append(value);
        } else {
            // LIKE
            if (op.equalsIgnoreCase(ParseHelper.LK) || op.equalsIgnoreCase(ParseHelper.NLK)) {
                value = '%' + value + '%';
            }
            sb.append(quoteValue(value, fieldMeta.getType()));
        }

        if (isBetween) {
            sb.insert(0, "( ")
                    .append(" and ").append(quoteValue(valueEnd, fieldMeta.getType()))
                    .append(" )");
        }

        return sb.toString();
    }

    /**
     * @param val
     * @param op
     * @param field
     * @param endVal 仅对日期时间有意义
     * @return
     */
    private String parseValue(Object val, String op, Field field, boolean endVal) {
        String value;
        // IN
        if (val instanceof JSONArray) {
            Set<String> inVals = new HashSet<>();
            for (Object v : (JSONArray) val) {
                inVals.add(quoteValue(v.toString(), field.getType()));
            }
            return optimizeIn(inVals);

        } else {
            value = val.toString();
            if (StringUtils.isBlank(value)) {
                return null;
            }

            // TIMESTAMP 仅指定了日期值，则补充时间值
            if (field.getType() == FieldType.TIMESTAMP && StringUtils.length(value) == 10) {
                if (ParseHelper.GT.equalsIgnoreCase(op)) {
                    value += ParseHelper.FULL_TIME;  // 不含当日
                } else if (ParseHelper.LT.equalsIgnoreCase(op)) {
                    value += ParseHelper.ZERO_TIME;  // 不含当日
                } else if (ParseHelper.GE.equalsIgnoreCase(op)) {
                    value += ParseHelper.ZERO_TIME;  // 含当日
                } else if (ParseHelper.LE.equalsIgnoreCase(op)) {
                    value += ParseHelper.FULL_TIME;  // 含当日
                } else if (ParseHelper.BW.equalsIgnoreCase(op)) {
                    value += (endVal ? ParseHelper.FULL_TIME : ParseHelper.ZERO_TIME);  // 含当日
                }
            }

            // 多个值的情况下，兼容 | 号分割
            if (op.equalsIgnoreCase(ParseHelper.IN) || op.equalsIgnoreCase(ParseHelper.NIN)
                    || op.equalsIgnoreCase(ParseHelper.SFD)) {
                Set<String> inVals = new HashSet<>();
                for (String v : value.split("\\|")) {
                    inVals.add(quoteValue(v, field.getType()));
                }
                return optimizeIn(inVals);
            }
        }
        return value;
    }

    /**
     * @param val
     * @param type
     * @return
     */
    private String quoteValue(String val, Type type) {
        if (NumberUtils.isNumber(val) && isNumberType(type)) {
            return val;
        } else if (StringUtils.isNotBlank(val)) {
            return String.format("'%s'", StringEscapeUtils.escapeSql(val));
        }
        return "''";
    }

    /**
     * @param inVals
     * @return
     */
    private String optimizeIn(Set<String> inVals) {
        if (inVals == null || inVals.isEmpty()) {
            return null;
        }
        return "( " + StringUtils.join(inVals, ",") + " )";
    }

    /**
     * @param type
     * @return
     */
    private boolean isNumberType(Type type) {
        return type == FieldType.INT || type == FieldType.SMALL_INT || type == FieldType.LONG
                || type == FieldType.DOUBLE || type == FieldType.DECIMAL;
    }

    /**
     * 快速查询
     *
     * @param quickFields
     * @return
     */
    private JSONArray buildQuickFilterItems(String quickFields) {
        Set<String> usesFields = ParseHelper.buildQuickFields(rootEntity, quickFields);

        JSONArray items = new JSONArray();
        for (String field : usesFields) {
            items.add(JSON.parseObject("{ op:'LK', value:'{1}', field:'" + field + "' }"));
        }
        return items;
    }

    /**
     * 测试高级表达式
     *
     * @param equation
     * @return null 表示无效
     */
    public static String validEquation(String equation) {
        if (StringUtils.isBlank(equation)) {
            return "OR";
        }
        if ("OR".equalsIgnoreCase(equation) || "AND".equalsIgnoreCase(equation)) {
            return equation;
        }

        String clearEquation = equation.toUpperCase()
                .replace("OR", " OR ")
                .replace("AND", " AND ")
                .replaceAll("\\s+", " ")
                .trim();
        equation = clearEquation;

        if (clearEquation.startsWith("AND") || clearEquation.startsWith("OR")
                || clearEquation.endsWith("AND") || clearEquation.endsWith("OR")) {
            return null;
        }
        if (clearEquation.contains("()") || clearEquation.contains("( )")) {
            return null;
        }

        for (String token : clearEquation.split(" ")) {
            token = token.replace("(", "");
            token = token.replace(")", "");

            // 数字不能大于 10
            if (NumberUtils.isNumber(token)) {
                if (NumberUtils.toInt(token) > 10) {
                    return null;
                } else {
                    // 允许
                }
            } else if ("AND".equals(token) || "OR".equals(token) || "(".equals(token) || ")".equals(token)) {
                // 允许
            } else {
                return null;
            }
        }

        // 去除 AND OR 0-9 及空格
        // noinspection RegExpDuplicateCharacterInClass
        clearEquation = clearEquation.replaceAll("[AND|OR|0-9|\\s]", "");
        // 括弧成对出现
        for (int i = 0; i < 20; i++) {
            clearEquation = clearEquation.replace("()", "");
            if (clearEquation.length() == 0) {
                return equation;
            }
        }
        return null;
    }
}
