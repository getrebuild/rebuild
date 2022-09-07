/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.service.general.SeriesReindexTask;
import com.rebuild.core.service.general.series.SeriesGeneratorFactory;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2020/4/30
 */
@RestController
@RequestMapping("/admin/field/")
public class SeriesController extends BaseController {

    @RequestMapping("series-reindex")
    public JSON seriesReindex(@EntityParam Entity entity, HttpServletRequest request) {
        String field = getParameterNotNull(request, "field");
        Field metaField = entity.getField(field);

        SeriesReindexTask seriesReindexTask = new SeriesReindexTask(metaField);
        seriesReindexTask.setUser(getRequestUser(request));

        int reindex = seriesReindexTask.exec();
        return JSONUtils.toJSONObject("reindex", reindex);
    }

    @RequestMapping("series-reset")
    public RespBody seriesReset(@EntityParam Entity entity, HttpServletRequest request) {
        String field = getParameterNotNull(request, "field");
        Field metaField = entity.getField(field);

        SeriesGeneratorFactory.zero(metaField);
        return RespBody.ok();
    }
}
