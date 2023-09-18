/*!
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
import com.alibaba.fastjson.JSON;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 元数据获取
 *
 * @author Zixin (RB)
 * @since 09/19/2018
 */
@RestController
@RequestMapping("/commons/metadata/")
public class MetadataGetting extends BaseController {

    @GetMapping("entities")
    public List<JSON> entities(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        boolean usesBizz = getBoolParameter(request, "bizz", false);
        boolean usesDetail = getBoolParameter(request, "detail", false);

        List<JSON> data = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities(user, usesBizz, usesDetail)) {
            JSONObject item = (JSONObject) EasyMetaFactory.valueOf(e).toJSON();
            item.put("name", item.getString("entity"));
            item.put("label", item.getString("entityLabel"));
            data.add(item);
        }
        return data;
    }

    @GetMapping("fields")
    public List<JSONObject> fields(HttpServletRequest request) {
        Entity entityMeta = MetadataHelper.getEntity(getParameterNotNull(request, "entity"));
        // 返回引用实体的字段
        int appendRefFields = getIntParameter(request, "deep", 0);

        List<JSONObject> into = new ArrayList<>();
        putFields(into, entityMeta, null);

        List<Object[]> deep3 = new ArrayList<>();

        // 追加二级引用字段
        if (appendRefFields >= 2) {
            for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
                if (!isAllowAppendRefFields(field)) continue;

                String fieldName = field.getName();
                String fieldLabel = EasyMetaFactory.getLabel(field);
                putFields(into, field.getReferenceEntity(), new String[] { fieldName, fieldLabel });

                if (appendRefFields < 3) continue;

                // v35 追加三级引用字段
                for (Field field3 : MetadataSorter.sortFields(field.getReferenceEntity(), DisplayType.REFERENCE)) {
                    if (!isAllowAppendRefFields(field)) continue;

                    deep3.add(new Object[] { fieldName, fieldLabel, field3 });
                }
            }

            if (!deep3.isEmpty()) {
                for (Object[] e : deep3) {
                    Field field3 = (Field) e[2];
                    String fieldName = e[0] + "." + field3.getName();
                    String fieldLabel = e[1] + "." + EasyMetaFactory.getLabel(field3);
                    putFields(into, field3.getReferenceEntity(), new String[] { fieldName, fieldLabel });
                }
            }
        }

        return into;
    }

    private boolean isAllowAppendRefFields(Field field) {
        if (!field.isQueryable()) return false;

        int code = field.getReferenceEntity().getEntityCode();
        if (MetadataHelper.isBizzEntity(code) || code == EntityHelper.RobotApprovalConfig) {
            // NOTE 特殊放行
            return field.getName().equals(EntityHelper.OwningUser)
                    || field.getName().equals(EntityHelper.OwningDept)
                    || field.getName().equals(EntityHelper.ApprovalLastUser);
        }
        return true;
    }

    private void putFields(List<JSONObject> dest, Entity useEntity, String[] parentsField) {
        final String parentRefField = parentsField == null ? null : parentsField[0];
        final String parentRefFieldLabel = parentsField == null ? null : parentsField[1];

        for (Field field : MetadataSorter.sortFields(useEntity)) {
            if (!field.isQueryable()) continue;

            JSONObject map = (JSONObject) EasyMetaFactory.valueOf(field).toJSON();
            if (parentRefField != null) {
                map.put("name", parentRefField + "." + map.get("name"));
                map.put("label", parentRefFieldLabel + "." + map.get("label"));
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
            // FIXME N2N 支持 ???
            if (field.getOwnEntity().getMainEntity() == null && field.getType() == FieldType.REFERENCE) {
                references.add(field.getOwnEntity());
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
