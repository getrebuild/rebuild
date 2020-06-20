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

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.RebuildException;
import com.rebuild.server.business.rbstore.MetaschemaImporter;
import com.rebuild.server.business.rbstore.RBStore;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *  导入元数据模型
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@Controller
public class MetaschemaControll extends BaseControll {
	
	@RequestMapping("/admin/metaschema/imports")
	public void imports(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String entityKey = getParameterNotNull(request, "key");

		JSONArray index = (JSONArray) RBStore.fetchMetaschema("index.json");
		String entityFile = null;
		List<String> refFiles = new ArrayList<>();
		for (Object o : index) {
			JSONObject item = (JSONObject) o;
			if (!entityKey.equalsIgnoreCase(item.getString("key"))) {
				continue;
			}

			entityFile = item.getString("file");
			JSONArray refs = item.getJSONArray("refs");
			if (refs != null) {
				for (Object ref : refs) {
					String refEntity = (String) ref;
					if (!MetadataHelper.containsEntity(refEntity)) {
						refFiles.add(foundFile(index, refEntity));
					}
				}
			}
			break;
		}

		Assert.notNull(entityFile, "No metaschema found : " + entityKey);

		// 先处理引用实体
		// NOTE 失败后无回滚
		for (String file : refFiles) {
			MetaschemaImporter importer = new MetaschemaImporter(file);
			try {
				String hasError = importer.verfiy();
				if (hasError != null) {
					writeFailure(response, hasError);
					return;
				}

				TaskExecutors.exec(importer);
			} catch (Exception ex) {
				writeFailure(response, ex.getLocalizedMessage());
				return;
			}
		}

		MetaschemaImporter importer = new MetaschemaImporter(entityFile);
		try {
			String hasError = importer.verfiy();
			if (hasError != null) {
				writeFailure(response, hasError);
				return;
			}

			Object entityName = TaskExecutors.exec(importer);
			writeSuccess(response, entityName);

		} catch (Exception ex) {
			writeFailure(response, ex.getLocalizedMessage());
		}
	}

	private String foundFile(JSONArray index, String key) {
		for (Object o : index) {
			JSONObject item = (JSONObject) o;
			if (key.equalsIgnoreCase(item.getString("key"))) {
				return item.getString("file");
			}
		}
		throw new RebuildException("No metaschema found : " + key);
	}
}
