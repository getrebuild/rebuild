/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图表数据
 *
 * @author devezhao
 * @since 12/14/2018
 */
public abstract class ChartData extends SetUser implements ChartSpec {

    protected JSONObject config;

    private boolean fromPreview = false;
    // 额外的图表参数
    private Map<String, Object> extraParams;

    /**
     * @param config
     */
    protected ChartData(JSONObject config) {
        this.config = config;
    }

    /**
     * @return
     */
    public Map<String, Object> getExtraParams() {
        return extraParams == null ? Collections.emptyMap() : extraParams;
    }

    /**
     * @param extraParams
     */
    public ChartData setExtraParams(Map<String, Object> extraParams) {
        this.extraParams = extraParams;
        return this;
    }

    /**
     * 预览模式
     *
     * @return
     */
    protected boolean isFromPreview() {
        return fromPreview;
    }

    /**
     * 源实体
     *
     * @return
     */
    public Entity getSourceEntity() {
        String e = config.getString("entity");
        return MetadataHelper.getEntity(e);
    }

    /**
     * 标题
     *
     * @return
     */
    public String getTitle() {
        return StringUtils.defaultIfBlank(config.getString("title"), "未命名图表");
    }

    /**
     * 维度轴
     *
     * @return
     */
    public Dimension[] getDimensions() {
        JSONArray items = config.getJSONObject("axis").getJSONArray("dimension");
        if (items == null || items.isEmpty()) {
            return new Dimension[0];
        }

        List<Dimension> list = new ArrayList<>();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            Field[] validFields = getValidFields(item);
            Dimension dim = new Dimension(
                    validFields[0], getFormatSort(item), getFormatCalc(item),
                    item.getString("label"),
                    validFields[1]);
            list.add(dim);
        }
        return list.toArray(new Dimension[0]);
    }

    /**
     * 数值轴
     *
     * @return
     */
    public Numerical[] getNumericals() {
        JSONArray items = config.getJSONObject("axis").getJSONArray("numerical");
        if (items == null || items.isEmpty()) {
            return new Numerical[0];
        }

        List<Numerical> list = new ArrayList<>();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            Field[] validFields = getValidFields(item);
            Numerical num = new Numerical(
                    validFields[0], getFormatSort(item), getFormatCalc(item),
                    item.getString("label"),
                    item.getInteger("scale"),
                    item.getJSONObject("filter"),
                    validFields[1]);
            list.add(num);
        }
        return list.toArray(new Numerical[0]);
    }

    /**
     * @param item
     * @return
     * @see MetadataHelper#getLastJoinField(Entity, String)
     */
    private Field[] getValidFields(JSONObject item) {
        String fieldName = item.getString("field");
        if (MetadataHelper.getLastJoinField(getSourceEntity(), fieldName) == null) {
            throw new DefinedException(Language.L("字段 [%s] 已不存在，请调整图表配置", fieldName.toUpperCase()));
        }

        Field[] fields = new Field[2];
        String[] fieldNames = fieldName.split("\\.");

        if (fieldNames.length > 1) {
            fields[1] = getSourceEntity().getField(fieldNames[0]);
            fields[0] = fields[1].getReferenceEntity().getField(fieldNames[1]);
        } else {
            fields[0] = getSourceEntity().getField(fieldNames[0]);
        }
        return fields;
    }

    /**
     * @param item
     * @return
     */
    private FormatSort getFormatSort(JSONObject item) {
        if (StringUtils.isNotBlank(item.getString("sort"))) {
            return FormatSort.valueOf(item.getString("sort"));
        }
        return FormatSort.NONE;
    }

    /**
     * @param item
     * @return
     */
    private FormatCalc getFormatCalc(JSONObject item) {
        if (StringUtils.isNotBlank(item.getString("calc"))) {
            return FormatCalc.valueOf(item.getString("calc"));
        }
        return FormatCalc.NONE;
    }

    /**
     * 获取过滤 SQL
     *
     * @return
     */
    protected String getFilterSql(Numerical withAxisFilter) {
        String where = getFilterSql();
        if (withAxisFilter != null && ParseHelper.validAdvFilter(withAxisFilter.getFilter())) {
            AdvFilterParser filterParser = new AdvFilterParser(withAxisFilter.getFilter());
            String fieldWhere = filterParser.toSqlWhere();
            if (fieldWhere != null) where = String.format("((%s) and (%s))", where, fieldWhere);
        }
        return where;
    }

    /**
     * 获取过滤 SQL
     *
     * @return
     */
    protected String getFilterSql() {
        String previewFilter = StringUtils.EMPTY;
        // 限制预览数据量
        if (isFromPreview() && getSourceEntity().containsField(EntityHelper.AutoId)) {
            String maxAidSql = String.format("select max(autoId) from %s", getSourceEntity().getName());
            Object[] o = Application.createQueryNoFilter(maxAidSql).unique();
            long maxAid = ObjectUtils.toLong(o[0]);
            if (maxAid > 5000) {
                previewFilter = String.format("(%s >= %d) and ", EntityHelper.AutoId, Math.max(maxAid - 2000, 0));
            }
        }

        JSONObject filterExpr = config.getJSONObject("filter");
        if (ParseHelper.validAdvFilter(filterExpr)) {
            AdvFilterParser filterParser = new AdvFilterParser(filterExpr);
            String sqlWhere = filterParser.toSqlWhere();
            if (sqlWhere != null) {
                sqlWhere = previewFilter + sqlWhere;
            }
            return StringUtils.defaultIfBlank(sqlWhere, "(1=1)");
        }

        return previewFilter + "(1=1)";
    }

    /**
     * 获取排序 SQL
     *
     * @return
     */
    protected String getSortSql() {
        Set<String> sorts = new HashSet<>();
        for (Axis dim : getDimensions()) {
            FormatSort fs = dim.getFormatSort();
            if (fs != FormatSort.NONE) {
                sorts.add(dim.getSqlName() + " " + fs.toString().toLowerCase());
            }
        }
//        // NOTE 优先维度排序
//        if (!sorts.isEmpty()) {
//            return String.join(", ", sorts);
//        }

        for (Numerical num : getNumericals()) {
            FormatSort fs = num.getFormatSort();
            if (fs != FormatSort.NONE) {
                sorts.add(num.getSqlName() + " " + fs.toString().toLowerCase());
            }
        }
        return sorts.isEmpty() ? null : String.join(", ", sorts);
    }

    /**
     * 格式化数值（无千分位）
     *
     * @param numerical
     * @param value
     * @return
     */
    protected String wrapAxisValue(Numerical numerical, Object value) {
        return wrapAxisValue(numerical, value, Boolean.FALSE);
    }

    /**
     * @param numerical
     * @param value
     * @param useThousands 使用千分位
     * @return
     */
    protected String wrapAxisValue(Numerical numerical, Object value, boolean useThousands) {
        if (ChartsHelper.isZero(value)) {
            return ChartsHelper.VALUE_ZERO;
        }

        String format = "###";
        if (numerical.getScale() > 0) {
            format = "##0.";
            format = StringUtils.rightPad(format, format.length() + numerical.getScale(), "0");
        }

        if (useThousands) format = "#," + format;

        if (ID.isId(value)) value = 1;

        String n = new DecimalFormat(format).format(value);
        if (useThousands) n = formatAxisValue(numerical, n);
        return n;
    }

    /**
     * @see com.rebuild.core.metadata.easymeta.EasyDecimal#wrapValue(Object)
     */
    private String formatAxisValue(Numerical numerical, String value) {
        String type = getNumericalFlag(numerical);
        if (type == null) return value;

        if ("%".equals(type)) value += "%";
        else if (type.contains("%s")) value = String.format(type, value);
        else value = type + " " + value;
        return value;
    }

    /**
     * @see com.rebuild.core.metadata.easymeta.EasyDecimal#wrapValue(Object)
     */
    protected String getNumericalFlag(Numerical numerical) {
        if (numerical.getField().getType() != FieldType.DECIMAL) return null;

        if (!(numerical.getFormatCalc() == FormatCalc.SUM
                || numerical.getFormatCalc() == FormatCalc.AVG
                || numerical.getFormatCalc() == FormatCalc.MIN
                || numerical.getFormatCalc() == FormatCalc.MAX)) {
            return null;
        }

        String type = EasyMetaFactory.valueOf(numerical.getField()).getExtraAttr(EasyFieldConfigProps.DECIMAL_TYPE);
        if (type == null || "0".equalsIgnoreCase(type)) return null;
        else return type;
    }

    /**
     * 获取纬度标签
     *
     * @param dimension
     * @param value
     * @return
     */
    protected String wrapAxisValue(Dimension dimension, Object value) {
        return wrapAxisValue(dimension, value, Boolean.FALSE);
    }

    /**
     * 获取纬度标签
     *
     * @param dimension
     * @param value
     * @param useRefLink 使用链接
     * @return
     */
    protected String wrapAxisValue(Dimension dimension, Object value, boolean useRefLink) {
        if (value == null || value == ChartsHelper.VALUE_NONE) {
            return ChartsHelper.VALUE_NONE;
        }

        EasyField axisField = EasyMetaFactory.valueOf(dimension.getField());
        DisplayType axisType = axisField.getDisplayType();

        String label;
        if (axisType == DisplayType.REFERENCE
                || axisType == DisplayType.CLASSIFICATION
                || axisType == DisplayType.BOOL
                || axisType == DisplayType.PICKLIST
                || axisType == DisplayType.STATE) {
            label = (String) FieldValueHelper.wrapFieldValue(value, axisField, true);
            label = CommonsUtils.escapeHtml(label);

            if (useRefLink && axisType == DisplayType.REFERENCE
                    && ID.valueOf(value.toString()).getEntityCode() > 100) {
                label = String.format("<a href='/app/redirect?id=%s&type=newtab'>%s</a>", value, label);
            }

        } else {
            label = value.toString();
            label = CommonsUtils.escapeHtml(label);
        }
        return label;
    }

    /**
     * 构建数据
     *
     * @param fromPreview
     * @return
     */
    public JSON build(boolean fromPreview) {
        this.fromPreview = fromPreview;
        try {
            return this.build();
        } finally {
            this.fromPreview = false;
        }
    }

    /**
     * 创建查询。会自动处理权限选项
     *
     * @param sql
     * @return
     */
    protected Query createQuery(String sql) {
        if (this.fromPreview) {
            return Application.createQuery(sql, this.getUser());
        }

        boolean noPrivileges = false;
        JSONObject option = config.getJSONObject("option");
        if (option != null) {
            noPrivileges = option.getBooleanValue("noPrivileges");
        }
        String co = config.getString("chartOwning");
        ID chartOwning = ID.isId(co) ? ID.valueOf(co) : null;

        if (chartOwning == null || !noPrivileges) {
            return Application.createQuery(sql, this.getUser());
        } else {
            // 管理员创建的才能使用全部数据
            return Application.createQuery(sql,
                    UserHelper.isAdmin(chartOwning) ? UserService.SYSTEM_USER : this.getUser());
        }
    }

    /**
     * 1D [1-9]N
     *
     * @param dim
     * @param nums
     * @return
     */
    protected String buildSql(Dimension dim, Numerical[] nums) {
        List<String> numSqlItems = new ArrayList<>();
        for (Numerical num : nums) {
            numSqlItems.add(num.getSqlName());
        }

        String sql = "select {0},{1} from {2} where {3} group by {0}";
        sql = MessageFormat.format(sql,
                dim.getSqlName(),
                StringUtils.join(numSqlItems, ", "),
                getSourceEntity().getName(), getFilterSql());
        return appendSqlSort(sql);
    }

    /**
     * [1-9]D 1N
     *
     * @param dims
     * @param num
     * @return
     */
    protected String buildSql(Dimension[] dims, Numerical num) {
        List<String> dimSqlItems = new ArrayList<>();
        for (Dimension dim : dims) {
            dimSqlItems.add(dim.getSqlName());
        }

        String sql = "select {0},{1} from {2} where {3} group by {0}";
        sql = MessageFormat.format(sql,
                StringUtils.join(dimSqlItems, ", "),
                num.getSqlName(),
                getSourceEntity().getName(),
                getFilterSql());
        return appendSqlSort(sql);
    }

    /**
     * 1D 1N
     *
     * @param dim
     * @param num
     * @param withFilter
     * @return
     */
    protected String buildSql(Dimension dim, Numerical num, boolean withFilter) {
        String sql = "select {0},{1} from {2} where {3} group by {0}";
        String where = getFilterSql(withFilter ? num : null);

        sql = MessageFormat.format(sql,
                dim.getSqlName(),
                num.getSqlName(),
                getSourceEntity().getName(), where);
        return appendSqlSort(sql);
    }

    /**
     * [1-9]N
     *
     * @param nums
     * @return
     */
    protected String buildSql(Numerical[] nums) {
        List<String> numSqlItems = new ArrayList<>();
        for (Numerical num : nums) {
            numSqlItems.add(num.getSqlName());
        }

        String sql = "select {0} from {1} where {2}";
        sql = MessageFormat.format(sql,
                StringUtils.join(numSqlItems, ", "),
                getSourceEntity().getName(), getFilterSql());
        return appendSqlSort(sql);
    }

    /**
     * @param num
     * @param withFilter
     * @return
     */
    protected String buildSql(Numerical num, boolean withFilter) {
        String sql = "select {0} from {1} where {2}";
        String where = getFilterSql(withFilter ? num : null);

        sql = MessageFormat.format(sql, num.getSqlName(), getSourceEntity().getName(), where);
        return appendSqlSort(sql);
    }

    /**
     * 添加排序 SQL
     *
     * @param sql
     * @return
     */
    protected String appendSqlSort(String sql) {
        String sorts = getSortSql();
        if (sorts != null) sql += " order by " + sorts;
        return sql;
    }
}
