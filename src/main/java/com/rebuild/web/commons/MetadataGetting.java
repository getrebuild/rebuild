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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyField;
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
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@RestController
@RequestMapping("/commons/metadata/")
public class MetadataGetting extends BaseController {

    @GetMapping("entities")
    public List<JSON> entities(HttpServletRequest request) {
        ID user = getRequestUser(request);
        // 返回明细实体
        boolean usesDetail = getBoolParameter(request, "detail", false);

        List<JSON> data = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities(user, false, usesDetail)) {
            JSONObject item = (JSONObject) EasyMetaFactory.valueOf(e).toJSON();
            item.put("name", item.getString("entity"));
            item.put("label", item.getString("entityLabel"));
            data.add(item);
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
            for (Field field : MetadataSorter.sortFields(entityMeta, DisplayType.REFERENCE)) {
                if (!field.isQueryable()) continue;

                int code = field.getReferenceEntity().getEntityCode();
                if (MetadataHelper.isBizzEntity(code) || code == EntityHelper.RobotApprovalConfig) continue;

                data.add(EasyMetaFactory.toJSON(field));
                putFields(data, field, false);
            }
        }

//        // 高级查询追加虚拟字段
//        if ("VF_USER_TEAMS".equalsIgnoreCase(getParameter(request, "extra"))) {
//            final JSONObject temp = JSONUtils.toJSONObject(
//                    new String[] { "name", "label", "type", "ref" },
//                    new Object[] { null, "." + Language.L("JoinedTeams"), "REFERENCE", new String[] { "Team", "TEXT" } });
//
//            List<JSONObject> dataNew = new ArrayList<>();
//            for (JSONObject item : data) {
//                dataNew.add(item);
//                JSONArray ref = item.getJSONArray("ref");
//                if (ref != null && ref.get(0).equals("User") && !item.getString("name").contains(".")) {
//                    JSONObject clone = (JSONObject) temp.clone();
//                    clone.put("name", item.getString("name") + ParseHelper.VF_USER_TEAMS);
//                    clone.put("label", item.getString("label") + clone.getString("label"));
//                    dataNew.add(clone);
//                }
//            }
//            data = dataNew;
//        }

        return data;
    }

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
            if (!field.isQueryable()) continue;

            EasyField easyField = EasyMetaFactory.valueOf(field);

            // 特殊引用字段处理
            if (easyField.getDisplayType() == DisplayType.REFERENCE && filterRefField) {
                boolean isApprovalId = field.getName().equalsIgnoreCase(EntityHelper.ApprovalId);
                boolean isBizz = MetadataHelper.isBizzEntity(field.getReferenceEntity());
                if (!(isApprovalId || isBizz)) continue;
            }

            JSONObject map = (JSONObject) easyField.toJSON();
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
