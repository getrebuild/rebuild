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

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.ServiceSpec;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.IllegalParameterException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 记录操作
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
@Controller
@RequestMapping("/app/entity/")
public class GeneralOperatingControll extends BaseControll {

	// 重复字段值
	public static final int CODE_REPEATED_VALUES = 499;

	@RequestMapping("record-save")
	public void save(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		
		Record record;
		try {
			record = EntityHelper.parse((JSONObject) formJson, user);
		} catch (DataSpecificationException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}

		List<Record> repeated = Application.getGeneralEntityService().checkRepeated(record);
		if (!repeated.isEmpty()) {
			JSONObject map = new JSONObject();
			map.put("error_code", CODE_REPEATED_VALUES);
			map.put("error_msg", "存在重复值");
			map.put("data", buildRepeatedData(repeated));
			writeJSON(response, map);
			return;
		}

		try {
			record = Application.getService(record.getEntity().getEntityCode()).createOrUpdate(record);
		} catch (AccessDeniedException | DataSpecificationException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		Map<String, Object> map = new HashMap<>();
		map.put("id", record.getPrimary());
		writeSuccess(response, map);
	}
	
	@RequestMapping("record-delete")
	public void delete(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final ID[] records = parseIdList(request);
		if (records.length == 0) {
			writeFailure(response, "没有要删除的记录");
			return;
		}
		
		final ID firstId = records[0];
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		String[] cascades = parseCascades(request);
		ServiceSpec ies = Application.getService(entity.getEntityCode());
		
		int affected;
		try {
			if (!EntityService.class.isAssignableFrom(ies.getClass())) {
				affected = ies.delete(firstId);
			} else if (records.length == 1) {
				affected = ((EntityService) ies).delete(firstId, cascades);
			} else {
				BulkContext context = new BulkContext(user, BizzPermission.DELETE, null, cascades, records);
				affected = ((EntityService) ies).bulk(context);
			}
		} catch (AccessDeniedException | DataSpecificationException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "deleted", "requests" },
				new Object[] { affected, records.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-assign")
	public void assign(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final ID[] records = parseIdList(request);
		if (records.length == 0) {
			writeFailure(response, "没有要分派的记录");
			return;
		}
		
		final ID firstId = records[0];
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		ID assignTo = getIdParameterNotNull(request, "to");
		String[] cascades = parseCascades(request);
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected;
		try {
			// 仅一条记录
			if (records.length == 1) {
				affected = ies.assign(firstId, assignTo, cascades);
			} else {
				BulkContext context = new BulkContext(user, BizzPermission.ASSIGN, assignTo, cascades, records);
				affected = ies.bulk(context);
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "assigned", "requests" },
				new Object[] { affected, records.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-share")
	public void share(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final ID[] records = parseIdList(request);
		if (records.length == 0) {
			writeFailure(response, "没有要共享的记录");
			return;
		}
		
		final ID[] toUsers = parseUserList(request);
		if (toUsers.length == 0) {
			writeFailure(response, "没有要共享的用户");
			return;
		}
		
		final ID firstId = records[0];
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		String[] cascades = parseCascades(request);
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected = 0;
		try {
			for (ID to : toUsers) {
				// 一条记录
				if (records.length == 1) {
					affected += ies.share(firstId, to, cascades);
				} else {
					BulkContext context = new BulkContext(user, BizzPermission.SHARE, to, cascades, records);
					affected += ies.bulk(context);
				}
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "shared", "requests" },
				new Object[] { affected, records.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-unshare")
	public void unsharesa(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final ID record = getIdParameterNotNull(request, "record");  // Record ID
		final ID[] accessIds = parseIdList(request);  // ShareAccess IDs
		if (accessIds.length == 0) {
			writeFailure(response, "没有要取消共享的记录");
			return;
		}
		
		final ID firstId = accessIds[0];
		final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
		
		EntityService ies = Application.getEntityService(entity.getEntityCode());
		
		int affected;
		try {
			if (accessIds.length == 1) {
				affected = ies.unshare(record, accessIds[0]);
			} else {
				BulkContext context = new BulkContext(user, EntityService.UNSHARE, accessIds, record);
				affected = ies.bulk(context);
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "unshared", "requests" },
				new Object[] { affected, accessIds.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-unshare-batch")
	public void unshareBatch(HttpServletRequest request, HttpServletResponse response) {
		final ID user = getRequestUser(request);
		final ID[] records = parseIdList(request);
		if (records.length == 0) {
			writeFailure(response, "没有要取消共享的记录");
			return;
		}
		
		// 查询共享记录ID
		String sql = String.format(
				"select recordId,accessId from ShareAccess where recordId in ('%s')", StringUtils.join(records, "','"));
		String to = getParameterNotNull(request, "to");
		if (!"$ALL$".equals(to)) {
			ID[] toUsers = parseUserList(request);
			if (toUsers.length == 0) {
				writeFailure(response, "没有要取消共享的用户");
				return;
			}
			sql += String.format(" and shareTo in ('%s')", StringUtils.join(toUsers, "','"));
		}
		
		Object[][] accessArray = Application.createQueryNoFilter(sql).array();
		if (accessArray.length == 0) {
			JSON ret = JSONUtils.toJSONObject(
					new String[] { "unshared", "requests" },
					new Object[] { 0, 0 });
			writeSuccess(response, ret);
			return;
		}
		
		Map<ID, Set<ID>> accessMap = new HashMap<>();
		for (Object[] o : accessArray) {
			ID record = (ID) o[0];
			Set<ID> access = accessMap.computeIfAbsent(record, k -> new HashSet<>());
			access.add((ID) o[1]);
		}
		
		EntityService ies = Application.getEntityService(records[0].getEntityCode());
		
		int affected = 0;
		try {
			for (Map.Entry<ID, Set<ID>> e : accessMap.entrySet()) {
				ID record = e.getKey();
				Set<ID> access = e.getValue();
				BulkContext context = new BulkContext(
						user, EntityService.UNSHARE, access.toArray(new ID[0]), record);
				affected += ies.bulk(context);
			}
		} catch (AccessDeniedException know) {
			writeFailure(response, know.getLocalizedMessage());
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "unshared", "requests" },
				new Object[] { affected, records.length });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-meta")
	public void fetchRecordMeta(HttpServletRequest request, HttpServletResponse response) {
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
			owning = new String[] { user.getIdentity().toString(), user.getFullName(), dept };
			
			Object[][] shareTo = Application.createQueryNoFilter(
					"select shareTo from ShareAccess where belongEntity = ? and recordId = ?")
					.setParameter(1, entity.getName())
					.setParameter(2, id)
					.setLimit(9)
					.array();
			sharingList = new ArrayList<>();
			for (Object[] st : shareTo) {
				sharingList.add(new String[] { st[0].toString(), UserHelper.getName((ID) st[0]) });
			}
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "createdOn", "modifiedOn", "owningUser", "sharingList" },
				new Object[] { recordMeta[0], recordMeta[1], owning, sharingList });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("record-lastModified")
	public void fetchRecordLastModified(HttpServletRequest request, HttpServletResponse response) {
		final ID id = getIdParameterNotNull(request, "id");
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		
		String sql = String.format("select modifiedOn from %s where %s = '%s'",
				entity.getName(), entity.getPrimaryField().getName(), id);
		Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
		if (recordMeta == null) {
			writeFailure(response, "NO_EXISTS");
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "lastModified" },
				new Object[] { ((Date) recordMeta[0]).getTime() });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("shared-list")
	public void fetchSharedList(HttpServletRequest request, HttpServletResponse response) {
		ID id = getIdParameterNotNull(request, "id");
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		
		Object[][] array = Application.createQueryNoFilter(
				"select shareTo,accessId,createdOn,createdBy from ShareAccess where belongEntity = ? and recordId = ?")
				.setParameter(1, entity.getName())
				.setParameter(2, id)
				.array();
		for (Object[] o : array) {
			o[0] = new String[] { o[0].toString(), UserHelper.getName((ID) o[0]) };
			o[2] = CalendarUtils.getUTCDateTimeFormat().format(o[2]);
			o[3] = UserHelper.getName((ID) o[3]);
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
			if (!ID.isId(id)) continue;
			ID id0 = ID.valueOf(id);
			if (sameEntityCode == 0) {
				sameEntityCode = id0.getEntityCode();
			}
			if (sameEntityCode != id0.getEntityCode()) {
				throw new IllegalParameterException("只能批量删除同一实体的记录");
			}
			idList.add(ID.valueOf(id));
		}
		return idList.toArray(new ID[0]);
	}
	
	/**
	 * 用户列表
	 * 
	 * @param request
	 * @return
	 */
	private ID[] parseUserList(HttpServletRequest request) {
		String to = getParameterNotNull(request, "to");
		Set<ID> toList = new HashSet<>();
		for (String id : to.split(",")) {
			if (ID.isId(id)) {
				ID uid = ID.valueOf(id);
				if (uid.getEntityCode() != EntityHelper.User) {
					throw new IllegalParameterException("无效用户:" + uid);
				}
				toList.add(uid);
			}
		}
		return toList.toArray(new ID[0]);
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
		return casList.toArray(new String[0]);
	}

	/**
	 * 转成二维数组（首行为字段名，首列为ID）
	 *
	 * @param records
	 * @return
	 */
	private JSON buildRepeatedData(List<Record> records) {
		Entity entity = records.get(0).getEntity();

		// 准备字段
		List<String> fields = new ArrayList<>();
		fields.add(entity.getPrimaryField().getName());
		for (Record r : records) {
			for (Iterator<String> iter = r.getAvailableFieldIterator(); iter.hasNext(); ) {
				String field = iter.next();
				if (!fields.contains(field)) {
					fields.add(field);
				}
			}
		}

		JSONArray fieldsJson = new JSONArray();
		for (String field : fields) {
			fieldsJson.add(EasyMeta.getLabel(entity.getField(field)));
		}

		JSONArray data = new JSONArray();
		data.add(fieldsJson);

		for (Record r : records) {
			JSONArray valuesJson = new JSONArray();
			for (String field : fields) {
				Object value = r.getObjectValue(field);
				value = FieldValueWrapper.instance.wrapFieldValue(value, entity.getField(field));
				valuesJson.add(value);
			}
			data.add(valuesJson);
		}
		return data;
	}
}
