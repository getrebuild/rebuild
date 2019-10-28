/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.base;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.ClassificationManager;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 引用字段搜索
 * 
 * @author zhaofang123@gmail.com
 * @since 08/24/2018
 * @see RecentlyUsedSearch
 */
@Controller
@RequestMapping("/commons/search/")
public class ReferenceSearch extends BaseControll {
	
	// 快速搜索引用字段
	@RequestMapping({ "reference", "quick" })
	public void referenceSearch(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final String entity = getParameterNotNull(request, "entity");
		final String field = getParameterNotNull(request, "field");
		
		Entity metaEntity = MetadataHelper.getEntity(entity);
		Field referenceField = metaEntity.getField(field);
		if (referenceField.getType() != FieldType.REFERENCE) {
			writeSuccess(response, JSONUtils.EMPTY_ARRAY);
			return;
		}
		
		Entity referenceEntity = referenceField.getReferenceEntity();
		Field referenceNameField = MetadataHelper.getNameField(referenceEntity);
		if (referenceNameField == null) {
			LOG.warn("No name-field found : " + referenceEntity.getName());
			writeSuccess(response, JSONUtils.EMPTY_ARRAY);
			return;
		}
		
		String q = getParameter(request, "q");
		// 为空则加载最近使用的
		if (StringUtils.isBlank(q)) {
			String type = getParameter(request, "type");
			ID[] recently = Application.getRecentlyUsedCache().gets(user, referenceEntity.getName(), type);
			if (recently.length == 0) {
				writeSuccess(response, JSONUtils.EMPTY_ARRAY);
			} else {
				writeSuccess(response, RecentlyUsedSearch.formatSelect2(recently, null));
			}
			return;
		}
		q = StringEscapeUtils.escapeSql(q);
		
		String sql = "select {0},{1} from {2} where ( {1} like ''%{3}%''";
		sql = MessageFormat.format(sql, 
				referenceEntity.getPrimaryField().getName(), referenceNameField.getName(), referenceEntity.getName(), q);
		if (referenceEntity.containsField(EntityHelper.QuickCode) && StringUtils.isAlphanumericSpace(q)) {
			sql += MessageFormat.format(" or quickCode like ''%{0}%'' )", q);
		} else {
			sql += " )";
		}
		if (referenceEntity.containsField(EntityHelper.ModifiedOn)) {
			sql += " order by modifiedOn desc";
		}
		
		List<Object> result = resultSearch(sql, metaEntity, referenceNameField);
		writeSuccess(response, result);
	}
	
	// 搜索指定实体的指定字段
	@RequestMapping("search")
	public void search(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final String entity = getParameterNotNull(request, "entity");
		
		String q = getParameter(request, "q");
		// 为空则加载最近使用的
		if (StringUtils.isBlank(q)) {
			String type = getParameter(request, "type");
			ID[] recently = Application.getRecentlyUsedCache().gets(user, entity, type);
			if (recently.length == 0) {
				writeSuccess(response, JSONUtils.EMPTY_ARRAY);
			} else {
				writeSuccess(response, RecentlyUsedSearch.formatSelect2(recently, null));
			}
			return;
		}
		q = StringEscapeUtils.escapeSql(q);
		
		Entity metaEntity = MetadataHelper.getEntity(entity);
		Field nameField = MetadataHelper.getNameField(metaEntity);
		if (nameField == null) {
			LOG.warn("No name-field found : " + entity);
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		// 查询字段，未指定则使用名称字段和 quickCode
		String qfields = getParameter(request, "qfields");
		if (StringUtils.isBlank(qfields)) {
			qfields = nameField.getName();
			if (metaEntity.containsField(EntityHelper.QuickCode)) {
				qfields += "," + EntityHelper.QuickCode;
			}
		}
		
		List<String> or = new ArrayList<>();
		for (String field : qfields.split(",")) {
			if (!metaEntity.containsField(field)) {
				LOG.warn("No field for search : " + field);
			} else {
				or.add(field + " like '%" + q + "%'");
			}
		}
		if (or.isEmpty()) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		String sql = "select {0},{1} from {2} where ({3})";
		sql = MessageFormat.format(sql, 
				metaEntity.getPrimaryField().getName(), nameField.getName(), metaEntity.getName(), StringUtils.join(or, " or "));
		if (metaEntity.containsField(EntityHelper.ModifiedOn)) {
			sql += " order by modifiedOn desc";
		}
		
		List<Object> result = resultSearch(sql, metaEntity, nameField);
		writeSuccess(response, result);
	}
	
	// 获取记录的名称字段值
	@RequestMapping("read-labels")
	public void referenceLabel(HttpServletRequest request, HttpServletResponse response) {
		String ids = getParameter(request, "ids", null);
		if (ids == null) {
			writeSuccess(response);
			return;
		}
		
		Map<String, String> labels = new HashMap<>();
		for (String id : ids.split("\\|")) {
			if (!ID.isId(id)) {
				continue;
			}
			String label = FieldValueWrapper.getLabelNotry(ID.valueOf(id));
			labels.put(id, label);
		}
		writeSuccess(response, labels);
	}

	// 搜索分类字段
	@RequestMapping("classification")
	public void searchClassification(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final String entity = getParameterNotNull(request, "entity");
		final String field = getParameterNotNull(request, "field");

		Field fieldMeta = MetadataHelper.getField(entity, field);
		ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, false);

		String q = getParameter(request, "q");
		// 为空则加载最近使用的
		if (StringUtils.isBlank(q)) {
		    String type = entity + "." + field;
			ID[] recently = Application.getRecentlyUsedCache().gets(user, "ClassificationData", "d" + useClassification);
			if (recently.length == 0) {
				writeSuccess(response, JSONUtils.EMPTY_ARRAY);
			} else {
				writeSuccess(response, RecentlyUsedSearch.formatSelect2(recently, null));
			}
			return;
		}
		q = StringEscapeUtils.escapeSql(q);

        int openLevel = ClassificationManager.instance.getOpenLevel(fieldMeta);

		String sql = "select itemId,fullName from ClassificationData where dataId = '%s' and level = %d and fullName like '%%%s%%' order by fullName";
        sql = String.format(sql, useClassification.toLiteral(), openLevel, q);
		List<Object> result = resultSearch(sql, null, null);
		writeSuccess(response, result);
	}
	
	/**
	 * 封装查询结果
	 * 
	 * @param sql
	 * @param entity
	 * @param nameField
	 * @return
	 */
	private List<Object> resultSearch(String sql, Entity entity, Field nameField) {
		Object[][] array = Application.createQuery(sql).setLimit(10).array();
		List<Object> result = new ArrayList<>();
		for (Object[] o : array) {
			ID recordId = (ID) o[0];
			if (entity != null
                    && MetadataHelper.isBizzEntity(entity.getEntityCode())
					&& (!UserHelper.isActive(recordId) || recordId.equals(UserService.SYSTEM_USER))) {
				continue;
			}
			
			String label;
			if (o[1] == null || StringUtils.isBlank(o[1].toString())) {
				label = FieldValueWrapper.NO_LABEL_PREFIX + recordId.toLiteral().toUpperCase();
			} else {
				label = nameField == null ? o[1].toString()
                        : (String) FieldValueWrapper.instance.wrapFieldValue(o[1], nameField);
			}
			result.add(JSONUtils.toJSONObject(new String[] { "id", "text" }, new Object[] { recordId, label }));
		}
		return result;
	}
}
