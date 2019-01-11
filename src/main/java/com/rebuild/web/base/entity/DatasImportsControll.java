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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.rebuild.server.business.datas.DataFileParser;
import com.rebuild.server.business.datas.DataImports;
import com.rebuild.server.business.datas.ImportsEnter;
import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.server.helper.task.BulkTask;
import com.rebuild.server.helper.task.BulkTaskExecutor;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
@RequestMapping("/admin/")
public class DatasImportsControll extends BasePageControll {

	@RequestMapping("/datas/imports")
	public ModelAndView pageDataImports(HttpServletRequest request) {
		return createModelAndView("/admin/datas/imports.jsp");
	}
	
	@RequestMapping("/datas/imports-fields")
	public void fields(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		Entity entityBase = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> list = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityBase)) {
			String fieldName = field.getName();
			if (EntityHelper.OwningDept.equals(fieldName)) {
				continue;
			}
			
			EasyMeta easyMeta = new EasyMeta(field);
			if (easyMeta.getDisplayType() == DisplayType.FILE || easyMeta.getDisplayType() == DisplayType.IMAGE) {
				continue;
			}
			
			Map<String, Object> map = new HashMap<>();
			map.put("name", fieldName);
			map.put("label", easyMeta.getLabel());
			map.put("type", easyMeta.getDisplayType().getDisplayName());
			map.put("isNullable", field.isNullable());
			
			String defaultValue = null;
			if (EntityHelper.CreatedOn.equals(fieldName) || EntityHelper.ModifiedOn.equals(fieldName)) {
				defaultValue = "当前时间";
			} else if (EntityHelper.CreatedBy.equals(fieldName) || EntityHelper.ModifiedBy.equals(fieldName)  || EntityHelper.OwningUser.equals(fieldName)) {
				defaultValue = "当前用户";
			}  else if (easyMeta.getDisplayType() == DisplayType.SERIES) {
				defaultValue = "自动编号";
			}
			if (defaultValue != null) {
				map.put("defaultValue", defaultValue);
			}
			list.add(map);
		}
		writeSuccess(response, list);
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
		List<Cell[]> preview = parser.parse(1);
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("rows_count", parser.getRowsCount());
		ret.put("rows_preview", preview);
		
		SerializeConfig.getGlobalInstance().put(Cell.class, ToStringSerializer.instance);
		writeSuccess(response, ret);
	}
	
	// 开始导入
	@RequestMapping("/datas/imports-submit")
	public void importsSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSONObject idata = (JSONObject) ServletUtils.getRequestJson(request);
		
		ImportsEnter ienter = null;
		try {
			ienter = ImportsEnter.parse(idata);
		} catch (IllegalArgumentException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		
		DataImports dataImports = new DataImports(ienter, user);
		String taskid = BulkTaskExecutor.submit(dataImports);
		
		JSON ret = JSONUtils.toJSONObject("taskid", taskid);
		writeSuccess(response, ret);
	}
	
	// 导入状态
	@RequestMapping("/datas/imports-state")
	public void importsState(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String taskid = getParameterNotNull(request, "taskid");
		BulkTask task = BulkTaskExecutor.getTask(taskid);
		if (task == null) {
			writeFailure(response, "无效任务 : " + taskid);
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "total", "complete", "isCompleted" }, 
				new Object[] { task.getTotal(), task.getComplete(), task.isCompleted() });
		writeSuccess(response, ret);
	}
	
	// 导入取消
	@RequestMapping("/datas/imports-cancel")
	public void importsCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String taskid = getParameterNotNull(request, "taskid");
		BulkTask task = BulkTaskExecutor.getTask(taskid);
		if (task == null) {
			writeFailure(response, "无效任务 : " + taskid);
			return;
		}
		
		task.interrupt();
		for (int i = 0; i < 10; i++) {
			ThreadPool.waitFor(200);
			if (task.isInterrupted()) {
				writeSuccess(response);
				return;
			}
		}
		writeFailure(response);
	}
}
