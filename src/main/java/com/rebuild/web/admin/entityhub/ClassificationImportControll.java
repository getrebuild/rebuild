/*
rebuild - Building your system freely.
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

package com.rebuild.web.admin.entityhub;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.business.dataio.ClassificationImporter;
import com.rebuild.server.helper.task.BulkTaskExecutor;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.engine.ID;

/**
 * 分类数据导入
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 * 
 * @see ClassificationImporter
 */
@RequestMapping("/admin/classification/")
@Controller
public class ClassificationImportControll extends BaseControll {
	
	@RequestMapping("/imports/load-index")
	public void loadDataIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON index = ClassificationImporter.fetchRemoteJson("index.json");
		if (index == null) {
			writeSuccess(response, "无法获取索引数据");
		} else {
			writeSuccess(response, index);
		}
	}

	@RequestMapping("/imports/starts")
	public void starts(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID dest = getIdParameterNotNull(request, "dest");
		String fileUrl = getParameterNotNull(request, "file");
		
		ClassificationImporter importer = new ClassificationImporter(user, dest, fileUrl);
		String taskid = BulkTaskExecutor.submit(importer);
		writeSuccess(response, taskid);
	}
}
