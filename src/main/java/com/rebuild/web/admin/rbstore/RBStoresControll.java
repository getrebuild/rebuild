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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.business.rbstores.RBStores;
import com.rebuild.web.BasePageControll;

/**
 * TODO
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@Controller
@RequestMapping("/admin/rbstore")
public class RBStoresControll extends BasePageControll {

	@RequestMapping("load-index")
	public void loadDataIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String path = getParameterNotNull(request, "path");
		JSON index = RBStores.fetchRemoteJson(path);
		if (index == null) {
			writeSuccess(response, "无法获取索引数据");
		} else {
			writeSuccess(response, index);
		}
	}
}
