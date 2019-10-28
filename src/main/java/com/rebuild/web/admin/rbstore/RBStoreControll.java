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

import com.alibaba.fastjson.JSON;
import com.rebuild.server.business.rbstore.RBStore;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@Controller
@RequestMapping("/admin/rbstore")
public class RBStoreControll extends BasePageControll {

	@RequestMapping("load-index")
	public void loadDataIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String type = getParameterNotNull(request, "type");
		JSON index = RBStore.fetchRemoteJson(type + "/index.json");
		if (index == null) {
			writeSuccess(response, "无法获取索引数据");
		} else {
			writeSuccess(response, index);
		}
	}
}
