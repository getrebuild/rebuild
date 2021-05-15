/*
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

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * 列表配置
 *
 * @author devezhao
 * @since 01/07/2019
 */
@Controller
@RequestMapping("/app/{entity}/")
public class ShowFieldsController extends BaseController implements ShareTo {

    @PostMapping("list-fields")
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList),
                $L("无操作权限"));

        ID cfgid = getIdParameter(request, "id");
        // 普通用户只能有一个
        if (cfgid != null && !UserHelper.isSelf(user, cfgid)) {
            ID useList = DataListManager.instance.detectUseConfig(user, entity, DataListManager.TYPE_DATALIST);
            if (useList != null && UserHelper.isSelf(user, useList)) {
                cfgid = useList;
            } else {
                cfgid = null;
            }
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

        writeSuccess(response);
    }

    @GetMapping("list-fields")
    public void gets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final Entity entityMeta = MetadataHelper.getEntity(entity);

        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (canListField(field)) {
                fieldList.add(DataListManager.instance.formatField(field));
            }
        }

        // 明细关联字段
        final Field dtmField = entityMeta.getMainEntity() == null ? null : MetadataHelper.getDetailToMainField(entityMeta);

        // 引用实体的字段
        for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
            // 过滤所属用户/所属部门等系统字段（除了明细引用（主实体）字段）
            if (EasyMetaFactory.valueOf(field).isBuiltin() && (dtmField == null || !dtmField.equals(field))) {
                continue;
            }

            Entity refEntity = field.getReferenceEntity();
            // 无权限的不返回
            if (!Application.getPrivilegesManager().allowRead(user, refEntity.getEntityCode())) {
                continue;
            }

            for (Field fieldOfRef : MetadataSorter.sortFields(refEntity)) {
                if (canListField(fieldOfRef)) {
                    fieldList.add(DataListManager.instance.formatField(fieldOfRef, field));
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
            raw = DataListManager.instance.getLayoutOfDatalist(user, entity);
        }

        JSONObject config = (JSONObject) DataListManager.instance.formatFieldsLayout(entity, user, false, raw);

        Map<String, Object> ret = new HashMap<>();
        ret.put("fieldList", fieldList);
        ret.put("configList", config.getJSONArray("fields"));
        if (raw != null) {
            ret.put("configId", raw.getID("id"));
            ret.put("shareTo", raw.getString("shareTo"));
        }
        writeSuccess(response, ret);
    }

    /**
     * @see NavSettings#getsList(HttpServletRequest, HttpServletResponse)
     */
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

    private boolean canListField(Field field) {
        return EasyMetaFactory.getDisplayType(field) != DisplayType.BARCODE;
    }
}
