/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service.entitymanage;

import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.BaseService;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/07/2018
 */
public class PickListService extends BaseService  {

	protected PickListService(PersistManagerFactory persistManagerFactory) {
		super(persistManagerFactory);
	}

	@Override
	public int getEntity() {
		return EntityHelper.PickList;
	}

	/**
	 * 保存配置
	 * 
	 * @param field
	 * @param config
	 */
	public void txBatchUpdate(Field field, JSONObject config) {
		Assert.notNull(config, "无效配置");
		ID user = Application.getCurrentCaller().get();
		
		JSONArray showItem = config.getJSONArray("show");
		JSONArray hideItem = config.getJSONArray("hide");
		
		Object[][] itemsHold = Application.createQuery(
				"select itemId from PickList where belongEntity = ? and belongField = ?")
				.setParameter(1, field.getOwnEntity().getName())
				.setParameter(2, field.getName())
				.array();
		Set<ID> itemsHoldList = new HashSet<>();
		for (Object[] o : itemsHold) {
			itemsHoldList.add((ID) o[0]);
		}
		
		for (Object o : hideItem) {
			JSONObject item = (JSONObject) o;
			String id = item.getString("id");
			if (!ID.isId(id)) {
				continue;
			}
			
			ID id2id = ID.valueOf(id);
			Record r = EntityHelper.forUpdate(id2id, user);
			r.setBoolean("isHide", true);
			r.setString("text", item.getString("text"));
			update(r);
			itemsHoldList.remove(id2id);
		}
		
		int seq = 0;
		for (Object o : showItem) {
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
			}
			createOrUpdate(r);
			
			if (id2id != null) {
				itemsHoldList.remove(id2id);
			}
		}
		
		for (ID id : itemsHoldList) {
			delete(id);
		}
	}
}
