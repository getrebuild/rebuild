/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesManager;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 元数据获取
 *
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@RestController
@RequestMapping("/commons/metadata/")
public class MetadataGetting extends BaseController {

    @GetMapping("entities")
    public List<Map<String, Object>> entities(HttpServletRequest request) {
        ID user = getRequestUser(request);
        // 返回明细实体
        boolean usesDetail = getBoolParameter(request, "detail", false);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities(user, false, usesDetail)) {
            Map<String, Object> map = new HashMap<>();
            EasyEntity easy = EasyMetaFactory.valueOf(e);
            map.put("name", e.getName());
            map.put("label", easy.getLabel());
            map.put("icon", easy.getIcon());
            data.add(map);
        }
        return data;
    }

    @GetMapping("fields")
    public List<JSONObject> fields(HttpServletRequest request) {
        String entity = getParameterNotNull(request, "entity");
        Entity entityMeta = MetadataHelper.getEntity(entity);
        // 返回引用实体的字段
        boolean appendRefFields = "2".equals(getParameter(request, "deep"));

        List<JSONObject> data = new ArrayList<>();
        putFields(data, entityMeta, appendRefFields);

        // 追加二级引用字段
        if (appendRefFields) {
            for (Field field : entityMeta.getFields()) {
                if (!field.isQueryable() || EasyMetaFactory.getDisplayType(field) != DisplayType.REFERENCE) continue;

                int code = field.getReferenceEntity().getEntityCode();
                if (MetadataHelper.isBizzEntity(code) || code == EntityHelper.RobotApprovalConfig) continue;

                data.add(EasyMetaFactory.toJSON(field));
                putFields(data, field, false);
            }
        }
        return data;
    }

    /**
     * @param dest
     * @param entityOrField
     * @param filterRefField
     */
    private void putFields(List<JSONObject> dest, BaseMeta entityOrField, boolean filterRefField) {
        Field parentField = null;
        Entity useEntity;
        if (entityOrField instanceof Field) {
            parentField = (Field) entityOrField;
            useEntity = parentField.getReferenceEntity();
        } else {
            useEntity = (Entity) entityOrField;
        }

        for (Field field : MetadataSorter.sortFields(useEntity)) {
            JSONObject map = EasyMetaFactory.toJSON(field);

            // 引用字段处理
            if (EasyMetaFactory.getDisplayType(field) == DisplayType.REFERENCE && filterRefField) {
                boolean isApprovalId = field.getName().equalsIgnoreCase(EntityHelper.ApprovalId);
                boolean isBizz = MetadataHelper.isBizzEntity(field.getReferenceEntity());
                if (!(isApprovalId || isBizz)) {
                    continue;
                }
            }

            if (parentField != null) {
                map.put("name", parentField.getName() + "." + map.get("name"));
                map.put("label", EasyMetaFactory.getLabel(parentField) + "." + map.get("label"));
            }

            dest.add(map);
        }
    }

    // 哪些实体引用了指定实体
    @GetMapping("references")
    public List<String[]> references(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        Entity entity = MetadataHelper.getEntity(getParameterNotNull(request, "entity"));

        String permission = getParameter(request, "permission");
        Permission checkPermission = null;
        if (permission != null) {
            checkPermission = PrivilegesManager.parse(permission);
        }

        Set<Entity> references = new HashSet<>();
        for (Field field : entity.getReferenceToFields()) {
            Entity own = field.getOwnEntity();
            if (!(own.getMainEntity() != null || field.getType() == FieldType.ANY_REFERENCE)) {
                references.add(own);
            }
        }

        List<String[]> data = new ArrayList<>();
        for (Entity e : references) {
            if (checkPermission == null
                    || Application.getPrivilegesManager().allow(user, e.getEntityCode(), checkPermission)) {
                EasyEntity easy = EasyMetaFactory.valueOf(e);
                data.add(new String[] { easy.getName(), easy.getLabel() });
            }
        }
        return data;
    }
}
