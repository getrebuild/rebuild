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
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.PrivilegesManager;
import com.rebuild.core.support.state.StateHelper;
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
        boolean usesDetail = getBoolParameter(request, "detail", false);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities(user, false, usesDetail)) {
            Map<String, Object> map = new HashMap<>();
            EasyMeta easy = new EasyMeta(e);
            map.put("name", e.getName());
            map.put("label", easy.getLabel());
            map.put("icon", easy.getIcon());
            data.add(map);
        }
        return data;
    }

    @GetMapping("fields")
    public List<Map<String, Object>> fields(HttpServletRequest request) {
        String entity = getParameterNotNull(request, "entity");
        Entity entityMeta = MetadataHelper.getEntity(entity);
        boolean appendRefFields = "2".equals(getParameter(request, "deep"));

        List<Map<String, Object>> data = new ArrayList<>();
        putFields(data, entityMeta, appendRefFields);

        // 追加二级引用字段
        if (appendRefFields) {
            for (Field field : entityMeta.getFields()) {
                if (!field.isQueryable() || EasyMeta.getDisplayType(field) != DisplayType.REFERENCE) continue;

                int code = field.getReferenceEntity().getEntityCode();
                if (MetadataHelper.isBizzEntity(code) || code == EntityHelper.RobotApprovalConfig) continue;

                data.add(formatField(field));
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
    private void putFields(List<Map<String, Object>> dest, BaseMeta entityOrField, boolean filterRefField) {
        Field parentField = null;
        Entity useEntity;
        if (entityOrField instanceof Field) {
            parentField = (Field) entityOrField;
            useEntity = parentField.getReferenceEntity();
        } else {
            useEntity = (Entity) entityOrField;
        }

        for (Field field : MetadataSorter.sortFields(useEntity)) {
            Map<String, Object> map = formatField(field);

            // 引用字段处理
            if (EasyMeta.getDisplayType(field) == DisplayType.REFERENCE && filterRefField) {
                boolean isApprovalId = field.getName().equalsIgnoreCase(EntityHelper.ApprovalId);
                boolean isBizz = MetadataHelper.isBizzEntity(field.getReferenceEntity().getEntityCode());
                if (!(isApprovalId || isBizz)) {
                    continue;
                }
            }

            if (parentField != null) {
                map.put("name", parentField.getName() + "." + map.get("name"));
                map.put("label", EasyMeta.getLabel(parentField) + "." + map.get("label"));
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
                EasyMeta easy = new EasyMeta(e);
                data.add(new String[] { easy.getName(), easy.getLabel() });
            }
        }
        return data;
    }

    // --

    /**
     * @param field
     * @return
     */
    public static Map<String, Object> formatField(Field field) {
        Map<String, Object> map = new HashMap<>();
        EasyMeta easyField = EasyMeta.valueOf(field);
        map.put("name", field.getName());
        map.put("label", easyField.getLabel());
        map.put("type", easyField.getDisplayType().name());
        map.put("nullable", field.isNullable());
        map.put("creatable", field.isCreatable());
        map.put("updatable", field.isUpdatable());

        DisplayType dt = EasyMeta.getDisplayType(field);
        if (dt == DisplayType.REFERENCE) {
            Entity refEntity = field.getReferenceEntity();
            Field nameField = MetadataHelper.getNameField(refEntity);
            map.put("ref", new String[]{refEntity.getName(), EasyMeta.getDisplayType(nameField).name()});
        } else if (dt == DisplayType.STATE) {
            map.put("stateClass", StateHelper.getSatetClass(field).getName());
        }
        return map;
    }
}
