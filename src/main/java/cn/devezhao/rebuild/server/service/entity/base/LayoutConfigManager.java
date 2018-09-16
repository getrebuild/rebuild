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

package cn.devezhao.rebuild.server.service.entity.base;

import com.alibaba.fastjson.JSON;

import cn.devezhao.rebuild.server.Application;

/**
 * 布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public class LayoutConfigManager {
	
	// 表单
	public static final String TYPE_FORM = "FORM";
	// 视图
	public static final String TYPE_VIEW = "VIEW";
	// 数据列表
	public static final String TYPE_DATALIST = "DATALIST";
	// 导航
	public static final String TYPE_NAVI = "NAVI";
	
	/**
	 * @param entity
	 * @param type
	 * @return
	 */
	public static Object[] getLayoutConfigRaw(String entity, String type) {
		Object[] config = Application.createNoFilterQuery(
				"select layoutId,config from LayoutConfig where type = ? and belongEntity = ?")
				.setParameter(1, type)
				.setParameter(2, entity)
				.unique();
		if (config == null) {
			return null;
		}
		config[1] = JSON.parse(config[1].toString());
		return config;
	}
}
