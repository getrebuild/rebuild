/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONAware;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 分类数据管理
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/27
 */
@RestController
@RequestMapping("/admin/metadata/")
public class ClassificationController extends BaseController {

    @GetMapping("classifications")
    public ModelAndView page() {
        return createModelAndView("/admin/metadata/classification-list");
    }

    @RequestMapping("classification/{id}")
    public ModelAndView page(@PathVariable ID id,
                             HttpServletRequest request, HttpServletResponse resp) throws IOException {
        Object[] data = Application.createQuery(
                "select name,openLevel from Classification where dataId = ?")
                .setParameter(1, id)
                .unique();
        if (data == null) {
            resp.sendError(404, getLang(request, "SomeNotExists", "Classification"));
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/metadata/classification-editor");
        mv.getModel().put("dataId", id);
        mv.getModel().put("name", data[0]);
        mv.getModel().put("openLevel", data[1]);
        return mv;
    }

    @RequestMapping("classification/list")
    public Object[][] list() {
        return Application.createQuery(
                "select dataId,name,isDisabled,openLevel from Classification order by name")
                .array();
    }

    @RequestMapping("classification/info")
    public JSONAware info(@IdParam ID classId) {
        Object[] data = Application.createQuery(
                "select name,openLevel from Classification where dataId = ?")
                .setParameter(1, classId)
                .unique();

        if (data == null) {
            return RespBody.errorl("SomeNotExists,Classification");
        } else {
            return JSONUtils.toJSONObject(new String[] { "name", "openLevel" }, data);
        }
    }

    @RequestMapping("classification/save-data-item")
    public JSONAware saveDataItem(@IdParam(name = "item_id", required = false) ID itemId,
                                  @IdParam(name = "data_id", required = false) ID dataId,
                                  HttpServletRequest request) {
        final ID user = getRequestUser(request);

        Record item;
        if (itemId != null) {
            item = EntityHelper.forUpdate(itemId, user);
        } else if (dataId != null) {
            ID parent = getIdParameter(request, "parent");
            int level = getIntParameter(request, "level", 0);

            item = EntityHelper.forNew(EntityHelper.ClassificationData, user);
            item.setID("dataId", dataId);
            if (parent != null) {
                item.setID("parent", parent);
            }
            item.setInt("level", level);
        } else {
            return RespBody.errorl("InvalidParams");
        }

        String code = getParameter(request, "code");
        String name = getParameter(request, "name");
        String hide = getParameter(request, "hide");
        if (StringUtils.isNotBlank(code)) {
            item.setString("code", code);
        }
        if (StringUtils.isNotBlank(name)) {
            item.setString("name", name);
        }
        if (StringUtils.isNotBlank(hide)) {
            item.setBoolean("isHide", BooleanUtils.toBooleanObject(hide));
        }

        item = Application.getBean(ClassificationService.class).createOrUpdateItem(item);
        return RespBody.ok(item.getPrimary());
    }

    @RequestMapping("classification/delete-data-item")
    public RespBody deleteDataItem(@IdParam(name = "item_id") ID itemId) {
        Application.getBean(ClassificationService.class).deleteItem(itemId);
        return RespBody.ok();
    }

    @RequestMapping("classification/load-data-items")
    public RespBody loadDataItems(@IdParam(name = "data_id", required = false) ID dataId,
                                  @IdParam(name = "parent", required = false) ID parentId) {
        Object[][] child;
        if (parentId != null) {
            child = Application.createQuery(
                    "select itemId,name,code,isHide from ClassificationData where dataId = ? and parent = ? order by code,name")
                    .setParameter(1, dataId)
                    .setParameter(2, parentId)
                    .array();
        } else if (dataId != null) {
            child = Application.createQuery(
                    "select itemId,name,code,isHide from ClassificationData where dataId = ? and parent is null order by code,name")
                    .setParameter(1, dataId)
                    .array();
        } else {
            return RespBody.errorl("InvalidParams");
        }

        return RespBody.ok(child);
    }
}
