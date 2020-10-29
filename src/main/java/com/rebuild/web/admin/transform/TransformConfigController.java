/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.admin.data.ReportTemplateController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 记录转换配置
 *
 * @author devezhao
 * @since 2020/10/27
 */
@Controller
@RequestMapping("/admin/")
public class TransformConfigController extends BaseController {

    @GetMapping("transforms")
    public ModelAndView pageList() {
        return createModelAndView("/admin/transform/transform-list");
    }

    @GetMapping("transform/{id}")
    public ModelAndView pageEditor(@PathVariable String id, HttpServletResponse response) throws IOException {
        ModelAndView mv = createModelAndView("/admin/transform/transform-editor");

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

        mv.getModelMap().put("configId", configId);
        mv.getModelMap().put("config", config.getJSON("config"));

        Entity sourceEntity = MetadataHelper.getEntity(config.getString("source"));
        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));

        // 主实体
        mv.getModelMap().put("sourceEntity", buildEntity(sourceEntity, true));
        mv.getModelMap().put("targetEntity", buildEntity(targetEntity, false));

        // 明细（两个实体均有明细才返回）
        if (sourceEntity.getDetailEntity() != null && targetEntity.getDetailEntity() != null) {
            mv.getModelMap().put("sourceDetailEntity", buildEntity(sourceEntity.getDetailEntity(), true));
            mv.getModelMap().put("targetDetailEntity", buildEntity(targetEntity.getDetailEntity(), false));
        }

        return mv;
    }

    private JSONObject buildEntity(Entity entity, boolean isSource) {
        JSONObject entityData = JSONUtils.toJSONObject(
                new String[] { "entity", "label" },
                new Object[] { entity.getName(), EasyMeta.getLabel(entity) });

        JSONArray fields = new JSONArray();
        if (isSource) {
            fields.add(buildField(entity.getPrimaryField()));
        }

        for (Field field : MetadataSorter.sortFields(entity)) {
            if (!isSource && !field.isCreatable()) continue;
            fields.add(buildField(field));
        }
        entityData.put("fields", fields);

        return entityData;
    }

    private JSONObject buildField(Field field) {
        EasyMeta easyMeta = EasyMeta.valueOf(field);
        JSONObject item = JSONUtils.toJSONObject(
                new String[] { "id", "text", "nullable" },
                new Object[] { field.getName(), easyMeta.getLabel(), field.isNullable() });

        String fullType = easyMeta.getDisplayType(false);
        if (DisplayType.REFERENCE.name().equals(fullType)) {
            fullType += "." + field.getReferenceEntity().getName();
        } else if (DisplayType.STATE.name().equals(fullType)) {
            fullType += "." + easyMeta.getExtraAttr(FieldExtConfigProps.STATE_STATECLASS);
        } else if (DisplayType.CLASSIFICATION.name().equals(fullType)) {
            fullType += "." + easyMeta.getExtraAttr(FieldExtConfigProps.CLASSIFICATION_USE);
        } else if (DisplayType.ID.name().equals(fullType)) {
            fullType = DisplayType.REFERENCE.name() + "." + field.getOwnEntity().getName();
        }
        item.put("type", fullType);
        return item;
    }

    @ResponseBody
    @RequestMapping("transform/list")
    public Object[][] transformList(HttpServletRequest request) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");

        String sql = "select configId,belongEntity,belongEntity,targetEntity,targetEntity,modifiedOn,name,isDisabled from TransformConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] data = ReportTemplateController.queryListOfConfig(sql, belongEntity, q);
        for (Object[] o : data) {
            o[4] = EasyMeta.getLabel(MetadataHelper.getEntity((String) o[4]));
        }
        return data;
    }

}
