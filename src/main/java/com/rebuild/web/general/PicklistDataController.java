/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 下拉列表型字段值列表
 *
 * @author ZHAO
 * @since 2019/12/1
 */
@Controller
@RequestMapping("/commons/metadata/")
public class PicklistDataController extends BaseController {

    // for PickList/MultiSelect/State
    @GetMapping({"picklist", "field-options"})
    public void fetchPicklist(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");

        Field fieldMeta = getRealField(entity, field);
        DisplayType dt = EasyMeta.getDisplayType(fieldMeta);

        JSON options;
        if (dt == DisplayType.STATE) {
            options = StateManager.instance.getStateOptions(fieldMeta);
        } else if (dt == DisplayType.MULTISELECT) {
            options = MultiSelectManager.instance.getSelectList(fieldMeta);
        } else {
            options = PickListManager.instance.getPickList(fieldMeta);
        }

        writeSuccess(response, options);
    }

    // for Classification
    @RequestMapping("classification")
    public void fetchClassification(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");

        Field fieldMeta = getRealField(entity, field);
        ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, true);
        if (useClassification == null) {
            writeFailure(response, "分类字段配置有误");
            return;
        }

        ID parent = getIdParameter(request, "parent");
        String sql = "select itemId,name from ClassificationData where dataId = ? and isHide = 'F' and ";
        if (parent != null) {
            sql += "parent = '" + parent + "'";
        } else {
            sql += "parent is null";
        }
        sql += " order by code, name";
        Object[][] data = Application.createQueryNoFilter(sql)
                .setParameter(1, useClassification)
                .setLimit(500)  // 最多显示
                .array();

        writeSuccess(response, data);
    }

    private Field getRealField(String entity, String fieldPath) {
        Entity entityMeta = MetadataHelper.getEntity(entity);
        return MetadataHelper.getLastJoinField(entityMeta, fieldPath);
    }
}
