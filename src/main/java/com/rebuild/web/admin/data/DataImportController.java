/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.data;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.service.dataimport.DataFileParser;
import com.rebuild.core.service.dataimport.DataImporter;
import com.rebuild.core.service.dataimport.ImportRule;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author devezhao
 * @since 01/03/2019
 */
@Controller
@RequestMapping("/admin/data/")
public class DataImportController extends BaseController {

    @GetMapping("/data-imports")
    public ModelAndView page() {
        return createModelAndView("/admin/data/data-imports");
    }

    // 检查导入文件
    @RequestMapping("/data-imports/check-file")
    public void checkFile(HttpServletRequest request, HttpServletResponse response) {
        String file = getParameterNotNull(request, "file");
        File tmp = getFileOfImport(file);
        if (tmp == null) {
            writeFailure(response, getLang(request, "SomeInvalid,DataFile"));
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
            writeFailure(response, getLang(request, "NotParseDataFile"));
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"count", "preview"}, new Object[]{count, preview});
        writeSuccess(response, ret);
    }

    // 检查所属用户权限
    @RequestMapping("/data-imports/check-user")
    public void checkUserPrivileges(HttpServletRequest request, HttpServletResponse response) {
        ID user = getIdParameterNotNull(request, "user");
        String entity = getParameterNotNull(request, "entity");

        Entity entityMeta = MetadataHelper.getEntity(entity);
        boolean canCreated = Application.getPrivilegesManager().allowCreate(user, entityMeta.getEntityCode());
        boolean canUpdated = Application.getPrivilegesManager().allowUpdate(user, entityMeta.getEntityCode());

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"canCreate", "canUpdate"}, new Object[]{canCreated, canUpdated});
        writeSuccess(response, ret);
    }

    @RequestMapping("/data-imports/import-fields")
    public void importFields(HttpServletRequest request, HttpServletResponse response) {
        Entity entity = MetadataHelper.getEntity(getParameterNotNull(request, "entity"));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entity)) {
            String fieldName = field.getName();
            if (EntityHelper.OwningDept.equals(fieldName)
                    || MetadataHelper.isApprovalField(fieldName)
                    || MetadataHelper.isSystemField(fieldName)) {
                continue;
            }

            // TODO 开放媒体字段导入
            EasyMeta easyMeta = new EasyMeta(field);
            if (easyMeta.getDisplayType() == DisplayType.FILE
                    || easyMeta.getDisplayType() == DisplayType.IMAGE
                    || easyMeta.getDisplayType() == DisplayType.AVATAR
                    || easyMeta.getDisplayType() == DisplayType.BARCODE
                    || easyMeta.getDisplayType() == DisplayType.ID
                    || easyMeta.getDisplayType() == DisplayType.ANYREFERENCE) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("name", fieldName);
            map.put("label", easyMeta.getLabel());
            map.put("type", easyMeta.getDisplayType().getDisplayName());
            map.put("nullable", field.isNullable());

            String defaultValue = null;
            if (EntityHelper.CreatedOn.equals(fieldName)
                    || EntityHelper.ModifiedOn.equals(fieldName)) {
                defaultValue = getLang(request, "CurrentTime");
            } else if (EntityHelper.CreatedBy.equals(fieldName)
                    || EntityHelper.ModifiedBy.equals(fieldName)
                    || EntityHelper.OwningUser.equals(fieldName)) {
                defaultValue = getLang(request, "CurrentUser");
            } else if (easyMeta.getDisplayType() == DisplayType.SERIES) {
                defaultValue = getLang(request, "t.SERIES");
            }

            if (defaultValue != null) {
                map.put("defaultValue", defaultValue);
            }
            list.add(map);
        }
        writeSuccess(response, list);
    }

    // 开始导入
    @RequestMapping("/data-imports/import-submit")
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
        File tmp = RebuildConfiguration.getFileOfTemp(file);
        return (!tmp.exists() || tmp.isDirectory()) ? null : tmp;
    }
}
