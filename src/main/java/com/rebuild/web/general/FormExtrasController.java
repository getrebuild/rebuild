/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 表单功能扩展
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
@Controller
@RequestMapping("/app/entity/extras/")
public class FormExtrasController extends BaseController {

    /**
     * 获取表单回填值
     *
     * @param request
     * @param response
     */
    @RequestMapping("fillin-value")
    public void getFillinValue(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");
        ID source = getIdParameterNotNull(request, "source");

        JSON ret = AutoFillinManager.instance.getFillinValue(MetadataHelper.getField(entity, field), source);
        writeSuccess(response, ret);
    }
}
