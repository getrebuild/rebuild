/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.transform;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

        ModelAndView mv = createModelAndView("/admin/robot/transform-editor");
        mv.getModelMap().put("configId", configId);
        mv.getModelMap().put("config", config.getJSON("config"));

        Entity sourceEntity = MetadataHelper.getEntity(config.getString("source"));
        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));

        // 主实体
        mv.getModelMap().put("sourceEntity", buildEntity(sourceEntity, true));
        mv.getModelMap().put("targetEntity", buildEntity(targetEntity, false));

        // v2.10 目标为主实体
        if (targetEntity.getDetailEntity() != null) {
            if (sourceEntity.getDetailEntity() != null) {
                mv.getModelMap().put("sourceDetailEntity", buildEntity(sourceEntity.getDetailEntity(), true));
            } else {
                mv.getModelMap().put("sourceDetailEntity", buildEntity(sourceEntity, true));
            }
            mv.getModelMap().put("targetDetailEntity", buildEntity(targetEntity.getDetailEntity(), false));
        }

        mv.getModelMap().put("name", config.getString("name"));

        return mv;
    }

    private JSONObject buildEntity(Entity entity, boolean sourceTyp) {
        JSONObject entityData = JSONUtils.toJSONObject(
                new String[] { "entity", "label" },
                new Object[] { entity.getName(), EasyMetaFactory.getLabel(entity) });

        JSONArray fields = new JSONArray();

        for (Field field : MetadataSorter.sortFields(entity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (easyField.getDisplayType() == DisplayType.BARCODE) continue;

            if (sourceTyp) {
                fields.add(easyField.toJSON());
            } else if (!MetadataHelper.isCommonsField(field)) {
                // v2.10 非可创建字段也支持
                fields.add(MetaFormatter.buildRichField(easyField));
            }
        }

        if (sourceTyp) {
            fields.add(EasyMetaFactory.toJSON(entity.getPrimaryField()));
        } else {
            fields.add(EasyMetaFactory.toJSON(entity.getField(EntityHelper.OwningUser)));
        }

        // 二级字段（父级）
        if (sourceTyp && entity.getMainEntity() != null) {
            Field dtf = MetadataHelper.getDetailToMainField(entity);
            JSONArray res = MetaFormatter.buildFields(dtf);
            if (res != null) fields.addAll(res);
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
