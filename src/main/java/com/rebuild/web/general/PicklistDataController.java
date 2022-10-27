/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 下拉列表型字段值列表
 *
 * @author ZHAO
 * @since 2019/12/1
 */
@RestController
@RequestMapping("/commons/metadata/")
public class PicklistDataController extends BaseController {

    // for PickList/MultiSelect/State
    @GetMapping({"picklist", "field-options"})
    public JSON fetchOptions(HttpServletRequest request) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");

        Field fieldMeta = getRealField(entity, field);
        DisplayType dt = EasyMetaFactory.getDisplayType(fieldMeta);

        JSON options;
        if (dt == DisplayType.STATE) {
            options = StateManager.instance.getStateOptions(fieldMeta);
        } else if (dt == DisplayType.MULTISELECT) {
            options = MultiSelectManager.instance.getSelectList(fieldMeta);
        } else {
            options = PickListManager.instance.getPickList(fieldMeta);
        }

        return options;
    }

    // for Classification
    @RequestMapping("classification")
    public RespBody fetchClassification(HttpServletRequest request) {
        final String entity = getParameterNotNull(request, "entity");
        final String field = getParameterNotNull(request, "field");

        Field fieldMeta = getRealField(entity, field);
        ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, true);
        if (useClassification == null) {
            return RespBody.errorl("无效分类数据，请联系管理员配置");
        }

        ID parent = getIdParameter(request, "parent");
        String sql = "select itemId,name from ClassificationData where dataId = ? and isHide = 'F' and ";
        if (parent != null) sql += "parent = '" + parent + "'";
        else sql += "parent is null";

        Object[][] data = Application.createQueryNoFilter(sql + " order by code,name")
                .setParameter(1, useClassification)
                .setLimit(500)  // 最多显示
                .array();

        return RespBody.ok(data);
    }

    private Field getRealField(String entity, String fieldPath) {
        Entity entityMeta = MetadataHelper.getEntity(entity);
        return MetadataHelper.getLastJoinField(entityMeta, fieldPath);
    }
}
