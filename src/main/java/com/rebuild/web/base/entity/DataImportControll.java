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
import com.rebuild.server.business.datas.DataImporter;
import com.rebuild.server.business.datas.ImportEnter;
import com.rebuild.server.helper.SystemConfig;
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
public class DataImportControll extends BasePageControll {

	@RequestMapping("/datas/importer")
	public ModelAndView pageDataImports(HttpServletRequest request) {
		return createModelAndView("/admin/datas/importer.jsp");
	}
	
	@RequestMapping("/datas/import-fields")
	public void importFields(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
	@RequestMapping("/datas/import-preview")
	public void importPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String file = getParameterNotNull(request, "file");
		if (file.contains("%")) {
			file = CodecUtils.urlDecode(file);
			file = CodecUtils.urlDecode(file);
		}
		File tmp = SystemConfig.getFileOfTemp(file);
		if (!tmp.exists() || tmp.isDirectory()) {
			writeFailure(response, "无效文件");
			return;
		}
		
		DataFileParser parser = null;
		int count = 0;
		List<Cell[]> preview = null;
		try {
			parser = new DataFileParser(tmp);
			count = parser.getRowsCount();
			preview = parser.parse(10);
		} catch (Exception ex) {
			writeFailure(response, "无法解析数据，请检查数据文件格式");
			return;
		} finally {
			if (parser != null) {
				parser.close();
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("count", count);
		ret.put("preview", preview);
		
		SerializeConfig.getGlobalInstance().put(Cell.class, ToStringSerializer.instance);
		writeSuccess(response, ret);
	}
	
	// 开始导入
	@RequestMapping("/datas/import-submit")
	public void importSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSONObject idata = (JSONObject) ServletUtils.getRequestJson(request);
		
		ImportEnter ienter = null;
		try {
			ienter = ImportEnter.parse(idata);
		} catch (IllegalArgumentException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		
		DataImporter dataImports = new DataImporter(ienter, user);
		String taskid = BulkTaskExecutor.submit(dataImports);
		
		JSON ret = JSONUtils.toJSONObject("taskid", taskid);
		writeSuccess(response, ret);
	}
	
	// 导入状态
	@RequestMapping("/datas/import-state")
	public void importState(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String taskid = getParameterNotNull(request, "taskid");
		BulkTask task = BulkTaskExecutor.getTask(taskid);
		if (task == null) {
			writeFailure(response, "无效任务 : " + taskid);
			return;
		}
		
		writeSuccess(response, formatTaskState((DataImporter) task));
	}
	
	// 导入取消
	@RequestMapping("/datas/import-cancel")
	public void importCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String taskid = getParameterNotNull(request, "taskid");
		BulkTask task = BulkTaskExecutor.getTask(taskid);
		if (task == null) {
			writeFailure(response, "无效任务 : " + taskid);
			return;
		}
		if (task.isCompleted()) {
			writeFailure(response, "无法终止，因为导入任务已执行完成");
			return;
		}
		
		task.interrupt();
		for (int i = 0; i < 10; i++) {
			if (task.isInterrupted()) {
				writeSuccess(response, formatTaskState((DataImporter) task));
				return;
			}
			ThreadPool.waitFor(200);
		}
		writeFailure(response);
	}
	
	/**
	 * @param task
	 * @return
	 */
	private JSON formatTaskState(DataImporter task) {
		JSON state = JSONUtils.toJSONObject(
				new String[] { "total", "complete", "success", "isCompleted", "isInterrupted" },
				new Object[] { task.getTotal(), task.getComplete(),
						((DataImporter) task).getSuccess(), task.isCompleted(), task.isInterrupted() });
		return state;
	}
}
