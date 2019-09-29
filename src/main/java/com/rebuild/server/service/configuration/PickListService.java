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

package com.rebuild.server.service.configuration;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.privileges.AdminGuard;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * 下拉列表
 * 
 * @author zhaofang123@gmail.com
 * @since 09/07/2018
 */
public class PickListService extends ConfigurationService implements AdminGuard {

	protected PickListService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.PickList;
	}

	/**
	 * 保存配置
	 * 
	 * @param field
	 * @param config
	 */
	public void updateBatch(Field field, JSONObject config) {
		Assert.notNull(config, "无效配置");
		ID user = Application.getCurrentUser();
		
		JSONArray showItems = config.getJSONArray("show");
		JSONArray hideItems = config.getJSONArray("hide");
		
		Object[][] itemsHold = Application.createQueryNoFilter(
				"select itemId from PickList where belongEntity = ? and belongField = ?")
				.setParameter(1, field.getOwnEntity().getName())
				.setParameter(2, field.getName())
				.array();
		Set<ID> itemsHoldList = new HashSet<>();
		for (Object[] o : itemsHold) {
			itemsHoldList.add((ID) o[0]);
		}
		
		if (hideItems != null) {
			for (Object o : hideItems) {
				JSONObject item = (JSONObject) o;
				String id = item.getString("id");
				if (!ID.isId(id)) {
					continue;
				}
				
				ID id2id = ID.valueOf(id);
				Record r = EntityHelper.forUpdate(id2id, user);
				r.setBoolean("isHide", true);
				r.setString("text", item.getString("text"));
				super.update(r);
				itemsHoldList.remove(id2id);
				cleanCache(id2id);
			}
		}

		// MultiSelect 专用
		long nextMaskValue = 0;
		if (EasyMeta.getDisplayType(field) == DisplayType.MULTISELECT) {
			Object[] max = Application.createQueryNoFilter(
					"select max(maskValue) from PickList where belongEntity = ? and belongField = ?")
					.setParameter(1, field.getOwnEntity().getName())
					.setParameter(2, field.getName())
					.unique();
			nextMaskValue = ObjectUtils.toLong(max[0]);
			if (nextMaskValue < 1) {
				nextMaskValue = 1;
			} else {
				nextMaskValue *= 2;
			}
		}

		int seq = 0;
		for (Object o : showItems) {
			JSONObject item = (JSONObject) o;
			String id = item.getString("id");
			ID id2id = ID.isId(id) ? ID.valueOf(id) : null;
			
			Record r = id2id == null 
					? EntityHelper.forNew(EntityHelper.PickList, user) : EntityHelper.forUpdate(id2id, user);
			r.setInt("seq", seq++);
			r.setString("text", item.getString("text"));
			r.setBoolean("isHide", false);
			r.setBoolean("isDefault", item.getBoolean("default"));
			if (id2id == null) {
				r.setString("belongEntity", field.getOwnEntity().getName());
				r.setString("belongField", field.getName());

				if (nextMaskValue > 0) {
					r.setLong("maskValue", nextMaskValue);
					nextMaskValue *= 2;
				}
			}
			super.createOrUpdate(r);
			
			if (id2id != null) {
				itemsHoldList.remove(id2id);
				cleanCache(id2id);
			}
		}
		
		for (ID item : itemsHoldList) {
			cleanCache(item);
			super.delete(item);
		}
		PickListManager.instance.clean(field);
	}

    /**
     * @param field
     * @param config
     */
	public void updateBatchMultiSelect(Field field, JSONObject config) {
		this.updateBatch(field, config);
    }
	
	@Override
	protected void cleanCache(ID configId) {
		PickListManager.instance.clean(configId);
	}
}
