/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.audit;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO 修改历史
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/admin/audit/")
public class RevisionHistoryControl extends EntityController {

    @GetMapping("revision-history")
    public ModelAndView page() {
        return createModelAndView("/admin/audit/revision-history");
    }

    @RequestMapping("revision-history/details")
    public void details(HttpServletRequest request, HttpServletResponse response) {
        ID id = getIdParameterNotNull(request, "id");
        Object[] rev = Application.createQueryNoFilter(
                "select revisionContent,belongEntity from RevisionHistory where revisionId = ?")
                .setParameter(1, id)
                .unique();

        JSONArray data = JSON.parseArray((String) rev[0]);

        // 字段名称
        if (MetadataHelper.containsEntity((String) rev[1])) {
            Entity entity = MetadataHelper.getEntity((String) rev[1]);
            for (Object o : data) {
                JSONObject item = (JSONObject) o;
                String field = item.getString("field");
                if (entity.containsField(field)) {
                    field = EasyMeta.getLabel(entity.getField(field));
                } else {
                    field = "[" + field.toUpperCase() + "]";
                }
                item.put("field", field);
            }
        }

        writeSuccess(response, data);
    }
}
