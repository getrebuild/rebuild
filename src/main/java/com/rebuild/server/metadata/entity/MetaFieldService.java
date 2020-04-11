/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.metadata.entity;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class MetaFieldService extends BaseService implements AdminGuard {

	protected MetaFieldService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.MetaField;
	}
	
	@Override
	public int delete(ID recordId) {
		Object[] fieldRecord = getPMFactory().createQuery(
				"select belongEntity,fieldName from MetaField where fieldId = ?")
				.setParameter(1, recordId)
				.unique();
		Field field = null;
		try {
			field = MetadataHelper.getField((String) fieldRecord[0], (String) fieldRecord[1]);
		} catch (MetadataException ignored) {
		}

		// 删除此字段的相关配置记录
		// Field: belongEntity, belongField
		String[] whoUsed = field == null ? new String[0] : new String[] {
				"PickList", "AutoFillinConfig"
		};
		int del = 0;
		for (String who : whoUsed) {
			Entity whichEntity = MetadataHelper.getEntity(who);
			if (!whichEntity.containsField("belongEntity") || !whichEntity.containsField("belongField")) {
				continue;
			}

			String sql = String.format("select %s from %s where belongEntity = '%s' and belongField = '%s'",
					whichEntity.getPrimaryField().getName(), whichEntity.getName(), field.getOwnEntity().getName(), field.getName());
			Object[][] usedArray = getPMFactory().createQuery(sql).array();
			for (Object[] used : usedArray) {
				del += super.delete((ID) used[0]);
			}
			if (usedArray.length > 0) {
				LOG.warn("deleted configuration of field [ " + field.getOwnEntity().getName() + "." + field.getName() + " ] in [ " + who + " ] : " + usedArray.length);
			}
		}

		del += super.delete(recordId);
		return del;
	}
}