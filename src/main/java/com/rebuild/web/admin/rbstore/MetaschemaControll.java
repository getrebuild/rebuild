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
import com.rebuild.server.business.rbstore.MetaschemaImporter;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
		ID user = getRequestUser(request);
		String fileUrl = getParameterNotNull(request, "file");
		
		MetaschemaImporter importer = new MetaschemaImporter(user, fileUrl);
		try {
			String hasError = importer.verfiy();
			if (hasError != null) {
				writeFailure(response, hasError);
				return;
			}
			
			Object entityName = importer.exec();
			writeSuccess(response, entityName);
		} catch (Exception ex) {
			writeFailure(response, ex.getLocalizedMessage());
		}
	}
}
