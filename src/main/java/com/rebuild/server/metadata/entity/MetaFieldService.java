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

package com.rebuild.server.metadata.entity;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
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
		final Field field = MetadataHelper.getField((String) fieldRecord[0], (String) fieldRecord[1]);
		
		// 删除此字段的相关配置记录
		String whoUsed[] = new String[] {
				"PickList", "AutoFillinConfig"
		};
		int del = 0;
		for (String who : whoUsed) {
			Entity whoEntity = MetadataHelper.getEntity(who);
			if (!(whoEntity.containsField("belongEntity") || whoEntity.containsField("belongField"))) {
				continue;
			}
			
			String sql = String.format("select %s from %s where belongEntity = '%s' and belongField = '%s'", 
					whoEntity.getPrimaryField().getName(), whoEntity.getName(), field.getOwnEntity().getName(), field.getName());
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