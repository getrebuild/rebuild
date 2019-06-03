/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.configuration;

import com.rebuild.server.configuration.portals.BaseLayoutManager;
import com.rebuild.server.configuration.portals.FormsManager;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/30
 */
public class LayoutConfigService extends CleanableCacheService {

	protected LayoutConfigService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	protected void cleanCache(ID configId) {
		Object[] o = this.getPMFactory().createQuery(
				"select belongEntity,applyType from LayoutConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (o == null || !MetadataHelper.containsEntity((String) o[0])) {
			return;
		}
		
		Entity entity = MetadataHelper.getEntity((String) o[0]);
		if (BaseLayoutManager.TYPE_FORM.equals(o[1])) {
			FormsManager.instance.clean(entity);
		}
	}
}
