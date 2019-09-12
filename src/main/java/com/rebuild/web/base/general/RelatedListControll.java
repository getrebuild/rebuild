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

package com.rebuild.web.base.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 相关项列表
 * 
 * @author devezhao
 * @since 10/22/2018
 */
@Controller
@RequestMapping("/app/entity/")
public class RelatedListControll extends BaseControll {

	@RequestMapping("related-list")
	public void relatedList(HttpServletRequest request, HttpServletResponse response) {
		ID masterId = getIdParameterNotNull(request, "masterId");
		String related = getParameterNotNull(request, "related");
		
		Entity relatedEntity = MetadataHelper.getEntity(related);
		String sql = buildMasterSql(masterId, relatedEntity, false);
		
		int pn = NumberUtils.toInt(getParameter(request, "pageNo"), 1);
		int ps = NumberUtils.toInt(getParameter(request, "pageSize"), 200);
		
		Object[][] array = Application.createQuery(sql).setLimit(ps, pn * ps - ps).array();
		for (Object[] o : array) {
			o[1] = FieldValueWrapper.instance.wrapFieldValue(o[1], MetadataHelper.getNameField(relatedEntity));
			if (o[1] == null || StringUtils.isEmpty(o[1].toString())) {
				o[1] = o[0].toString().toUpperCase();  // 使用ID值作为名称字段值
			}
//			o[2] = CalendarUtils.getUTCDateTimeFormat().format(o[2]);
			o[2] = Moment.moment((Date) o[2]).fromNow();
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "total", "data" },
				new Object[] { 0, array });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("related-counts")
	public void relatedCounts(HttpServletRequest request, HttpServletResponse response) {
		ID masterId = getIdParameterNotNull(request, "masterId");
        String[] relates = getParameterNotNull(request, "relateds").split(",");
		
		Map<String, Integer> countMap = new HashMap<>();
		for (String related : relates) {
			String sql = buildMasterSql(masterId, MetadataHelper.getEntity(related), true);
			if (sql != null) {
				Object[] count = Application.createQuery(sql).unique();
				countMap.put(related, ObjectUtils.toInt(count[0]));
			}
		}
		writeSuccess(response, countMap);
	}
	
	/**
	 * @param recordOfMain
	 * @param relatedEntity
	 * @param count
	 * @return
	 */
	private String buildMasterSql(ID recordOfMain, Entity relatedEntity, boolean count) {
		Entity masterEntity = MetadataHelper.getEntity(recordOfMain.getEntityCode());
		Set<String> relatedFields = new HashSet<>();
		for (Field field : relatedEntity.getFields()) {
			if ((field.getType() == FieldType.REFERENCE || field.getType() == FieldType.ANY_REFERENCE)
					&& ArrayUtils.contains(field.getReferenceEntities(), masterEntity)) {
				relatedFields.add(field.getName() + " = ''{0}''");
			}
		}
		if (relatedFields.isEmpty()) {
			return null;
		}
		
		String masterSql = "(" + StringUtils.join(relatedFields, " or ") + ")";
		masterSql = MessageFormat.format(masterSql, recordOfMain);
		
		String baseSql = "select %s from " + relatedEntity.getName() + " where " + masterSql;
		
		Field primaryField = relatedEntity.getPrimaryField();
		Field namedField = MetadataHelper.getNameField(relatedEntity);
		
		if (count) {
			baseSql = String.format(baseSql, "count(" + primaryField.getName() + ")");
		} else {
			baseSql = String.format(baseSql, primaryField.getName() + "," + namedField.getName() + "," + EntityHelper.ModifiedOn);
			baseSql += " order by modifiedOn desc";
		}
		return baseSql;
	}
}
