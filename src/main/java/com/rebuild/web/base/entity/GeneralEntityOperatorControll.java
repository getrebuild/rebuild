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

package com.rebuild.web.base.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.IllegalParameterException;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
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
public class GeneralEntityOperatorControll extends BaseControll {

	@RequestMapping("record-save")
	public void save(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		
		Record record = null;
		try {
			record = EntityHelper.parse((JSONObject) formJson, user);
		} catch (DataSpecificationException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		// TODO 检查不可重复字段值
		
		try {
			record = Application.getEntityService(record.getEntity().getEntityCode()).createOrUpdate(record);
		} catch (AccessDeniedException | DataSpecificationException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
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
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		String[] cascades = parseCascades(request);
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected = 0;
		try {
			if (ids.length == 1) {
				affected = ies.delete(firstId, cascades);
			} else {
				BulkContext context = new BulkContext(user, BizzPermission.DELETE, null, cascades, ids);
				affected = ies.bulk(context);
			}
		} catch (AccessDeniedException | DataSpecificationException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
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
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		ID assignTo = getIdParameterNotNull(request, "to");
		String[] cascades = parseCascades(request);
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected = 0;
		try {
			// 仅涉及一条记录
			if (ids.length == 1 && cascades.length == 0) {
				affected = ies.assign(firstId, assignTo, cascades);
			} else {
				BulkContext context = new BulkContext(user, BizzPermission.ASSIGN, assignTo, cascades, ids);
				affected = ies.bulk(context);
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "assigned", "requests" },
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
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		ID shareTo = getIdParameterNotNull(request, "to");
		String[] cascades = parseCascades(request);
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected = 0;
		try {
			// 仅涉及一条记录
			if (ids.length == 1 && cascades.length == 0) {
				affected = ies.share(firstId, shareTo, cascades);
			} else {
				BulkContext context = new BulkContext(user, BizzPermission.SHARE, shareTo, cascades, ids);
				affected = ies.bulk(context);
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "shared", "requests" },
				new Object[] { affected, ids.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-unshare")
	public void unshare(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		final ID record = getIdParameterNotNull(request, "record");  // Record ID
		final ID[] ids = parseIdList(request);  // ShareAccess IDs
		if (ids.length == 0) {
			writeFailure(response, "没有要取消共享的记录");
			return;
		}
		
		final ID firstId = ids[0];
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected = 0;
		try {
			if (ids.length == 1) {
				affected = ies.unshare(record, ids[0]);
			} else {
				BulkContext context = new BulkContext(user, EntityService.UNSHARE, ids, record);
				affected = ies.bulk(context);
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "unshared", "requests" },
				new Object[] { affected, ids.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-meta")
	public void fetchRecordMeta(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID id = getIdParameterNotNull(request, "id");
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		
		String sql = "select createdOn,modifiedOn from %s where %s = '%s'";
		if (EntityHelper.hasPrivilegesField(entity)) {
			sql = sql.replaceFirst("modifiedOn", "modifiedOn,owningUser");
		}
		
		sql = String.format(sql, entity.getName(), entity.getPrimaryField().getName(), id);
		Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
		if (recordMeta == null) {
			writeFailure(response, "记录不存在");
			return;
		}
		
		recordMeta[0] = Moment.moment((Date) recordMeta[0]).fromNow();
		recordMeta[1] = Moment.moment((Date) recordMeta[1]).fromNow();
		
		String[] owning = null;
		List<String[]> sharingList = null;
		if (recordMeta.length == 3) {
			User user = Application.getUserStore().getUser((ID) recordMeta[2]);
			String dept = user.getOwningDept() == null ? null : user.getOwningDept().getName();
			owning = new String[] { user.getIdentity().toString(), user.getFullName(), user.getAvatarUrl(true), dept };
			
			Object[][] shareTo = Application.createQueryNoFilter(
					"select shareTo from ShareAccess where belongEntity = ? and recordId = ?")
					.setParameter(1, entity.getName())
					.setParameter(2, id)
					.setLimit(9)
					.array();
			sharingList = new ArrayList<>();
			for (Object[] st : shareTo) {
				String[] shows2 = UserHelper.getShows((ID) st[0]);
				shows2 = new String[] { st[0].toString(), shows2[0], shows2[1] };
				sharingList.add(shows2);
			}
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "createdOn", "modifiedOn", "owningUser", "sharingList" },
				new Object[] { recordMeta[0], recordMeta[1], owning, sharingList });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("sharing-list")
	public void fetchSharingList(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		
		Object[][] array = Application.createQueryNoFilter(
				"select shareTo,accessId,createdOn,createdBy from ShareAccess where belongEntity = ? and recordId = ?")
				.setParameter(1, entity.getName())
				.setParameter(2, id)
				.array();
		for (Object[] o : array) {
			o[0] = UserHelper.getShows((ID) o[0]);
			o[2] = CalendarUtils.getUTCDateTimeFormat().format(o[2]);
			o[3] = UserHelper.getShows((ID) o[3]);
		}
		writeSuccess(response, array);
	}
	
	/**
	 * 操作 ID 列表
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
				throw new IllegalParameterException("只能批量删除同一实体的记录");
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
		
		List<String> casList = new ArrayList<>();
		for (String c : cascades.split(",")) {
			if (MetadataHelper.containsEntity(c)) {
				casList.add(c);
			} else {
				LOG.warn("Unknow entity in cascades : " + c);
			}
		}
		return casList.toArray(new String[casList.size()]);
	}
}
