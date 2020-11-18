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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.general.transform.RecordTransfomer;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 表单功能扩展
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
@RestController
@RequestMapping("/app/entity/extras/")
public class FormExtrasController extends BaseController {

    // 获取表单回填数据
    @RequestMapping("fillin-value")
    public JSON getFillinValue(@EntityParam Entity entity, @IdParam(name = "source") ID sourceRecord,
                               HttpServletRequest request) {
        String field = getParameterNotNull(request, "field");
        Field useField = entity.getField(field);

        return AutoFillinManager.instance.getFillinValue(useField, sourceRecord);
    }

    // 记录转换
    @RequestMapping("transform")
    public RespBody transform(HttpServletRequest request) {
        ID transid = getIdParameterNotNull(request, "transid");
        ID sourceRecord = getIdParameterNotNull(request, "source");

        ConfigBean config = TransformManager.instance.getTransformConfig(transid, null);
        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));

        RecordTransfomer transfomer = new RecordTransfomer(targetEntity, (JSONObject) config.getJSON("config"));
        if (!transfomer.checkFilter(sourceRecord)) {
            return RespBody.error(getLang(request, "TransformNotAllow"), 400);
        }

        ID newId = transfomer.transform(sourceRecord);
        return RespBody.ok(newId);
    }
}
