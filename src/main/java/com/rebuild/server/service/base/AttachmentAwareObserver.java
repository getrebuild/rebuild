/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.business.files.FilesHelper;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 更新媒体字段到附件表
 * 
 * @author devezhao
 * @since 12/25/2018
 */
public class AttachmentAwareObserver extends OperatingObserver {
	
	public AttachmentAwareObserver() {
		super();
	}
	
	@Override
	public void onCreate(OperatingContext context) {
		Record record = context.getAfterRecord();
		Field[] fileFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
		if (fileFields.length == 0) return;

		List<Record> creates = new ArrayList<>();
		for (Field field : fileFields) {
			if (record.hasValue(field.getName(), false)) {
				JSONArray filesJson = parseFilesJson(record.getString(field.getName()));
				for (Object file : filesJson) {
					Record add = createAttachment(
					        field, context.getAfterRecord().getPrimary(), (String) file, context.getOperator());
					creates.add(add);
				}
			}
		}
		if (creates.isEmpty()) return;

		Application.getCommonService().createOrUpdate(creates.toArray(new Record[0]), false);
	}
	
	@Override
	public void onUpdate(OperatingContext context) {
		Record record = context.getAfterRecord();
		Field[] fileFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
		if (fileFields.length == 0) return;

		Record before = context.getBeforeRecord();
		
		List<Record> creates = new ArrayList<>();
		List<ID> deletes = new ArrayList<>();
		for (Field field : fileFields) {
			String fieldName = field.getName();
			if (record.hasValue(fieldName)) {
				JSONArray beforeFiles = parseFilesJson(before.getString(fieldName));  // 修改前
				JSONArray afterFiles = parseFilesJson(record.getString(fieldName));	  // 修改后
				
				for (Iterator<Object> iter = afterFiles.iterator(); iter.hasNext(); ) {
					Object a = iter.next();
					if (beforeFiles.contains(a)) {
						beforeFiles.remove(a);
						iter.remove();
					}
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("Remove ... " + beforeFiles);
					LOG.debug("Add ... " + afterFiles);
				}
				
				for (Object o : beforeFiles) {
					Object[] delete = Application.createQueryNoFilter(
							"select attachmentId from Attachment where belongEntity = ? and belongField = ? and filePath = ?")
							.setParameter(1, field.getOwnEntity().getEntityCode())
							.setParameter(2, fieldName)
							.setParameter(3, o)
							.unique();
					if (delete != null) {
						deletes.add((ID) delete[0]);
					}
				}
				
				for (Object o : afterFiles) {
					Record add = createAttachment(
					        field, context.getAfterRecord().getPrimary(), (String) o, context.getOperator());
					creates.add(add);
				}
			}
		}
		if (creates.isEmpty() && deletes.isEmpty()) return;

		Application.getCommonService().createOrUpdateAndDelete(
				creates.toArray(new Record[0]), deletes.toArray(new ID[0]), false);
	}
	
	@Override
	public void onDelete(OperatingContext context) {
		Record record = context.getBeforeRecord();
		Field[] fileFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
		if (fileFields.length == 0) return;

		Object[][] array = Application.createQueryNoFilter(
				"select attachmentId from Attachment where relatedRecord = ?")
				.setParameter(1, record.getPrimary())
				.array();
		if (array.length == 0) return;

		// 回收站开启，不物理删除附件
		final boolean rbEnable = SysConfiguration.getInt(ConfigurableItem.RecycleBinKeepingDays) > 0;

		List<Record> updates = new ArrayList<>();
		List<ID> deletes = new ArrayList<>();
		for (Object[] o : array) {
			if (rbEnable) {
				Record upt = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
				upt.setBoolean(EntityHelper.IsDeleted, true);
				updates.add(upt);
			} else {
				deletes.add((ID) o[0]);
			}
		}

		Application.getCommonService().createOrUpdateAndDelete(
				updates.toArray(new Record[0]), deletes.toArray(new ID[0]), false);
	}

	private JSONArray parseFilesJson(String files) {
		if (StringUtils.isBlank(files)) {
			return JSONUtils.EMPTY_ARRAY;
		}
		return JSON.parseArray(files);
	}

	private Record createAttachment(Field field, ID recordId, String filePath, ID user) {
		Record attach = FilesHelper.createAttachment(filePath, user);
		attach.setInt("belongEntity", field.getOwnEntity().getEntityCode());
		attach.setString("belongField", field.getName());
		attach.setID("relatedRecord", recordId);
		return attach;
	}
}