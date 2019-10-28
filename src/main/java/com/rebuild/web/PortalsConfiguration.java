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

package com.rebuild.web;

import cn.devezhao.persist4j.Record;
import com.rebuild.server.configuration.ConfigManager;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 页面/界面相关配置接口规范
 * 
 * @author devezhao
 * @since 10/14/2018
 * @see ConfigManager
 */
public interface PortalsConfiguration {
	
	void sets(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException;
	
	void gets(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException;

	/**
	 * 公共字段
	 * @param request
	 * @param record
	 */
	default void putCommonsFields(HttpServletRequest request, Record record) {
		String shareTo = request.getParameter("shareTo");
		if (StringUtils.isNotBlank(shareTo)) {
			record.setString("shareTo", shareTo);
		}
		String configName = request.getParameter("configName");
		if (StringUtils.isNotBlank(configName)) {
			record.setString("configName", configName);
		}
	}
}