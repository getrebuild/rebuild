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

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.rebuild.server.Application;
import com.rebuild.server.business.dataimport.DataFileParser;
import com.rebuild.server.business.dataimport.DataImporter;
import com.rebuild.server.business.dataimport.ImportRule;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
@RequestMapping("/admin/datas/")
public class DataImportControll extends BasePageControll {
	
	static {
		SerializeConfig.getGlobalInstance().put(Cell.class, ToStringSerializer.instance);
	}

	@RequestMapping("/data-importer")
	public ModelAndView pageDataImports(HttpServletRequest request) {
		return createModelAndView("/admin/entityhub/data-importer.jsp");
	}
	
	// 检查导入文件
	@RequestMapping("/data-importer/check-file")
	public void checkFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String file = getParameterNotNull(request, "file");
		File tmp = getFileOfImport(file);
		if (tmp == null) {
			writeFailure(response, "无效数据文件");
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
			LOG.error("Parse excel error : " + file, ex);
			writeFailure(response, "无法解析数据，请检查数据文件格式");
			return;
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "count", "preview" }, new Object[] { count, preview });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("/data-importer/check-user-privileges")
	public void checkUserPrivileges(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID ouser = getIdParameterNotNull(request, "ouser");
		String entity = getParameterNotNull(request, "entity");
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		boolean canCreated = Application.getSecurityManager().allowedC(ouser, entityMeta.getEntityCode());
		boolean canUpdated = Application.getSecurityManager().allowedU(ouser, entityMeta.getEntityCode());
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "canCreate", "canUpdate" }, new Object[] { canCreated, canUpdated });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("/data-importer/import-fields")
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
	
	// 开始导入
	@RequestMapping("/data-importer/import-submit")
	public void importSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject idata = (JSONObject) ServletUtils.getRequestJson(request);
		ImportRule irule = null;
		try {
			irule = ImportRule.parse(idata);
		} catch (IllegalArgumentException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		
		DataImporter importer = new DataImporter(irule, getRequestUser(request));
		if (getBoolParameter(request, "preview")) {
			// TODO 导入预览
		} else {
			String taskid = TaskExecutors.submit(importer);
			JSON ret = JSONUtils.toJSONObject("taskid", taskid);
			writeSuccess(response, ret);
		}
	}
	
	// 导入状态
	@RequestMapping("/data-importer/import-state")
	public void importState(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String taskid = getParameterNotNull(request, "taskid");
		HeavyTask<?> task = TaskExecutors.getTask(taskid);
		if (task == null) {
			writeFailure(response, "无效任务 : " + taskid);
			return;
		}
		
		writeSuccess(response, formatTaskState((DataImporter) task));
	}
	
	// 导入取消
	@RequestMapping("/data-importer/import-cancel")
	public void importCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String taskid = getParameterNotNull(request, "taskid");
		HeavyTask<?> task = TaskExecutors.getTask(taskid);
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
				new String[] { "total", "complete", "success", "isCompleted", "isInterrupted", "elapsedTime" },
				new Object[] { task.getTotal(), task.getCompleted(),
						task.getSuccessed(), task.isCompleted(), task.isInterrupted(), task.getElapsedTime() });
		return state;
	}
	
	/**
	 * @param file
	 * @return
	 */
	private File getFileOfImport(String file) {
		if (file.contains("%")) {
			file = CodecUtils.urlDecode(file);
			file = CodecUtils.urlDecode(file);
		}
		File tmp = SysConfiguration.getFileOfTemp(file);
		return (!tmp.exists() || tmp.isDirectory()) ? null : tmp;
	}
}
