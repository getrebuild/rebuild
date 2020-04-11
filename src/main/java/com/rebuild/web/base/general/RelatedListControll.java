/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
import com.rebuild.server.business.feeds.FeedsType;
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

		String sql = buildMasterSql(masterId, related, false);

		String[] e = related.split("\\.");
		Field nameField = MetadataHelper.getNameField(e[0]);

		int pn = NumberUtils.toInt(getParameter(request, "pageNo"), 1);
		int ps = NumberUtils.toInt(getParameter(request, "pageSize"), 200);
		
		Object[][] array = Application.createQuery(sql).setLimit(ps, pn * ps - ps).array();
		for (Object[] o : array) {
		    Object nameValue = o[1];
            nameValue = FieldValueWrapper.instance.wrapFieldValue(nameValue, nameField, true);
			if (nameValue == null || StringUtils.isEmpty(nameValue.toString())) {
                nameValue = FieldValueWrapper.NO_LABEL_PREFIX + o[0].toString().toUpperCase();
			}
			o[1] = nameValue;
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
        String[] relateds = getParameterNotNull(request, "relateds").split(",");
		
		Map<String, Integer> countMap = new HashMap<>();
		for (String related : relateds) {
			String sql = buildMasterSql(masterId, related, true);
			if (sql != null) {
				Object[] count = Application.createQuery(sql).unique();
				countMap.put(related, ObjectUtils.toInt(count[0]));
			}
		}
		writeSuccess(response, countMap);
	}
	
	/**
	 * @param recordOfMain
	 * @param relatedExpr
	 * @param count
	 * @return
	 */
	private String buildMasterSql(ID recordOfMain, String relatedExpr, boolean count) {
		// Entity.Field
		String[] e = relatedExpr.split("\\.");
		Entity relatedEntity = MetadataHelper.getEntity(e[0]);

		Set<String> relatedFields = new HashSet<>();

		if (e.length > 1) {
			relatedFields.add(e[1]);
		} else {
			// v1.9 之前会把所有相关的查出来
			Entity masterEntity = MetadataHelper.getEntity(recordOfMain.getEntityCode());
			for (Field field : relatedEntity.getFields()) {
				if ((field.getType() == FieldType.REFERENCE || field.getType() == FieldType.ANY_REFERENCE)
						&& ArrayUtils.contains(field.getReferenceEntities(), masterEntity)) {
					relatedFields.add(field.getName());
				}
			}
		}

		if (relatedFields.isEmpty()) {
			return null;
		}
		
		String masterWhere = "(" + StringUtils.join(relatedFields, " = ''{0}'' or ") + " = ''{0}'')";
		masterWhere = MessageFormat.format(masterWhere, recordOfMain);
		if (relatedEntity.getEntityCode() == EntityHelper.Feeds) {
			masterWhere += " and type = " + FeedsType.FOLLOWUP.getMask();
		}

		String baseSql = "select %s from " + relatedEntity.getName() + " where " + masterWhere;
		
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
