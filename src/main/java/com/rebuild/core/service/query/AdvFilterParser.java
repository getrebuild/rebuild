/*!
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
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import cn.devezhao.persist4j.query.compiler.QueryCompiler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.License;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.devezhao.commons.CalendarUtils.addDay;
import static cn.devezhao.commons.CalendarUtils.addMonth;

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

    // 虚拟字段:当前审批人
    @Deprecated
    public static final String VF_ACU = "$APPROVALCURRENTUSER$";

    // 快速查询
    private static final String MODE_QUICK = "QUICK";

    // 名称字段 &
    private static final String NAME_FIELD_PREFIX = "" + QueryCompiler.NAME_FIELD_PREFIX;

    final private JSONObject filterExpr;
    final private Entity rootEntity;
    // v3.1 条件值使用记录作为变量
    final private ID varRecord;

    transient private Set<String> includeFields = null;

    /**
     * @param filterExpr
     */
    public AdvFilterParser(JSONObject filterExpr) {
        this(filterExpr, MetadataHelper.getEntity(filterExpr.getString("entity")));
    }

    /**
     * @param filterExpr
     * @param rootEntity
     */
    public AdvFilterParser(JSONObject filterExpr, Entity rootEntity) {
        Assert.notNull(filterExpr, "[filterExpr] cannot be null");
        this.filterExpr = filterExpr;
        this.rootEntity = rootEntity;
        this.varRecord = null;

        String entityName = filterExpr.getString("entity");
        if (entityName != null && !entityName.equalsIgnoreCase(this.rootEntity.getName())) {
            Assert.isTrue(entityName.equalsIgnoreCase(this.rootEntity.getName()),
                    "Filter(2) uses different entities : " + entityName + ", " + this.rootEntity.getName());
        }
    }

    /**
     * @param filterExpr
     * @param varRecord 条件中包含字段变量，将从该记录中提取实际值替换
     */
    public AdvFilterParser(JSONObject filterExpr, ID varRecord) {
        this.filterExpr = filterExpr;
        this.rootEntity = MetadataHelper.getEntity(varRecord.getEntityCode());
        this.varRecord = License.isRbvAttached() ? varRecord : null;

        String entityName = filterExpr.getString("entity");
        if (entityName != null) {
            Assert.isTrue(entityName.equalsIgnoreCase(this.rootEntity.getName()),
                    "Filter(3) uses different entities : " + entityName + ", " + this.rootEntity.getName() + ", " + varRecord);
        }
    }

    /**
     * @return
     */
    public String toSqlWhere() {
        if (filterExpr == null || filterExpr.isEmpty()) return null;

        this.includeFields = new HashSet<>();

        // 自动确定查询项
        if (MODE_QUICK.equalsIgnoreCase(filterExpr.getString("type"))) {
            rebuildQuickFilter38();
        }

        JSONArray items = filterExpr.getJSONArray("items");
        items = items == null ? JSONUtils.EMPTY_ARRAY : items;

        JSONObject values = filterExpr.getJSONObject("values");
        values = values == null ? JSONUtils.EMPTY_OBJECT : values;

        String equation = StringUtils.defaultIfBlank(filterExpr.getString("equation"), "OR");

        Map<Integer, String> indexItemSqls = new LinkedHashMap<>();
        int incrIndex = 1;
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            Integer index = item.getInteger("index");
            if (index == null) {
                index = incrIndex++;
            }

            String itemSql = parseItem(item, values, rootEntity);
            if (itemSql != null) {
                indexItemSqls.put(index, itemSql.trim());
                this.includeFields.add(item.getString("field"));
            }
            if (CommonsUtils.DEVLOG) System.out.println("[dev] Parse item : " + item + " >> " + itemSql);
        }

        if (indexItemSqls.isEmpty()) return null;

        String equationHold = equation;
        if ((equation = validEquation(equation)) == null) {
            throw new FilterParseException(Language.L("无效的高级表达式 : %s", equationHold));
        }

        if ("OR".equalsIgnoreCase(equation)) {
            return "( " + StringUtils.join(indexItemSqls.values(), " or ") + " )";
        } else if ("AND".equalsIgnoreCase(equation)) {
            return "( " + StringUtils.join(indexItemSqls.values(), " and ") + " )";
        } else {
            // 高级表达式 eg: (1 AND 2) or (3 AND 4)
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
                    log.warn("Invalid equation token : {}", token);
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
     * @param specRootEntity
     * @return
     */
    private String parseItem(JSONObject item, JSONObject values, Entity specRootEntity) {
        String field = item.getString("field");
        if (field.startsWith("&amp;")) field = field.replace("&amp;", NAME_FIELD_PREFIX);  // fix: _$unthy

        final boolean hasNameFlag = field.startsWith(NAME_FIELD_PREFIX);
        if (hasNameFlag) field = field.substring(1);

        Field lastFieldMeta = VF_ACU.equals(field)
                ? specRootEntity.getField(EntityHelper.ApprovalLastUser)
                : MetadataHelper.getLastJoinField(specRootEntity, field);
        if (lastFieldMeta == null) {
            log.warn("Invalid field : {} in {}", field, specRootEntity.getName());
            return null;
        }

        DisplayType dt = EasyMetaFactory.getDisplayType(lastFieldMeta);
        if (dt == DisplayType.CLASSIFICATION || (dt == DisplayType.PICKLIST && hasNameFlag) /* 快速查询 */) {
            field = NAME_FIELD_PREFIX + field;
        } else if (hasNameFlag) {
            if (!(dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE)) {
                log.warn("Non reference-field : {} in {}", field, specRootEntity.getName());
                return null;
            }

            // 转为名称字段
            if (dt == DisplayType.REFERENCE) {
                lastFieldMeta = lastFieldMeta.getReferenceEntity().getNameField();
                dt = EasyMetaFactory.getDisplayType(lastFieldMeta);
                field += "." + lastFieldMeta.getName();
            }
        }

        // 多引用用户
        final boolean isN2NUsers = dt == DisplayType.N2NREFERENCE
                && lastFieldMeta.getReferenceEntity().getEntityCode() == EntityHelper.User;

        String op = item.getString("op");
        Object checkValue = useValueOfVarField(item.getString("value"), lastFieldMeta);
        if (checkValue instanceof VarFieldNoValue37) return "(1=2)";
        String value = (String) checkValue;
        String valueEnd = null;

        // v3.8
        if (useFulltextOp(field) && "LK".equals(op)) op = "FT";

        if (dt == DisplayType.N2NREFERENCE) {
            String inWhere = null;
            boolean forceNot = false;
            if (hasNameFlag) {
                Entity refEntity = lastFieldMeta.getReferenceEntity();
                Field nameField = refEntity.getNameField();

                JSONObject fakeItem = (JSONObject) JSONUtils.clone(item);
                fakeItem.put("field", nameField.getName());
                fakeItem.put("op", fakeItem.getString("op"));

                // Not 转换
                String opCheck = fakeItem.getString("op");
                forceNot = ParseHelper.NLK.equalsIgnoreCase(opCheck) || ParseHelper.NEQ.equalsIgnoreCase(opCheck);
                if (forceNot) fakeItem.put("op", opCheck.substring(1));  // Remove `N`

                String realWhereSql = parseItem(fakeItem, null, refEntity);
                inWhere = String.format("select %s from %s where %s",
                        refEntity.getPrimaryField().getName(), refEntity.getName(), realWhereSql);
            }
            else if (isN2NUsers) {
                if (ParseHelper.SFU.equalsIgnoreCase(op)) {
                    op = ParseHelper.IN;
                    value = UserContextHolder.getReplacedUser().toLiteral();
                }

                if (ParseHelper.IN.equals(op) || ParseHelper.NIN.equals(op)) {
                    inWhere = parseValue(value, op, lastFieldMeta, false);
                    if (inWhere != null) inWhere = inWhere.substring(1, inWhere.length() - 1);
                    forceNot = ParseHelper.NIN.equals(op);
                }
                else if (ParseHelper.SFB.equalsIgnoreCase(op)) {
                    op = ParseHelper.IN;
                    value = Objects.requireNonNull(UserHelper.getDepartment(UserContextHolder.getReplacedUser())).getIdentity().toString();
                    inWhere = String.format("select userId from User where deptId = '%s'", value);
                }
            }
            // 查询 ID，仅支持 IN
            else if (ParseHelper.IN.equals(op) && ID.isId(value)) {
                inWhere = quoteValue(value, FieldType.STRING);
            }

            if (inWhere != null) {
                String xJoinField = specRootEntity.getPrimaryField().getName();
                if (StringUtils.countMatches(field, ".") > 0) {
                    xJoinField = field.substring(0, field.lastIndexOf("."));
                }

                String inWhere2 = String.format(
                        " in (select recordId from NreferenceItem where belongEntity = '%s' and belongField = '%s' and referenceId in (%s))",
                        lastFieldMeta.getOwnEntity().getName(), lastFieldMeta.getName(), inWhere);

                if (forceNot) inWhere2 = " not" + inWhere2;
                return xJoinField + inWhere2;
            }

            // else `NL` `NT`

        } else if (dt == DisplayType.TAG && (ParseHelper.IN.equals(op) || ParseHelper.NIN.equals(op))) {
            String xJoinField = specRootEntity.getPrimaryField().getName();
            if (StringUtils.countMatches(field, ".") > 0) {
                xJoinField = field.substring(0, field.lastIndexOf("."));
            }

            String inWhere = String.format(
                    " in (select recordId from TagItem where belongEntity = '%s' and belongField = '%s' and tagName in (%s))",
                    lastFieldMeta.getOwnEntity().getName(), lastFieldMeta.getName(), quoteValue(value, FieldType.STRING));

            if (ParseHelper.NIN.equals(op)) inWhere = " not" + inWhere;
            return xJoinField + inWhere;
        }

        // 根据字段类型转换 `op`

        final boolean isDatetime = dt == DisplayType.DATETIME;

        // 日期时间
        if (isDatetime || dt == DisplayType.DATE) {

            final boolean isREX = ParseHelper.RED.equalsIgnoreCase(op)
                    || ParseHelper.REM.equalsIgnoreCase(op)
                    || ParseHelper.REY.equalsIgnoreCase(op);
            final boolean isHHH = ParseHelper.HHH.equalsIgnoreCase(op);

            if (ParseHelper.TDA.equalsIgnoreCase(op)
                    || ParseHelper.YTA.equalsIgnoreCase(op)
                    || ParseHelper.TTA.equalsIgnoreCase(op)
                    || ParseHelper.DDD.equalsIgnoreCase(op) || isHHH
                    || ParseHelper.EVW.equalsIgnoreCase(op) || ParseHelper.EVM.equalsIgnoreCase(op)) {

                if (ParseHelper.DDD.equalsIgnoreCase(op)) {
                    int x = NumberUtils.toInt(value);
                    value = formatDate(addDay(x), 0);
                } else if (isHHH) {
                    int x = NumberUtils.toInt(value);
                    Date datetime = CalendarUtils.add(x, Calendar.HOUR_OF_DAY);
                    value = CalendarUtils.getUTCDateTimeFormat().format(datetime);
                    value = value.substring(0, 14) + "00:00";
                    valueEnd = value.substring(0, 14) + "59:59";
                } else if (ParseHelper.EVW.equalsIgnoreCase(op) || ParseHelper.EVM.equalsIgnoreCase(op)) {
                    final Calendar today = CalendarUtils.getInstance();

                    int x = NumberUtils.toInt(value);
                    if (ParseHelper.EVW.equalsIgnoreCase(op)) {
                        boolean isSunday = today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
                        if (isSunday) today.add(Calendar.DAY_OF_WEEK, -1);
                        if (x < 1) x = 1;
                        if (x > 7) x = 7;
                        x += 1;
                        if (x <= 7) {
                            today.set(Calendar.DAY_OF_WEEK, x);
                        } else {
                            today.set(Calendar.DAY_OF_WEEK, 7);
                            today.add(Calendar.DAY_OF_WEEK, 1);
                        }
                    } else {
                        if (x < 1) x = 1;
                        if (x > 31) x = 31;

                        // v3.4.4 每月最后一天
                        int maxDayOfMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH);
                        if (x > maxDayOfMonth) x = maxDayOfMonth;

                        today.set(Calendar.DAY_OF_MONTH, x);
                    }
                    value = formatDate(today.getTime(), 0);
                }
                else if (ParseHelper.YTA.equalsIgnoreCase(op)) {
                    value = formatDate(addDay(-1), 0);
                } else if (ParseHelper.TTA.equalsIgnoreCase(op)) {
                    value = formatDate(addDay(1), 0);
                } else {
                    value = formatDate(CalendarUtils.now(), 0);
                }

                if (isDatetime) {
                    op = ParseHelper.BW;
                    if (!isHHH) valueEnd = parseValue(value, op, lastFieldMeta, true);
                }

            } else if (ParseHelper.CUW.equalsIgnoreCase(op)
                    || ParseHelper.CUM.equalsIgnoreCase(op)
                    || ParseHelper.CUQ.equalsIgnoreCase(op)
                    || ParseHelper.CUY.equalsIgnoreCase(op)
                    || ParseHelper.PUW.equalsIgnoreCase(op)
                    || ParseHelper.PUM.equalsIgnoreCase(op)
                    || ParseHelper.PUQ.equalsIgnoreCase(op)
                    || ParseHelper.PUY.equalsIgnoreCase(op)
                    || ParseHelper.NUW.equalsIgnoreCase(op)
                    || ParseHelper.NUM.equalsIgnoreCase(op)
                    || ParseHelper.NUQ.equalsIgnoreCase(op)
                    || ParseHelper.NUY.equalsIgnoreCase(op)) {

                String unit = op.substring(2);
                int amount = op.startsWith("P") ? -1 : (op.startsWith("N") ? 1 : 0);

                Date begin = Moment.moment().startOf(op.substring(2)).add(amount, unit).date();
                value = formatDate(begin, 0);

                Date end = Moment.moment(begin).endOf(unit).date();
                valueEnd = formatDate(end, 0);

                if (isDatetime) {
                    value += ParseHelper.ZERO_TIME;
                    valueEnd += ParseHelper.FULL_TIME;
                }
                op = ParseHelper.BW;

            } else if (ParseHelper.EQ.equalsIgnoreCase(op)
                    && dt == DisplayType.DATETIME && StringUtils.length(value) == 10) {

                op = ParseHelper.BW;
                valueEnd = parseValue(value, op, lastFieldMeta, true);

            } else if (isREX
                    || ParseHelper.FUD.equalsIgnoreCase(op)
                    || ParseHelper.FUM.equalsIgnoreCase(op)
                    || ParseHelper.FUY.equalsIgnoreCase(op)) {

                int xValue = NumberUtils.toInt(value) * (isREX ? -1 : 1);
                Date date;
                if (ParseHelper.REM.equalsIgnoreCase(op) || ParseHelper.FUM.equalsIgnoreCase(op)) {
                    date = CalendarUtils.addMonth(xValue);
                } else if (ParseHelper.REY.equalsIgnoreCase(op) || ParseHelper.FUY.equalsIgnoreCase(op)) {
                    date = CalendarUtils.addMonth(xValue * 12);
                } else {
                    date = CalendarUtils.addDay(xValue);
                }

                if (isREX) {
                    value = formatDate(date, 0);
                    valueEnd = formatDate(CalendarUtils.now(), 0);
                } else {
                    value = formatDate(CalendarUtils.now(), 0);
                    valueEnd = formatDate(date, 0);
                }

                if (isDatetime) {
                    value += ParseHelper.ZERO_TIME;
                    valueEnd += ParseHelper.FULL_TIME;
                }
                op = ParseHelper.BW;

            } else if (ParseHelper.YYY.equalsIgnoreCase(op)
                    || ParseHelper.MMM.equalsIgnoreCase(op)) {

                int xValue = NumberUtils.toInt(value);
                Calendar now = CalendarUtils.getInstance();

                if (ParseHelper.YYY.equalsIgnoreCase(op)) {
                    now.add(Calendar.YEAR, xValue);
                    value = now.get(Calendar.YEAR) + "-01-01";
                    valueEnd = now.get(Calendar.YEAR) + "-12-31";
                } else {
                    now.set(Calendar.DAY_OF_MONTH, 1);
                    now.add(Calendar.MONTH, xValue);

                    value = CalendarUtils.getUTCDateFormat().format(now.getTime());
                    Moment last = Moment.moment(now.getTime()).endOf(Moment.UNIT_MONTH);
                    valueEnd = CalendarUtils.getUTCDateFormat().format(last.date());
                }
                op = ParseHelper.BW;
            }

        } else if (dt == DisplayType.TIME) {
            // 前端输入 HH:mm
            if (value != null && value.length() == 5) {
                if (ParseHelper.EQ.equalsIgnoreCase(op)) {
                    op = ParseHelper.BW;
                    valueEnd = value + ":59";
                }
                value += ":00";
            }

        } else if (dt == DisplayType.MULTISELECT) {
            if (ParseHelper.IN.equalsIgnoreCase(op) || ParseHelper.NIN.equalsIgnoreCase(op)
                    || ParseHelper.EQ.equalsIgnoreCase(op) || ParseHelper.NEQ.equalsIgnoreCase(op)) {
                // 多选的包含/不包含要按位计算
                if (ParseHelper.IN.equalsIgnoreCase(op)) op = ParseHelper.BAND;
                else if (ParseHelper.NIN.equalsIgnoreCase(op)) op = ParseHelper.NBAND;

                long maskValue = 0;
                for (String s : value.split("\\|")) {
                    maskValue += ObjectUtils.toLong(s);
                }
                value = String.valueOf(maskValue);
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
            value = formatDate(addDay(-NumberUtils.toInt(value)), isDatetime ? 1 : 0);
        } else if (ParseHelper.BFM.equalsIgnoreCase(op)) {
            value = formatDate(addMonth(-NumberUtils.toInt(value)), isDatetime ? 1 : 0);
        } else if (ParseHelper.BFY.equalsIgnoreCase(op)) {
            value = formatDate(addMonth(-NumberUtils.toInt(value) * 12), isDatetime ? 1 : 0);
        } else if (ParseHelper.AFD.equalsIgnoreCase(op)) {
            value = formatDate(addDay(NumberUtils.toInt(value)), isDatetime ? 2 : 0);
        } else if (ParseHelper.AFM.equalsIgnoreCase(op)) {
            value = formatDate(addMonth(NumberUtils.toInt(value)), isDatetime ? 2 : 0);
        } else if (ParseHelper.AFY.equalsIgnoreCase(op)) {
            value = formatDate(addMonth(NumberUtils.toInt(value) * 12), isDatetime ? 2 : 0);
        }
        // 部门/用户
        else if (ParseHelper.SFU.equalsIgnoreCase(op)) {
            value = UserContextHolder.getReplacedUser().toLiteral();
        } else if (ParseHelper.SFB.equalsIgnoreCase(op)) {
            Department dept = UserHelper.getDepartment(UserContextHolder.getReplacedUser());
            if (dept != null) {
                value = dept.getIdentity().toString();
                int ref = lastFieldMeta.getReferenceEntity().getEntityCode();
                if (ref == EntityHelper.User) {
                    sb.insert(sb.indexOf(" "), ".deptId");
                } else if (ref == EntityHelper.Department) {
                    // NOOP
                } else {
                    value = null;
                }
            }
        } else if (ParseHelper.SFD.equalsIgnoreCase(op)) {
            Department dept = UserHelper.getDepartment(UserContextHolder.getReplacedUser());
            if (dept != null) {
                int refe = lastFieldMeta.getReferenceEntity().getEntityCode();
                if (refe == EntityHelper.Department) {
                    value = StringUtils.join(UserHelper.getAllChildren(dept), "|");
                }
            }
        } else if (ParseHelper.SFT.equalsIgnoreCase(op)) {
            if (value == null) value = "0";  // No any
            // `in`
            value = String.format(
                    "( select userId from TeamMember where teamId in ('%s') )",
                    StringUtils.join(value.split("\\|"), "', '"));
        } else if (ParseHelper.REP.equalsIgnoreCase(op)) {
            // `in`
            value = MessageFormat.format(
                    "( select {0} from {1} group by {0} having (count({0}) > {2}) )",
                    field, rootEntity.getName(), String.valueOf(NumberUtils.toInt(value, 1)));
        }

        if (StringUtils.isBlank(value)) {
            log.warn("No search value defined : {}", item.toJSONString());
            return null;
        }

        // 快速搜索的占位符 `{1}`
        if (value.matches("\\{\\d+}")) {
            if (values == null || values.isEmpty()) return null;

            String valHold = value.replaceAll("[{}]", "");
            value = parseValue(values.get(valHold), op, lastFieldMeta, false);
        } else {
            value = parseValue(value, op, lastFieldMeta, false);
        }

        // No value for search
        if (value == null) return null;

        // 区间
        final boolean isBetween = op.equalsIgnoreCase(ParseHelper.BW);
        if (isBetween && valueEnd == null) {
            Object checkValueEnd = useValueOfVarField(item.getString("value2"), lastFieldMeta);
            if (checkValueEnd instanceof VarFieldNoValue37) return "(1=2)";
            valueEnd = (String) checkValueEnd;
            valueEnd = parseValue(valueEnd, op, lastFieldMeta, true);
            if (valueEnd == null) valueEnd = value;
        }

        // IN
        if (op.equalsIgnoreCase(ParseHelper.IN) || op.equalsIgnoreCase(ParseHelper.NIN)
                || op.equalsIgnoreCase(ParseHelper.SFD) || op.equalsIgnoreCase(ParseHelper.SFT)
                || op.equalsIgnoreCase(ParseHelper.REP)) {
            sb.append(value);
        } else {
            // LIKE
            if (op.equalsIgnoreCase(ParseHelper.LK) || op.equalsIgnoreCase(ParseHelper.NLK)) {
                value = '%' + value + '%';
            } else if (op.equalsIgnoreCase(ParseHelper.LK1)) {
                value = '%' + value;
            } else if (op.equalsIgnoreCase(ParseHelper.LK2)) {
                value = value + '%';
            }
            sb.append(quoteValue(value, lastFieldMeta.getType()));
        }

        if (isBetween) {
            sb.insert(0, "( ")
                    .append(" and ").append(quoteValue(valueEnd, lastFieldMeta.getType()))
                    .append(" )");
        }

        if (VF_ACU.equals(field)) {
            return String.format(
                    "(exists (select recordId from RobotApprovalStep where ^%s = recordId and state = 1 and isCanceled = 'F' and %s) and approvalState = 2)",
                    specRootEntity.getPrimaryField().getName(), sb.toString().replace(VF_ACU, "approver"));
        } else {
            return sb.toString();
        }
    }

    /**
     * @param val
     * @param op
     * @param field
     * @param fullTime 仅对日期时间有意义
     * @return
     */
    private String parseValue(Object val, String op, Field field, boolean fullTime) {
        String value;
        // IN
        if (val instanceof JSONArray) {
            Set<String> inVals = new HashSet<>();
            for (Object v : (JSONArray) val) {
                inVals.add(quoteValue(v.toString(), field.getType()));
            }
            return optimizeIn(inVals);

        } else {
            value = val == null ? null : val.toString();
            if (StringUtils.isBlank(value)) return null;

            final int valueLen = StringUtils.length(value);

            // TIMESTAMP 仅指定了日期值，则补充时间值
            if (field.getType() == FieldType.TIMESTAMP && valueLen == 10) {
                if (ParseHelper.GT.equalsIgnoreCase(op)) {
                    value += ParseHelper.FULL_TIME;  // 不含当日
                } else if (ParseHelper.LT.equalsIgnoreCase(op)) {
                    value += ParseHelper.ZERO_TIME;  // 不含当日
                } else if (ParseHelper.GE.equalsIgnoreCase(op)) {
                    value += ParseHelper.ZERO_TIME;  // 含当日
                } else if (ParseHelper.LE.equalsIgnoreCase(op)) {
                    value += ParseHelper.FULL_TIME;  // 含当日
                } else if (ParseHelper.BW.equalsIgnoreCase(op)) {
                    value += (fullTime ? ParseHelper.FULL_TIME : ParseHelper.ZERO_TIME);  // 含当日
                }
            }
            // v3.8 不修正了，否则因为格式问题（如日期带日、不带日）就带来不同的查询结果，这很怪异
//            // 修正月、日
//            else if (field.getType() == FieldType.DATE && valueLen == 10) {
//                String dateFormat = StringUtils.defaultIfBlank(
//                        EasyMetaFactory.valueOf(field).getExtraAttr(EasyFieldConfigProps.DATE_FORMAT),
//                        DisplayType.DATE.getDefaultFormat());
//                if (dateFormat.length() == 4) {
//                    value = value.substring(0, 4) + "-01-01";
//                } else if (dateFormat.length() == 7) {
//                    value = value.substring(0, 7) + "-01";
//                }
//            }

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
            return String.format("'%s'", CommonsUtils.escapeSql(val));
        }
        return "''";
    }

    /**
     * @param inVals
     * @return
     */
    private String optimizeIn(Set<String> inVals) {
        if (inVals == null || inVals.isEmpty()) return null;
        else return "( " + StringUtils.join(inVals, ",") + " )";
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
     * @param date
     * @param paddingTimeType 0=无, 1=FULLTIME, 2=ZEROTIME
     * @return
     */
    private String formatDate(Date date, int paddingTimeType) {
        String s = CalendarUtils.getUTCDateFormat().format(date);
        if (paddingTimeType == 1) s += ParseHelper.FULL_TIME;
        else if (paddingTimeType == 2) s += ParseHelper.ZERO_TIME;
        return s;
    }

    /**
     * 快速查询
     */
    private void rebuildQuickFilter38() {
        String quickFields = filterExpr.getString("quickFields");
        JSONArray quickItems = buildQuickFilterItems(quickFields, 1);

        JSONObject values = filterExpr.getJSONObject("values");
        final String quickValue = values.values().iterator().next().toString();

        // eg: =完全相等, *后匹配, 前匹配*
        if (quickValue.startsWith("=") || quickValue.startsWith("*") || quickValue.endsWith("*")) {
            String op2 = ParseHelper.EQ;
            String value2;
            if (quickValue.startsWith("*")) op2 = ParseHelper.LK1;
            else if (quickValue.endsWith("*")) op2 = ParseHelper.LK2;
            if (quickValue.endsWith("*")) value2 = quickValue.substring(0, quickValue.length() - 1);
            else value2 = quickValue.substring(1);

            for (Object o : quickItems) {
                JSONObject item = (JSONObject) o;
                item.put("op", op2);
                item.put("value", value2);
            }

        } else {
            // v3.6-b4,3.7: 多值查询（转义可输入 \|）。eg: 值1|值2
            String[] m = quickValue.split("(?<!\\\\)\\|");
            if (m.length > 1) {
                values.clear();
                values.put("1", m[0].trim());

                for (int i = 2; i <= m.length; i++) {
                    JSONArray quickItemsPlus = buildQuickFilterItems(quickFields, i);
                    values.put(String.valueOf(i), m[i - 1].trim());
                    quickItems.addAll(quickItemsPlus);
                }
                filterExpr.put("values", values);
            }
        }

        // 覆盖
        filterExpr.put("items", quickItems);
    }

    /**
     * @param quickFields
     * @param valueIndex
     * @return
     */
    private JSONArray buildQuickFilterItems(String quickFields, int valueIndex) {
        Set<String> usesFields = ParseHelper.buildQuickFields(rootEntity, quickFields);
        JSONArray items = new JSONArray();
        for (String field : usesFields) {
            items.add(JSON.parseObject("{ op:'LK', value:'{" + valueIndex + "}', field:'" + field + "' }"));
        }
        return items;
    }

    /**
     * 使用全文索引查詢
     *
     * @param fieldName
     * @return
     */
    private boolean useFulltextOp(String fieldName) {
        if (!CommandArgs.getBoolean(CommandArgs._UseDbFullText)) return false;

        Set<String> canFulltexts = new HashSet<>();
        canFulltexts.add("40#content");

        String key = rootEntity.getEntityCode() + "#" + fieldName;
        return canFulltexts.contains(key);
    }

    // 字段变量 {@FIELD}
    private static final String PATT_FIELDVAR = "\\{@([\\w.]+)}";
    // `当前`变量（当前日期、时间、用户）
    private static final String CURRENT_ANY = "CURRENT";
    private static final String CURRENT_DATE = "NOW";

    /**
     * 获取字段变量值
     * fix 3.7.2 获取不到原样返回
     *
     * @param value
     * @param queryField
     * @return
     */
    protected Object useValueOfVarField(final String value, Field queryField) {
        if (StringUtils.isBlank(value) || !value.matches(PATT_FIELDVAR)) return value;

        // {@FIELD} > FIELD
        final String fieldName = value.substring(2, value.length() - 1);

        Object useValue = null;

        // {@CURRENT} for DATE,TIME and Ref:User,Department
        if (CURRENT_ANY.equals(fieldName) || CURRENT_DATE.equals(fieldName)) {
            DisplayType dt = EasyMetaFactory.getDisplayType(queryField);
            if (dt == DisplayType.DATE || dt == DisplayType.DATETIME || dt == DisplayType.TIME) {
                useValue = dt == DisplayType.TIME ? LocalTime.now() : CalendarUtils.now();

            } else if (dt == DisplayType.REFERENCE) {
                if (queryField.getReferenceEntity().getEntityCode() == EntityHelper.User) {
                    useValue = UserContextHolder.getReplacedUser();
                } else if (queryField.getReferenceEntity().getEntityCode() == EntityHelper.Department) {
                    Department dept = UserHelper.getDepartment(UserContextHolder.getReplacedUser());
                    if (dept != null) useValue = dept.getIdentity();
                }
            } else {
                log.warn("Cannot use `{}` in `{}` (None date/ref fields)", value, queryField);
                return new VarFieldNoValue37(value);
            }
        }
        // {@CURRENT.} for USER
        if (fieldName.startsWith(CURRENT_ANY + ".")) {
            String userField = fieldName.substring(CURRENT_ANY.length() + 1);
            Object[] o = Application.getQueryFactory().uniqueNoFilter(UserContextHolder.getReplacedUser(), userField);
            if (o == null || o[0] == null) {
                log.warn("Cannot use `{}` in `{}` (No value found)", value, queryField);
                return new VarFieldNoValue37(value);
            } else {
                useValue = o[0];
            }
        }

        if (useValue == null) {
            if (varRecord == null) return value;

            Field valueField = MetadataHelper.getLastJoinField(rootEntity, fieldName);
            if (valueField == null) {
                throw new MissingMetaExcetion(fieldName, rootEntity.getName());
            }

            Object[] o = Application.getQueryFactory().uniqueNoFilter(varRecord, fieldName);
            if (o == null || o[0] == null) {
                log.warn("Cannot use `{}` in `{}` (None value found)", value, queryField);
                return new VarFieldNoValue37(value);
            }
            useValue = o[0];
        }

        if (useValue instanceof Date) {
            useValue = CalendarUtils.getUTCDateFormat().format(useValue);
        } else if (useValue instanceof TemporalAccessor) {
            useValue = DateTimeFormatter.ofPattern(DisplayType.TIME.getDefaultFormat()).format((TemporalAccessor) useValue);
        } else if (useValue instanceof BigDecimal) {
            useValue = String.valueOf(((BigDecimal) useValue).doubleValue());
        } else {
            useValue = String.valueOf(useValue);
        }
        return useValue;
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
            if (clearEquation.isEmpty()) return equation;
        }
        return null;
    }

    /**
     * 是否含有变量字段 `{@FIELD}`
     *
     * @param filterExpr
     * @return
     * @see #useValueOfVarField(String, Field)
     */
    public static boolean hasVarFields(JSONObject filterExpr) {
        for (Object o : filterExpr.getJSONArray("items")) {
            JSONObject item = (JSONObject) o;
            String value = item.getString("value");
            if (value != null && value.matches(PATT_FIELDVAR)) return true;
            String value2 = item.getString("value2");
            if (value2 != null && value2.matches(PATT_FIELDVAR)) return true;
        }
        return false;
    }

    /**
     * 变量字段无值
     */
    static class VarFieldNoValue37 {
        String varField;
        VarFieldNoValue37(String varField) {
            this.varField = varField;
        }
    }
}
