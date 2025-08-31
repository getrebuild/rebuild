/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.BaseLayoutManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.RoleService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 列表配置
 *
 * @author devezhao
 * @since 01/07/2019
 * @see com.rebuild.web.admin.metadata.ListStatsController
 */
@Controller
@RequestMapping("/app/{entity}/")
public class ListFieldsController extends BaseController implements ShareTo {

    @PostMapping("list-fields")
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList),
                Language.L("无操作权限"));

        ID cfgid = getIdParameter(request, "id");
        // 普通用户只能有一个
        if (cfgid != null && !UserHelper.isSelf(user, cfgid)) {
            ID useList = DataListManager.instance.detectUseConfig(user, entity, DataListManager.TYPE_DATALIST);
            if (useList != null && UserHelper.isSelf(user, useList)) cfgid = useList;
            else cfgid = null;
        }

        JSON config = ServletUtils.getRequestJson(request);

        Record record;
        if (cfgid == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", BaseLayoutManager.TYPE_DATALIST);
            record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
        } else {
            record = EntityHelper.forUpdate(cfgid, user);
        }
        record.setString("config", config.toJSONString());
        putCommonsFields(request, record);
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        writeSuccess(response, null);
    }

    @GetMapping("list-fields")
    public void gets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final Entity entityMeta = MetadataHelper.getEntity(entity);

        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (canListField(field)) {
                Map<String, Object> m = DataListManager.instance.formatField(field);
                m.put("quickCode", QuickCodeReindexTask.generateQuickCode((String) m.get("label")));
                fieldList.add(m);
            }
        }

        // 3级字段
        // FIXME 3级字段有问题，暂无法确定3级字段的权限
        final boolean deep3 = getBoolParameter(request, "deep3");

        // ~引用:2级
        List<Field[]> refFieldsOf2 = new ArrayList<>();
        // 明细关联字段
        Field dtmField = entityMeta.getMainEntity() == null ? null : MetadataHelper.getDetailToMainField(entityMeta);
        // 引用实体的字段
        for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
            // 过滤所属用户/所属部门等系统字段（除了明细引用（主实体）字段）
            if (EasyMetaFactory.valueOf(field).isBuiltin() && (dtmField == null || !dtmField.equals(field))) continue;
            // 无权限的不返回
            final Entity refEntity = field.getReferenceEntity();
            if (!Application.getPrivilegesManager().allowRead(user, refEntity.getEntityCode())) continue;

            for (Field field2 : MetadataSorter.sortFields(refEntity)) {
                if (canListField(field2)) {
                    Map<String, Object> m = DataListManager.instance.formatField(field2, field);
                    m.put("quickCode", QuickCodeReindexTask.generateQuickCode((String) m.get("label")));
                    fieldList.add(m);

                    if (deep3 && EasyMetaFactory.getDisplayType(field2) == DisplayType.REFERENCE) {
                        refFieldsOf2.add(new Field[]{field, field2});
                    }
                }
            }
        }
        // ~引用:3级
        for (Field[] parentField : refFieldsOf2) {
            Field field2 = parentField[1];

            // 明细关联字段
            final Field dtmField2 = field2.getOwnEntity().getMainEntity() == null ? null
                    : MetadataHelper.getDetailToMainField(field2.getOwnEntity());

            // 过滤所属用户/所属部门等系统字段（除了明细引用（主实体）字段）
            if (EasyMetaFactory.valueOf(field2).isBuiltin() && (dtmField2 == null || !dtmField2.equals(field2))) continue;
            // 无权限的不返回
            final Entity refEntity2 = field2.getReferenceEntity();
            if (!Application.getPrivilegesManager().allowRead(user, refEntity2.getEntityCode())) continue;

            for (Field field3 : MetadataSorter.sortFields(refEntity2)) {
                if (canListField(field3)) {
                    Map<String, Object> m = DataListManager.instance.formatField(field3, parentField);
                    m.put("quickCode", QuickCodeReindexTask.generateQuickCode((String) m.get("label")));
                    fieldList.add(m);
                }
            }
        }

        ConfigBean raw;
        String cfgid = request.getParameter("id");
        if ("NEW".equalsIgnoreCase(cfgid)) {
            raw = new ConfigBean();
            raw.set("config", JSONUtils.EMPTY_ARRAY);
        } else if (ID.isId(cfgid)) {
            raw = DataListManager.instance.getLayoutById(ID.valueOf(cfgid));
        } else {
            raw = DataListManager.instance.getLayoutOfDatalist(user, entity, null);
        }

        JSONObject config = (JSONObject) DataListManager.instance.formatListFields(entity, user, Boolean.FALSE, raw);

        Map<String, Object> ret = new HashMap<>();
        ret.put("fieldList", fieldList);
        ret.put("configList", config.getJSONArray("fields"));
        if (raw != null) {
            ret.put("configId", raw.getID("id"));
            ret.put("shareTo", raw.getString("shareTo"));
        }
        writeSuccess(response, ret);
    }

    private boolean canListField(Field field) {
        return field.isQueryable();
    }

    @GetMapping("list-fields/alist")
    public void getsList(@PathVariable String entity,
                         HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);

        String sql = "select configId,configName,shareTo,createdBy from LayoutConfig where ";
        if (UserHelper.isAdmin(user)) {
            sql += String.format("belongEntity = '%s' and applyType = '%s' and createdBy.roleId = '%s' order by configName",
                    entity, DataListManager.TYPE_DATALIST, RoleService.ADMIN_ROLE);
        } else {
            // 普通用户可用的
            ID[] uses = DataListManager.instance.getUsesDataListId(entity, user);
            sql += "configId in ('" + StringUtils.join(uses, "', '") + "')";
        }

        Object[][] list = Application.createQueryNoFilter(sql).array();
        writeSuccess(response, list);
    }
}
