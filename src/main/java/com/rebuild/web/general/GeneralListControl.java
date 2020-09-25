/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 数据列表
 *
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class GeneralListControl extends EntityController {

    @GetMapping("list")
    public ModelAndView pageList(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final ID user = getRequestUser(request);
        if (!MetadataHelper.containsEntity(entity) || MetadataHelper.isBizzEntity(entity)) {
            response.sendError(404);
            return null;
        }

        final Entity thatEntity = MetadataHelper.getEntity(entity);

        if (!thatEntity.isQueryable()) {
            response.sendError(404);
            return null;
        }

        if (!Application.getPrivilegesManager().allowRead(user, thatEntity.getEntityCode())) {
            response.sendError(403, getLang(request, "YouNoPermissionAccessSome", "Page"));
            return null;
        }

        ModelAndView mv;
        if (thatEntity.getMainEntity() != null) {
            mv = createModelAndView("/general/detail-list", entity, user);
        } else {
            mv = createModelAndView("/general/record-list", entity, user);
        }

        JSON config = DataListManager.instance.getFieldsLayout(entity, user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));

        // 列表相关权限
        mv.getModel().put(ZeroEntry.AllowCustomDataList.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList));
        mv.getModel().put(ZeroEntry.AllowDataExport.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport));
        mv.getModel().put(ZeroEntry.AllowBatchUpdate.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate));

        return mv;
    }

    @PostMapping("data-list")
    public void dataList(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) {
        JSONObject query = (JSONObject) ServletUtils.getRequestJson(request);

        DataListBuilder builder = new DataListBuilderImpl(query, getRequestUser(request));
        JSON result = builder.getJSONResult();
        writeSuccess(response, result);
    }
}
