/*
rebuild - Building your system freely.
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

package com.rebuild.web.base.entity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.business.datas.DataFileParser;
import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.excel.Cell;

/**
 * 
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
@RequestMapping("/admin/")
public class DataImportsControll extends BaseControll {

	@RequestMapping("/datas/imports")
	public ModelAndView pageDataImports(HttpServletRequest request) {
		return createModelAndView("/admin/datas/imports.jsp");
	}
	
	// 预览
	@RequestMapping("/datas/imports-preview")
	public void importsPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String file = getParameterNotNull(request, "file");
		if (file.contains("%")) {
			file = CodecUtils.urlDecode(file);
			file = CodecUtils.urlDecode(file);
		}
		File tmp = SystemConfiguration.getFileOfTemp(file);
		if (!tmp.exists() || tmp.isDirectory()) {
			writeFailure(response, "无效文件");
			return;
		}
		
		DataFileParser parser = new DataFileParser(tmp);
		List<Cell[]> preview = parser.parse(100);
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("rows_count", parser.getRowsCount());
		ret.put("rows_preview", preview);
		
		writeSuccess(response, ret);
	}
	
	// 开始导入
	@RequestMapping("/datas/imports-submit")
	public void importsSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
	}
	
	// 导入状态
	@RequestMapping("/datas/imports-state")
	public void importsState(HttpServletRequest request, HttpServletResponse response) throws IOException {
	}
}
