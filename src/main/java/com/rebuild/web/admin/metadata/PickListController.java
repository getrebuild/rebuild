/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.configuration.general.PickListService;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Zixin (RB)
 * @since 09/06/2018
 */
@Controller
@RequestMapping("/admin/field/")
public class PickListController extends BaseController {

    @RequestMapping({"picklist-gets", "multiselect-gets"})
    public void picklistGet(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");
        boolean isAll = "true".equals(getParameter(request, "isAll"));

        Field fieldMeta = MetadataHelper.getField(entity, field);
        ConfigBean[] entries = PickListManager.instance.getPickListRaw(fieldMeta, isAll);

        writeSuccess(response, JSONUtils.toJSONArray(entries));
    }

    @RequestMapping({"picklist-sets", "multiselect-sets"})
    public void picklistSet(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");
        JSONObject config = (JSONObject) ServletUtils.getRequestJson(request);

        Field fieldMeta = MetadataHelper.getField(entity, field);
        Application.getBean(PickListService.class).updateBatch(fieldMeta, config);
        writeSuccess(response, null);
    }
}