/*!
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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dataimport.DataFileParser;
import com.rebuild.core.service.dataimport.DataImporter;
import com.rebuild.core.service.dataimport.ImportRule;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@Slf4j
@RestController
@RequestMapping({ "/admin/data/", "/app/entity/" })
public class DataImportController extends BaseController {

    @GetMapping("/data-imports")
    public ModelAndView page(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataImport),
                Language.L("无操作权限"));

        boolean isAdmin = UserHelper.isAdmin(user);
        boolean viaAdmin = request.getRequestURI().contains("/admin/");
        if (isAdmin && !viaAdmin) {
            response.sendRedirect("../../admin/data/data-imports?entity=" + getParameter(request, "entity", ""));
            return null;
        }

        if (!viaAdmin) {
            RbAssert.isCommercial(
                    Language.L("免费版不支持非管理员用户数据导入 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        }

        return createModelAndView(viaAdmin ? "/admin/data/data-imports" : "/general/data-imports");
    }

    // 检查导入文件
    @RequestMapping("/data-imports/check-file")
    public RespBody checkFile(HttpServletRequest request) {
        String file = getParameterNotNull(request, "file");
        File tmp = getFileOfImport(file);
        if (tmp == null) {
            return RespBody.error(Language.L("数据文件无效"));
        }

        DataFileParser parser;
        int count = -1;
        List<Cell[]> preview;
        try {
            parser = new DataFileParser(tmp);
            preview = parser.parse(11);
        } catch (Exception ex) {
            log.error("Parse excel error : " + file, ex);
            return RespBody.error(Language.L("无法解析数据，请检查数据文件格式"));
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[]{"count", "preview"}, new Object[]{count, preview}));
    }

    // 检查所属用户权限
    @RequestMapping("/data-imports/check-user")
    public JSON checkUserPrivileges(@EntityParam Entity entity, HttpServletRequest request) {
        final ID user = getIdParameterNotNull(request, "user");
        boolean canUpdated = Application.getPrivilegesManager().allowUpdate(user, entity.getEntityCode());
        boolean canCreated;
        if (entity.getMainEntity() == null) {
            canCreated = Application.getPrivilegesManager().allowCreate(user, entity.getEntityCode());
        } else {
            canCreated = Application.getPrivilegesManager().allowUpdate(user, entity.getMainEntity().getEntityCode());
        }

        return JSONUtils.toJSONObject(
                new String[]{"canCreate", "canUpdate"}, new Object[]{canCreated, canUpdated});
    }

    // 可用字段
    @RequestMapping("/data-imports/import-fields")
    public RespBody importFields(@EntityParam Entity entity) {
        List<Map<String, Object>> alist = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entity)) {
            String fieldName = field.getName();
            if (EntityHelper.OwningDept.equals(fieldName)
                    || MetadataHelper.isApprovalField(fieldName)
                    || MetadataHelper.isSystemField(fieldName)) {
                continue;
            }

            EasyField easyMeta = EasyMetaFactory.valueOf(field);
            DisplayType dt = easyMeta.getDisplayType();
            if (!dt.isImportable()) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("name", fieldName);
            map.put("label", easyMeta.getLabel());
            map.put("type", easyMeta.getDisplayType().name());
            map.put("nullable", field.isNullable());

            String defaultValue = null;
            if (EntityHelper.CreatedOn.equals(fieldName)
                    || EntityHelper.ModifiedOn.equals(fieldName)) {
                defaultValue = Language.L("当前时间");
            } else if (EntityHelper.CreatedBy.equals(fieldName)
                    || EntityHelper.ModifiedBy.equals(fieldName)
                    || EntityHelper.OwningUser.equals(fieldName)) {
                defaultValue = Language.L("当前用户");
            } else if (easyMeta.getDisplayType() == DisplayType.SERIES) {
                defaultValue = Language.L("自动编号");
            }

            if (defaultValue != null) {
                map.put("defaultValue", defaultValue);
            }
            alist.add(map);
        }

        return RespBody.ok(alist);
    }

    // 开始导入
    @PostMapping("/data-imports/import-submit")
    public RespBody importSubmit(HttpServletRequest request) {
        final ID user = getIdParameterNotNull(request, "user");
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataImport),
                Language.L("无操作权限"));

        ImportRule irule;
        try {
            irule = ImportRule.parse((JSONObject) ServletUtils.getRequestJson(request));
        } catch (IllegalArgumentException ex) {
            return RespBody.error(ex.getLocalizedMessage());
        }

        DataImporter importer = new DataImporter(irule);
        if (getBoolParameter(request, "preview")) {
            // TODO 导入预览
            return RespBody.error("TODO");
        } else {
            String taskid = TaskExecutors.submit(importer, getRequestUser(request));
            return RespBody.ok(JSONUtils.toJSONObject("taskid", taskid));
        }
    }

    private File getFileOfImport(String file) {
        if (file.contains("%")) {
            file = CodecUtils.urlDecode(file);
            file = CodecUtils.urlDecode(file);
        }
        File tmp = RebuildConfiguration.getFileOfTemp(file);
        return (!tmp.exists() || tmp.isDirectory()) ? null : tmp;
    }

    @GetMapping("/data-imports/import-trace")
    public RespBody importTrace(HttpServletRequest request) {
        String taskid = getParameterNotNull(request, "taskid");
        HeavyTask<?> task = TaskExecutors.get(taskid);

        if (task == null) {
            return RespBody.error();
        } else {
            return RespBody.ok(((DataImporter) task).getTraceLogs());
        }
    }
}
