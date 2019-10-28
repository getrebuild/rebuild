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

package com.rebuild.web.common;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/02
 */
@Controller
@RequestMapping("/filex/qiniu")
public class QiniuUploadControll extends BaseControll {

	// 获取上传参数
	@RequestMapping("upload-keys")
	public void getUploadKeys(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String fileName = getParameterNotNull(request, "file");
		
		String fileKey = QiniuCloud.formatFileKey(fileName);
		String token = QiniuCloud.instance().getUploadToken(fileKey);
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "key", "token" }, new String[] { fileKey, token });
		writeSuccess(response, ret);
	}

}
