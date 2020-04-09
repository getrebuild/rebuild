/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.query.AdvFilterParser;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
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
public abstract class ChartData extends SetUser<ChartData> implements ChartSpec {
	
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
			throw new ChartsException("字段 [" + fieldName.toUpperCase() + " ] 不存在，请调整图表配置");
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
	protected String getFilterSql() {
		String previewFilter = StringUtils.EMPTY;
		// 限制预览数据量
		if (isFromPreview() && getSourceEntity().containsField(EntityHelper.AutoId)) {
			String maxAidSql = String.format("select max(%s) from %s", EntityHelper.AutoId, getSourceEntity().getName());
			Object[] o = Application.createQueryNoFilter(maxAidSql).unique();
			long maxAid = ObjectUtils.toLong(o[0]);
			if (maxAid > 5000) {
				previewFilter = String.format("(%s >= %d) and ", EntityHelper.AutoId, Math.max(maxAid - 2000, 0));
			}
		}
		
		JSONObject filterExp = config.getJSONObject("filter");
		if (filterExp == null || filterExp.isEmpty()) {
			return previewFilter + "(1=1)";
		}
		
		AdvFilterParser filterParser = new AdvFilterParser(filterExp);
		String sqlWhere = filterParser.toSqlWhere();
		if (sqlWhere != null) {
			sqlWhere = previewFilter + sqlWhere;
		}
		return StringUtils.defaultIfBlank(sqlWhere, "(1=1)");
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
		// 优先维度排序
		if (!sorts.isEmpty()) {
			return String.join(", ", sorts);
		}
		
		for (Numerical num : getNumericals()) {
			FormatSort fs = num.getFormatSort();
			if (fs != FormatSort.NONE) {
				sorts.add(num.getSqlName() + " " + fs.toString().toLowerCase());
			}
		}
		return sorts.isEmpty() ? null : String.join(", ", sorts);
	}
	
	/**
	 * @param axis
	 * @param value
	 * @return
	 */
	protected String wrapAxisValue(Axis axis, Object value) {
		return axis instanceof Numerical
				? wrapAxisValue((Numerical) axis, value)
				: wrapAxisValue((Dimension) axis, value);
	}
	
	/**
	 * 格式化数值
	 * 
	 * @param axis
	 * @param value
	 * @return
	 */
	protected String wrapAxisValue(Numerical axis, Object value) {
		if (value == null) {
			return "0";
		}
		
		String format = "###";
		if (axis.getScale() > 0) {
			format = "##0.";
			format = StringUtils.rightPad(format, format.length() + axis.getScale(), "0");
		}
		
		if (ID.isId(value)) {
			value = 1;
		}
		return new DecimalFormat(format).format(value);
	}
	
	/**
	 * 获取纬度标签
	 * 
	 * @param axis
	 * @param value
	 * @return
	 */
	protected String wrapAxisValue(Dimension axis, Object value) {
		if (value == null) {
			return "无";
		}

		EasyMeta axisField = EasyMeta.valueOf(axis.getField());
		DisplayType axisType = axisField.getDisplayType();

		String label;
		if (axisType == DisplayType.REFERENCE || axisType == DisplayType.CLASSIFICATION
                || axisType == DisplayType.BOOL || axisType == DisplayType.PICKLIST || axisType == DisplayType.STATE) {
			label = (String) FieldValueWrapper.instance.wrapFieldValue(value, axisField, true);
		} else {
			label = value.toString();
		}
		return label;
	}

	/**
	 * 格式化汇总数值
	 *
	 * @param sumOfAxis
	 * @param value
	 * @return
	 */
	protected String wrapSumValue(Axis sumOfAxis, Object value) {
		if (value == null) {
			return "0";
		}

		if (sumOfAxis instanceof Numerical) {
			return wrapAxisValue((Numerical) sumOfAxis, value);
		} else {
			return value.toString();
		}
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
}
