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

package com.rebuild.server.business.rbstore;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.ClassificationManager;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.ModifiyMetadataException;
import com.rebuild.server.service.configuration.ClassificationService;
import org.apache.commons.lang.StringUtils;

/**
 * 导入分类数据
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 */
public class ClassificationImporter extends HeavyTask<Integer> {
	
	final private ID user;
	final private ID dest;
	final private String fileUrl;
	
	private boolean forceClean = false;
	
	private int affected = 0;
	
	/**
	 * @param user
	 * @param dest
	 * @param fileUrl
	 */
	public ClassificationImporter(ID user, ID dest, String fileUrl) {
		this.user = user;
		this.dest = dest;
		this.fileUrl = fileUrl;
	}
	
	public void setForceClean(boolean forceClean) {
		this.forceClean = forceClean;
	}
	
	@Override
	public Integer exec() throws Exception {
		final JSONArray data = (JSONArray) RBStore.fetchClassification(fileUrl);
		
		Object[][] exists = Application.createQueryNoFilter(
				"select itemId from ClassificationData where dataId = ?")
				.setParameter(1, dest)
				.array();
		if (exists.length > 0) {
			if (this.forceClean) {
				String delSql = String.format("delete from `%s` where `DATA_ID` = '%s'",
						MetadataHelper.getEntity(EntityHelper.ClassificationData).getPhysicalName(), dest);
				Application.getSQLExecutor().execute(delSql);
				for (Object[] o : exists) {
					ClassificationManager.instance.clean((ID) o[0]);
				}
			} else {
				throw new ModifiyMetadataException("必须清空当前分类数据才能导入");
			}
		}
		
		this.setThreadUser(user);
		this.setTotal(data.size());
		
		for (Object o : data) {
			addNItem((JSONObject) o, null, 0);
			this.addCompleted();
		}
		return affected;
	}

	private ID addNItem(JSONObject node, ID parent, int level) {
		String code = node.getString("code");
		String name = node.getString("name");
		
		Record item = EntityHelper.forNew(EntityHelper.ClassificationData, this.user);
		item.setString("name", name);
		if (StringUtils.isNotBlank(code)) {
			item.setString("code", code);
		}
		item.setInt("level", level);
		item.setID("dataId", this.dest);
		if (parent != null) {
			item.setID("parent", parent);
		}
		item = Application.getBean(ClassificationService.class).createOrUpdateItem(item);
		this.affected++;
		
		JSONArray children = node.getJSONArray("children");
		if (children != null) {
			for (Object ch : children) {
				addNItem((JSONObject) ch, item.getPrimary(), level + 1);
			}
		}
		return item.getPrimary();
	}
}
