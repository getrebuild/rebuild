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

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.business.rbstore.ClassificationImporter;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 分类数据导入
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 * 
 * @see ClassificationImporter
 */
@Controller
public class ClassificationImportControll extends BaseControll {

	@RequestMapping("/admin/entityhub/classification/imports/start")
	public void starts(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID dest = getIdParameterNotNull(request, "dest");
		String fileUrl = getParameterNotNull(request, "file");
		
		ClassificationImporter importer = new ClassificationImporter(user, dest, fileUrl);
		String taskid = TaskExecutors.submit(importer);
		writeSuccess(response, taskid);
	}
}
