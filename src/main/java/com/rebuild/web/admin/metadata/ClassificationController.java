/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
@Controller
@RequestMapping("/admin/metadata/")
public class ClassificationController extends BaseController {

    @GetMapping("classifications")
    public ModelAndView page() {
        return createModelAndView("/admin/metadata/classification-list");
    }

    @RequestMapping("classification/{id}")
    public ModelAndView page(@PathVariable String id,
                             HttpServletRequest request, HttpServletResponse resp) throws IOException {
        Object[] data = Application.createQuery(
                "select name,openLevel from Classification where dataId = ?")
                .setParameter(1, ID.valueOf(id))
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
    public void list(HttpServletResponse resp) {
        Object[][] array = Application.createQuery(
                "select dataId,name,isDisabled,openLevel from Classification order by name")
                .array();
        writeSuccess(resp, array);
    }

    @RequestMapping("classification/info")
    public void info(HttpServletRequest request, HttpServletResponse resp) {
        ID dataId = getIdParameterNotNull(request, "id");
        Object[] data = Application.createQuery(
                "select name from Classification where dataId = ?")
                .setParameter(1, dataId)
                .unique();

        if (data == null) {
            writeFailure(resp, getLang(request, "SomeNotExists", "Classification"));
        } else {
            writeSuccess(resp, JSONUtils.toJSONObject("name", data[0]));
        }
    }

    @RequestMapping("classification/save-data-item")
    public void saveDataItem(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        ID itemId = getIdParameter(request, "item_id");
        ID dataId = getIdParameter(request, "data_id");

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
            writeFailure(response, getLang(request, "InvalidParams"));
            return;
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
        writeSuccess(response, item.getPrimary());
    }

    @RequestMapping("classification/delete-data-item")
    public void deleteDataItem(HttpServletRequest request, HttpServletResponse response) {
        ID itemId = getIdParameter(request, "item_id");
        Application.getBean(ClassificationService.class).deleteItem(itemId);
        writeSuccess(response);
    }

    @RequestMapping("classification/load-data-items")
    public void loadDataItems(HttpServletRequest request, HttpServletResponse response) {
        ID dataId = getIdParameterNotNull(request, "data_id");
        ID parent = getIdParameter(request, "parent");

        Object[][] child;
        if (parent != null) {
            child = Application.createQuery(
                    "select itemId,name,code,isHide from ClassificationData where dataId = ? and parent = ? order by code,name")
                    .setParameter(1, dataId)
                    .setParameter(2, parent)
                    .array();
        } else if (dataId != null) {
            child = Application.createQuery(
                    "select itemId,name,code,isHide from ClassificationData where dataId = ? and parent is null order by code,name")
                    .setParameter(1, dataId)
                    .array();
        } else {
            writeFailure(response, getLang(request, "InvalidParams"));
            return;
        }
        writeSuccess(response, child);
    }
}
