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
		Field[] attFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
		if (attFields.length == 0) {
			return;
		}
		
		List<Record> createWill = new ArrayList<>();
		for (Field field : attFields) {
			if (record.hasValue(field.getName())) {
				JSONArray filesJson = parseFilesJson(record.getString(field.getName()));
				for (Object file : filesJson) {
					Record att = createAttachment(
					        field, context.getAfterRecord().getPrimary(), (String) file, context.getOperator());
					createWill.add(att);
				}
			}
		}
		if (createWill.isEmpty()) {
			return;
		}
		
		Application.getCommonService().createOrUpdate(createWill.toArray(new Record[0]), false);
	}
	
	@Override
	public void onUpdate(OperatingContext context) {
		Record record = context.getAfterRecord();
		Field[] attFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
		if (attFields.length == 0) {
			return;
		}
		
		Record before = context.getBeforeRecord();
		
		List<Record> createWill = new ArrayList<>();
		List<ID> deleteWill = new ArrayList<>();
		for (Field field : attFields) {
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
						deleteWill.add((ID) delete[0]);
					}
				}
				
				for (Object o : afterFiles) {
					Record att = createAttachment(
					        field, context.getAfterRecord().getPrimary(), (String) o, context.getOperator());
					createWill.add(att);
				}
			}
		}
		if (createWill.isEmpty() && deleteWill.isEmpty()) {
			return;
		}
		
		Application.getCommonService().createOrUpdateAndDelete(
				createWill.toArray(new Record[0]), deleteWill.toArray(new ID[0]), false);
	}
	
	@Override
	public void onDelete(OperatingContext context) {
		Record record = context.getBeforeRecord();
		Field[] attFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.FILE, DisplayType.IMAGE);
		if (attFields.length == 0) {
			return;
		}
		
		Object[][] array = Application.createQueryNoFilter(
				"select attachmentId from Attachment where relatedRecord = ?")
				.setParameter(1, record.getPrimary())
				.array();
		if (array.length == 0) {
			return;
		}

		// 回收站开启，不物理删除附件
		final boolean rbEnable = SysConfiguration.getInt(ConfigurableItem.RecycleBinKeepingDays) > 0;

		List<Record> updateWill = new ArrayList<>();
		List<ID> deleteWill = new ArrayList<>();
		for (Object[] o : array) {
			if (rbEnable) {
				Record u = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
				u.setBoolean(EntityHelper.IsDeleted, true);
				updateWill.add(u);
			} else {
				deleteWill.add((ID) o[0]);
			}
		}

		Application.getCommonService().createOrUpdateAndDelete(
				updateWill.toArray(new Record[0]), deleteWill.toArray(new ID[0]), false);
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