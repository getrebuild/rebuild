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

package cn.devezhao.rebuild.web.base.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.service.base.DataListManager;
import cn.devezhao.rebuild.server.service.base.LayoutManager;
import cn.devezhao.rebuild.web.BaseControll;

/**
 * 数据列表相关配置
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
@Controller
@RequestMapping("/app/")
public class DataListSettings extends BaseControll {

	@RequestMapping(value = "{entity}/list-columns", method = RequestMethod.POST)
	public void columnsSet(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		JSON config = ServletUtils.getRequestJson(request);
		ID configId = getIdParameter(request, "cfgid");
		
		Record record = null;
		if (configId == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", entityMeta.getName());
			record.setString("type", LayoutManager.TYPE_DATALIST);
		} else {
			record = EntityHelper.forUpdate(configId, user);
		}
		record.setString("config", config.toJSONString());
		Application.getCommonService().createOrUpdate(record);
		writeSuccess(response);
	}
	
	@RequestMapping(value = "{entity}/list-columns", method = RequestMethod.GET)
	public void columnsGet(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> fieldList = new ArrayList<>();
		for (Field field : entityMeta.getFields()) {
			if (field.getType() == FieldType.PRIMARY) {
				continue;
			}
			fieldList.add(DataListManager.warpColumn(field));
		}
		
		List<Map<String, Object>> configList = new ArrayList<>();
		Object[] lcr = DataListManager.getLayoutConfigRaw(entity, DataListManager.TYPE_DATALIST);
		if (lcr != null) {
			for (Object o : (JSONArray) lcr[1]) {
				JSONObject jo = (JSONObject) o;
				String field = jo.getString("field");
				if (entityMeta.containsField(field)) {
					configList.add(DataListManager.warpColumn(entityMeta.getField(field)));
				} else {
					LOG.warn("Invalid field : " + field);
				}
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("fieldList", fieldList);
		ret.put("configList", configList);
		if (lcr != null) {
			ret.put("configId", lcr[0].toString());
		}
		writeSuccess(response, ret);
	}
}
