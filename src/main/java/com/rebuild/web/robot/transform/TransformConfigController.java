/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.admin.ConfigCommons;
import com.rebuild.web.general.MetaFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录转换配置
 *
 * @author devezhao
 * @since 2020/10/27
 */
@RestController
@RequestMapping("/admin/robot/")
public class TransformConfigController extends BaseController {

    @GetMapping("transforms")
    public ModelAndView pageList() {
        return createModelAndView("/admin/robot/transform-list");
    }

    @GetMapping("transform/{id}")
    public ModelAndView pageEditor(@PathVariable String id, HttpServletResponse response) throws IOException {
        ID configId = ID.isId(id) ? ID.valueOf(id) : null;
        if (configId == null) {
            response.sendError(404);
            return null;
        }

        ConfigBean config;
        try {
            config = TransformManager.instance.getTransformConfig(configId, null);
        } catch (ConfigurationException notExists) {
            response.sendError(404);
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/robot/transform-design");
        mv.getModelMap().put("configId", configId);
        mv.getModelMap().put("config", config.getJSON("config"));

        Entity sourceEntity = MetadataHelper.getEntity(config.getString("source"));
        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));

        mv.getModelMap().put("sourceEntity", buildEntity(sourceEntity, true));
        mv.getModelMap().put("targetEntity", buildEntity(targetEntity, false));

        // 目标为主实体时加入明细
        if (targetEntity.getDetailEntity() != null) {
            if (sourceEntity.getDetailEntity() != null) {
                mv.getModelMap().put("sourceDetailEntity", buildEntity(sourceEntity.getDetailEntity(), true));
                JSONArray sourceDetailEntities = new JSONArray();
                for (Entity de : MetadataSorter.sortDetailEntities(sourceEntity)) {
                    sourceDetailEntities.add(buildEntity(de, true));
                }
                mv.getModelMap().put("sourceDetailEntities", sourceDetailEntities);
            } else {
                // self
                JSONObject self = buildEntity(sourceEntity, true);
                mv.getModelMap().put("sourceDetailEntity", self);

                JSONArray sourceDetailEntities = new JSONArray();
                sourceDetailEntities.add(self);
                mv.getModelMap().put("sourceDetailEntities", sourceDetailEntities);
            }

            mv.getModelMap().put("targetDetailEntity", buildEntity(targetEntity.getDetailEntity(), false));
            JSONArray targetDetailEntities = new JSONArray();
            for (Entity de : MetadataSorter.sortDetailEntities(targetEntity)) {
                targetDetailEntities.add(buildEntity(de, false));
            }
            mv.getModelMap().put("targetDetailEntities", targetDetailEntities);
        }

        mv.getModelMap().put("name", config.getString("name"));
        mv.getModelMap().put("isDisabled", config.getBoolean("disabled"));

        // v3.1 明细导入
        if (targetEntity.getMainEntity() != null) {
            mv.getModelMap().put("targetIsDetail", true);
        }

        return mv;
    }

    @GetMapping("transform/imports-filter-fields")
    public RespBody getImportsFilterFields(@IdParam ID configId) {
        ConfigBean cb = TransformManager.instance.getTransformConfig(configId, null);

        Entity sourceEntity = MetadataHelper.getEntity(cb.getString("source"));
        Entity targetEntity = MetadataHelper.getEntity(cb.getString("target"));
        Entity sourceEntityMain = sourceEntity.getMainEntity();
        Entity targetEntityMain = targetEntity.getMainEntity();

        // 1. 源
        List<String[]> allowedSourceFields = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
            String type = "REFERENCE:" + field.getReferenceEntity().getName();
            allowedSourceFields.add(new String[]{ field.getName(), EasyMetaFactory.getLabel(field), type });
        }
        if (sourceEntityMain != null) {
            Field dtmfField = MetadataHelper.getDetailToMainField(sourceEntity);
            String namePrefix = dtmfField.getName() + ".";
            String labelPrefix = EasyMetaFactory.getLabel(dtmfField) + ".";

            for (Field field : MetadataSorter.sortFields(sourceEntityMain, DisplayType.REFERENCE)) {
                String type = "REFERENCE:" + field.getReferenceEntity().getName();
                allowedSourceFields.add(new String[]{
                        namePrefix + field.getName(), labelPrefix + EasyMetaFactory.getLabel(field), type });
            }
        }

        // 2.目标
        List<String[]> allowedTargetFields = new ArrayList<>();

        Field dtmfField = MetadataHelper.getDetailToMainField(targetEntity);
        String namePrefix = dtmfField.getName() + ".";
        String labelPrefix = EasyMetaFactory.getLabel(dtmfField) + ".";

        for (Field field : MetadataSorter.sortFields(targetEntityMain, DisplayType.REFERENCE)) {
            if (MetadataHelper.isCommonsField(field)) continue;

            String type = "REFERENCE:" + field.getReferenceEntity().getName();
            allowedTargetFields.add(new String[]{ namePrefix + field.getName(),
                    labelPrefix + EasyMetaFactory.getLabel(field), type });
        }

        JSON res = JSONUtils.toJSONObject(
                new String[] { "sourceFields", "targetFields" },
                new Object[] { allowedSourceFields, allowedTargetFields });
        return RespBody.ok(res);
    }

    private JSONObject buildEntity(Entity entity, boolean sourceType) {
        JSONObject entityData = JSONUtils.toJSONObject(
                new String[] { "entity", "label" },
                new Object[] { entity.getName(), EasyMetaFactory.getLabel(entity) });

        JSONArray fields;
        // 源
        if (sourceType) {
            fields = MetaFormatter.buildFieldsWithRefs(entity, 3, true, 1, field -> {
                if (field instanceof EasyField) {
                    EasyField easyField = (EasyField) field;
                    int c = easyField.getDisplayType() == DisplayType.REFERENCE
                            ? easyField.getRawMeta().getReferenceEntity().getEntityCode() : 0;

                    return easyField.getDisplayType() == DisplayType.BARCODE
                            || c == EntityHelper.RobotApprovalConfig;
                }
                return false;
            });
        }
        // 目标
        else {
            fields = MetaFormatter.buildFieldsWithRefs(entity, 1, true, field -> {
                EasyField easyField = (EasyField) field;
                return easyField.getDisplayType() == DisplayType.BARCODE
                        || easyField.getDisplayType() == DisplayType.SERIES
                        || MetadataHelper.isCommonsField(easyField.getRawMeta());
            });

            if (entity.containsField(EntityHelper.OwningUser)) {
                fields.add(EasyMetaFactory.toJSON(entity.getField(EntityHelper.OwningUser)));
            }

            // v3.4 for dtf
            if (entity.getMainEntity() != null) {
                String dtfName = MetadataHelper.getDetailToMainField(entity).getName();
                for (Object o : fields) {
                    JSONObject item = (JSONObject) o;
                    if (dtfName.equals(item.getString("name"))) {
                        item.put("readonly", true);
                        break;
                    }
                }
            }
        }

        entityData.put("fields", fields);
        return entityData;
    }

    @RequestMapping("transform/list")
    public Object[][] transformList(HttpServletRequest request) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");

        String sql = "select configId,belongEntity,belongEntity,targetEntity,targetEntity,modifiedOn,name,isDisabled from TransformConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] data = ConfigCommons.queryListOfConfig(sql, belongEntity, q);
        for (Object[] o : data) {
            o[4] = EasyMetaFactory.getLabel(MetadataHelper.getEntity((String) o[4]));
        }
        return data;
    }
}
