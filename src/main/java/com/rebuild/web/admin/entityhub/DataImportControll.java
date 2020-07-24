/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.dataimport.DataFileParser;
import com.rebuild.server.business.dataimport.DataImporter;
import com.rebuild.server.business.dataimport.ImportRule;
import com.rebuild.server.helper.SysConfiguration;
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
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
@RequestMapping("/admin/datas/")
public class DataImportControll extends BasePageControll {

	@RequestMapping("/data-importer")
	public ModelAndView pageDataImports() {
		return createModelAndView("/admin/entityhub/data-importer.jsp");
	}
	
	// 检查导入文件
	@RequestMapping("/data-importer/check-file")
	public void checkFile(HttpServletRequest request, HttpServletResponse response) {
		String file = getParameterNotNull(request, "file");
		File tmp = getFileOfImport(file);
		if (tmp == null) {
			writeFailure(response, "无效数据文件");
			return;
		}
		
		DataFileParser parser;
		int count = -1;
		List<Cell[]> preview;
		try {
			parser = new DataFileParser(tmp);
			preview = parser.parse(11);
		} catch (Exception ex) {
			LOG.error("Parse excel error : " + file, ex);
			writeFailure(response, "无法解析数据，请检查数据文件格式");
			return;
		}

		JSON ret = JSONUtils.toJSONObject(
				new String[] { "count", "preview" }, new Object[] { count, preview });
		writeSuccess(response, ret);
	}

	// 检查所属用户权限
	@RequestMapping("/data-importer/check-user")
	public void checkUserPrivileges(HttpServletRequest request, HttpServletResponse response) {
		ID user = getIdParameterNotNull(request, "user");
		String entity = getParameterNotNull(request, "entity");
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		boolean canCreated = Application.getPrivilegesManager().allowCreate(user, entityMeta.getEntityCode());
		boolean canUpdated = Application.getPrivilegesManager().allowUpdate(user, entityMeta.getEntityCode());
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "canCreate", "canUpdate" }, new Object[] { canCreated, canUpdated });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("/data-importer/import-fields")
	public void importFields(HttpServletRequest request, HttpServletResponse response) {
		String entity = getParameterNotNull(request, "entity");
		Entity entityBase = MetadataHelper.getEntity(entity);
		
		List<Map<String, Object>> list = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityBase)) {
			String fieldName = field.getName();
			if (EntityHelper.OwningDept.equals(fieldName)
					|| MetadataHelper.isApprovalField(fieldName) || MetadataHelper.isSystemField(fieldName)) {
				continue;
			}
			EasyMeta easyMeta = new EasyMeta(field);
			if (easyMeta.getDisplayType() == DisplayType.FILE || easyMeta.getDisplayType() == DisplayType.IMAGE
					|| easyMeta.getDisplayType() == DisplayType.AVATAR || easyMeta.getDisplayType() == DisplayType.BARCODE
					|| easyMeta.getDisplayType() == DisplayType.ID || easyMeta.getDisplayType() == DisplayType.ANYREFERENCE) {
				continue;
			}

			Map<String, Object> map = new HashMap<>();
			map.put("name", fieldName);
			map.put("label", easyMeta.getLabel());
			map.put("type", easyMeta.getDisplayType().getDisplayName());
			map.put("nullable", field.isNullable());
			
			String defaultValue = null;
			if (EntityHelper.CreatedOn.equals(fieldName) || EntityHelper.ModifiedOn.equals(fieldName)) {
				defaultValue = "当前时间";
			} else if (EntityHelper.CreatedBy.equals(fieldName) || EntityHelper.ModifiedBy.equals(fieldName) || EntityHelper.OwningUser.equals(fieldName)) {
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
	public void importSubmit(HttpServletRequest request, HttpServletResponse response) {
		JSONObject idata = (JSONObject) ServletUtils.getRequestJson(request);
		ImportRule irule;
		try {
			irule = ImportRule.parse(idata);
		} catch (IllegalArgumentException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		
		DataImporter importer = new DataImporter(irule);
		if (getBoolParameter(request, "preview")) {
			// TODO 导入预览
		} else {
			String taskid = TaskExecutors.submit(importer, getRequestUser(request));
			JSON ret = JSONUtils.toJSONObject("taskid", taskid);
			writeSuccess(response, ret);
		}
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
