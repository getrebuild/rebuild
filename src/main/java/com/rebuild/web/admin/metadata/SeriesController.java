/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.general.SeriesReindexTask;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author devezhao
 * @since 2020/4/30
 */
@Controller
@RequestMapping("/admin/field/")
public class SeriesController extends BaseController {

    @RequestMapping("series-reindex")
    public void seriesReindex(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");

        Field metaField = MetadataHelper.getField(entity, field);
        SeriesReindexTask seriesReindexTask = new SeriesReindexTask(metaField);
        seriesReindexTask.setUser(getRequestUser(request));

        int reindex = seriesReindexTask.exec();
        writeJSON(response, JSONUtils.toJSONObject("reindex", reindex));
    }
}
