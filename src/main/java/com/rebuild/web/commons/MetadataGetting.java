/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.PrivilegesManager;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.general.MetaFormatter;
import org.apache.commons.lang3.math.NumberUtils;
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

        // 根据不同的 referer 返回不同的字段列表
        // 返回 ID 主键字段
        String referer = getParameter(request, "referer");
        int forceWith = "withid".equals(referer) ? 1 : 0;

        return MetaFormatter.buildFieldsWithRefs(entity, appendRefFields, true, forceWith, field -> {
            if (!field.isQueryable()) return true;

            if (field instanceof Field) {
                int c = ((Field) field).getReferenceEntity().getEntityCode();
                if (c == EntityHelper.RobotApprovalConfig) return true;
                if (c == EntityHelper.User || c == EntityHelper.Department) {
                    return field.getName().equals(EntityHelper.CreatedBy) || field.getName().equals(EntityHelper.ModifiedBy);
                }
            }
            return false;
        });
    }

    // 哪些实体引用了指定实体（即相关项）
    @GetMapping("references")
    public List<String[]> references(@EntityParam Entity entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        String permission = getParameter(request, "permission");
        Permission checkPermission = null;
        if (permission != null) {
            checkPermission = PrivilegesManager.parse(permission);
        }

        Set<String> unique = new HashSet<>();
        List<String[]> data = new ArrayList<>();

        for (Field field : entity.getReferenceToFields()) {
            Entity ownEntity = field.getOwnEntity();
            if (unique.contains(ownEntity.getName())) continue;
            // 排除明细
            if (ownEntity.getMainEntity() != null) continue;

            EasyField easyField = EasyMetaFactory.valueOf(field);
            boolean isN2N = easyField.getDisplayType() == DisplayType.N2NREFERENCE;
            if (easyField.getDisplayType() == DisplayType.REFERENCE || isN2N) {
                // 权限
                if (checkPermission == null
                        || Application.getPrivilegesManager().allow(user, ownEntity.getEntityCode(), checkPermission)) {

                    // NOTE 是否需要显示到字段级别（目前是实体识别）??? 可能有多个字段引用同一实体

                    String label = EasyMetaFactory.getLabel(ownEntity);
//                    if (isN2N) label += " (N)";
                    data.add(new String[]{ ownEntity.getName(), label });
                    unique.add(ownEntity.getName());
                }
            }
        }
        return data;
    }

    @GetMapping("meta-info")
    public JSON metaInfo(HttpServletRequest request) {
        // Entity:Name,Code, or Field
        final String name = getParameterNotNull(request, "name");

        // Field: X.X
        if (name.contains(".")) {
            String[] ss = name.split("\\.");
            Field foundField = MetadataHelper.getField(ss[0], ss[1]);
            return EasyMetaFactory.valueOf(foundField).toJSON();
        }

        Entity foundEntity;
        if (NumberUtils.isDigits(name)) foundEntity = MetadataHelper.getEntity(Integer.parseInt(name));
        else foundEntity = MetadataHelper.getEntity(name);

        return EasyMetaFactory.valueOf(foundEntity).toJSON();
    }
}
