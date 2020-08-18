/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.metadata.entity;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.bizz.privileges.AdminGuard;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class MetaEntityService extends BaseService implements AdminGuard {
	
	private static final Log LOG = LogFactory.getLog(MetaEntityService.class);

	protected MetaEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.MetaEntity;
	}
	
	@Override
	public int delete(ID recordId) {
		Object[] entityRecord = getPMFactory().createQuery(
				"select entityName from MetaEntity where entityId = ?")
				.setParameter(1, recordId)
				.unique();
		final Entity entity = MetadataHelper.getEntity((String) entityRecord[0]);
		
		// 删除此实体的相关配置记录
		// Field: belongEntity
		String[] whoUsed = new String[] {
				"MetaField", "PickList", "LayoutConfig", "FilterConfig", "ShareAccess", "ChartConfig", 
				"Attachment", "AutoFillinConfig", "RobotTriggerConfig", "RobotApprovalConfig",
				"DataReportConfig",
		};
		int del = 0;
		for (String who : whoUsed) {
			Entity whichEntity = MetadataHelper.getEntity(who);
			if (!whichEntity.containsField("belongEntity")) {
				continue;
			}

			String ql = String.format("select %s from %s where belongEntity = '%s'",
					whichEntity.getPrimaryField().getName(), whichEntity.getName(), entity.getName());
			if (whichEntity.getEntityCode() == EntityHelper.Attachment) {
				ql = ql.split(" belongEntity ")[0] + " belongEntity = " + whichEntity.getEntityCode();
			}

			Object[][] usedArray = getPMFactory().createQuery(ql).array();
			for (Object[] used : usedArray) {
				if ("MetaField".equalsIgnoreCase(who)) {
					del += Application.getBean(MetaFieldService.class).delete((ID) used[0]);
				} else {
					del += super.delete((ID) used[0]);
				}
			}
			if (usedArray.length > 0) {
				LOG.warn("deleted configuration of entity [ " + entity.getName() + " ] in [ " + who + " ] : " + usedArray.length);
			}
		}
		
		del += super.delete(recordId);
		return del;
	}
}
