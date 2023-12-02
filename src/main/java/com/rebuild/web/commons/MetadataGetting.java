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
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesManager;
import com.rebuild.web.BaseController;
import com.rebuild.web.general.MetaFormatter;
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
    public JSON fields(HttpServletRequest request) {
        Entity entity = MetadataHelper.getEntity(getParameterNotNull(request, "entity"));
        // 返回引用实体的字段层级
        int appendRefFields = getIntParameter(request, "deep", 0);
        // 返回ID主键字段
        boolean forceWithId = getBoolParameter(request, "withid");

        return MetaFormatter.buildFieldsWithRefs(entity, appendRefFields, true, forceWithId, field -> {
            if (!field.isQueryable()) return true;

            if (field instanceof Field) {
                int c = ((Field) field).getReferenceEntity().getEntityCode();
                if (MetadataHelper.isBizzEntity(c) || c == EntityHelper.RobotApprovalConfig) {
                    return !(field.getName().equals(EntityHelper.OwningUser)
                            || field.getName().equals(EntityHelper.OwningDept)
                            || field.getName().equals(EntityHelper.ApprovalLastUser));
                }
            }
            return false;
        });
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
