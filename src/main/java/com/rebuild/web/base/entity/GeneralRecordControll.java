/*
rebuild - Building your system freely.
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

package com.rebuild.web.base.entity;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.privileges.User;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.base.GeneralEntityService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.InvalidRequestException;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 记录操作
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
@Controller
@RequestMapping("/app/entity/")
public class GeneralRecordControll extends BaseControll {

	@RequestMapping("record-save")
	public void save(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		if (record.getPrimary() == null) {
			if (!Application.getSecurityManager().allowedC(user, record.getEntity().getEntityCode())) {
				writeFailure(response, "没有新建权限");
				return;
			}
		} else if (!Application.getSecurityManager().allowedU(user, record.getPrimary())) {
			writeFailure(response, "你没有编辑此记录的权限");
			return;
		}
		
		record = Application.getGeneralEntityService(record.getEntity().getEntityCode()).createOrUpdate(record);
		
		Map<String, Object> map = new HashMap<>();
		map.put("id", record.getPrimary());
		writeSuccess(response, map);
	}
	
	@RequestMapping("record-delete")
	public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		final ID[] ids = parseIdList(request);
		if (ids.length == 0) {
			writeFailure(response, "没有要删除的记录");
			return;
		}
		
		final ID firstId = ids[0];
		
		Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		if (!Application.getSecurityManager().allowedD(user, entity.getEntityCode())) {
			writeFailure(response, "没有删除权限");
			return;
		}
		if (ids.length == 1 && !Application.getSecurityManager().allowedD(user, firstId)) {
			writeFailure(response, "你没有删除此记录的权限");
			return;
		}
		
		String[] cascades = parseCascades(request);
		GeneralEntityService ges = Application.getGeneralEntityService(entity.getEntityCode());
		
		int affected = 0;
		if (ids.length == 1) {
			affected = ges.delete(firstId, cascades);
		} else {
			BulkContext context = new BulkContext(user, BizzPermission.DELETE, null, cascades, ids);
			affected = ges.bulk(context);
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "deleted", "requests" },
				new Object[] { affected, ids.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-assign")
	public void assign(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		final ID[] ids = parseIdList(request);
		if (ids.length == 0) {
			writeFailure(response, "没有要分派的记录");
			return;
		}
		
		final ID firstId = ids[0];
		
		Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		if (!Application.getSecurityManager().allowedA(user, entity.getEntityCode())) {
			writeFailure(response, "没有分派权限");
			return;
		}
		if (ids.length == 1 && !Application.getSecurityManager().allowedA(user, firstId)) {
			writeFailure(response, "你没有分派此记录的权限");
			return;
		}
		
		ID assignTo = getIdParameterNotNull(request, "to");
		String[] cascades = parseCascades(request);
		GeneralEntityService ges = Application.getGeneralEntityService(entity.getEntityCode());
		
		int affected = 0;
		if (ids.length == 1) {
			affected = ges.assign(firstId, assignTo, cascades);
		} else {
			BulkContext context = new BulkContext(user, BizzPermission.ASSIGN, assignTo, cascades, ids);
			affected = ges.bulk(context);
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "shared", "requests" },
				new Object[] { affected, ids.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-share")
	public void share(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		final ID[] ids = parseIdList(request);
		if (ids.length == 0) {
			writeFailure(response, "没有要共享的记录");
			return;
		}
		
		final ID firstId = ids[0];
		
		Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		if (!Application.getSecurityManager().allowedA(user, entity.getEntityCode())) {
			writeFailure(response, "没有共享权限");
			return;
		}
		if (ids.length == 1 && !Application.getSecurityManager().allowedS(user, firstId)) {
			writeFailure(response, "你没有共享此记录的权限");
			return;
		}
		
		ID shareTo = getIdParameterNotNull(request, "to");
		String[] cascades = parseCascades(request);
		GeneralEntityService ges = Application.getGeneralEntityService(entity.getEntityCode());
		
		int affected = 0;
		if (ids.length == 1) {
			affected = ges.share(firstId, shareTo, cascades);
		} else {
			BulkContext context = new BulkContext(user, BizzPermission.SHARE, shareTo, cascades, ids);
			affected = ges.bulk(context);
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "shared", "requests" },
				new Object[] { affected, ids.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-meta")
	public void fetchRecordMeta(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID id = getIdParameterNotNull(request, "id");
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		
		if (!EntityHelper.hasPrivilegesField(entity)) {
			writeFailure(response, "无权限实体");
			return;
		}
		
		String sql = "select createdOn,modifiedOn,owningUser from %s where %s = '%s'";
		sql = String.format(sql, entity.getName(), entity.getPrimaryField().getName(), id);
		Object[] recordMeta = Application.getQueryFactory().createQueryNoFilter(sql).unique();
		
		recordMeta[0] = CalendarUtils.getUTCDateTimeFormat().format(recordMeta[0]);
		recordMeta[1] = CalendarUtils.getUTCDateTimeFormat().format(recordMeta[1]);
		
		User owning = Application.getUserStore().getUser((ID) recordMeta[2]);
		recordMeta[2] = new Object[] { owning.getId().toLiteral(), owning.getFullName() };
	
		Object[] shareTo = Application.createQuery(
				"select count(accessId) from ShareAccess where entity = ? and recordId = ?")
				.setParameter(1, entity.getEntityCode())
				.setParameter(2, id)
				.unique();
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "createdOn", "modifiedOn", "owningUser", "shareTo" },
				new Object[] { recordMeta[0], recordMeta[1], recordMeta[2], shareTo[0] });
		writeSuccess(response, ret);
	}
	
	/**
	 * 操作ID列表
	 * 
	 * @param request
	 * @return
	 */
	private ID[] parseIdList(HttpServletRequest request) {
		String ids = getParameterNotNull(request, "id");
		Set<ID> idList = new HashSet<>();
		int sameEntityCode = 0;
		for (String id : ids.split(",")) {
			ID id0 = ID.valueOf(id);
			if (sameEntityCode == 0) {
				sameEntityCode = id0.getEntityCode();
			}
			if (sameEntityCode != id0.getEntityCode()) {
				throw new InvalidRequestException("只能批量删除同一实体的记录");
			}
			idList.add(ID.valueOf(id));
		}
		return idList.toArray(new ID[idList.size()]);
	}
	
	/**
	 * 级联操作实体
	 * 
	 * @param request
	 * @return
	 */
	private String[] parseCascades(HttpServletRequest request) {
		String cascades = getParameter(request, "cascades");
		if (StringUtils.isBlank(cascades)) {
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}
		String[] cass = cascades.split(",");
		
		// TODO 验证实体???
		
		return cass;
	}
}
