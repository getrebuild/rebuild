/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 元数据获取
 *
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/commons/metadata/")
public class MetadataGetting extends BaseController {

    @RequestMapping("entities")
    public void entities(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        boolean usesDetail = getBoolParameter(request, "detail", false);

        List<Map<String, String>> list = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities(user, false, usesDetail)) {
            Map<String, String> map = new HashMap<>();
            EasyMeta easy = new EasyMeta(e);
            map.put("name", e.getName());
            map.put("label", easy.getLabel());
            map.put("icon", easy.getIcon());
            list.add(map);
        }
        writeSuccess(response, list);
    }

    @RequestMapping("fields")
    public void fields(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        Entity entityMeta = MetadataHelper.getEntity(entity);
        boolean appendRefFields = "2".equals(getParameter(request, "deep"));

        List<Map<String, Object>> fsList = new ArrayList<>();
        putFields(fsList, entityMeta, appendRefFields);

        // 追加二级引用字段
        if (appendRefFields) {
            for (Field field : entityMeta.getFields()) {
                if (EasyMeta.getDisplayType(field) != DisplayType.REFERENCE) continue;

                int code = field.getReferenceEntity().getEntityCode();
                if (MetadataHelper.isBizzEntity(code) || code == EntityHelper.RobotApprovalConfig) continue;

                fsList.add(buildField(field));
                putFields(fsList, field, false);
            }
        }

        writeSuccess(response, fsList);
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
            Map<String, Object> map = buildField(field);

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

    /**
     * @param field
     * @return
     */
    public static Map<String, Object> buildField(Field field) {
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

    // 哪些实体引用了指定实体
    @RequestMapping("references")
    public void references(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        Entity entityMeta = MetadataHelper.getEntity(entity);

        Set<Entity> references = new HashSet<>();
        for (Field field : entityMeta.getReferenceToFields()) {
            Entity own = field.getOwnEntity();
            if (!(own.getMainEntity() != null || field.getType() == FieldType.ANY_REFERENCE)) {
                references.add(own);
            }
        }

        List<String[]> list = new ArrayList<>();
        for (Entity e : references) {
            EasyMeta easy = new EasyMeta(e);
            list.add(new String[]{easy.getName(), easy.getLabel()});
        }
        writeSuccess(response, list);
    }
}
