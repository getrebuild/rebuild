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
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 表单/视图
 *
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class GeneralModelControl extends EntityController {

    @GetMapping("view/{id}")
    public ModelAndView pageView(@PathVariable String entity, @PathVariable String id,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final Entity thatEntity = MetadataHelper.getEntity(entity);

        if (!Application.getPrivilegesManager().allowRead(user, thatEntity.getEntityCode())) {
            response.sendError(403, "你没有访问此实体的权限");
            return null;
        }

        ID record = ID.valueOf(id);
        ModelAndView mv;
        if (thatEntity.getMainEntity() != null) {
            mv = createModelAndView("/general/detail-view", record, user);
        } else {
            mv = createModelAndView("/general/record-view", record, user);

            JSON vtab = ViewAddonsManager.instance.getViewTab(entity, user);
            mv.getModel().put("ViewTabs", vtab);
            JSON vadd = ViewAddonsManager.instance.getViewAdd(entity, user);
            mv.getModel().put("ViewAdds", vadd);
        }
        mv.getModel().put("id", record);

        return mv;
    }

    @PostMapping("form-model")
    public void entityForm(@PathVariable String entity,
                           HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        ID record = getIdParameter(request, "id");  // New or Update

        JSON initialVal = null;
        if (record == null) {
            initialVal = ServletUtils.getRequestJson(request);
            if (initialVal != null) {
                // 创建明细实体必须指定主实体，以便验证权限
                String mainid = ((JSONObject) initialVal).getString(FormsBuilder.DV_MAINID);
                if (ID.isId(mainid)) {
                    FormsBuilder.setCurrentMainId(ID.valueOf(mainid));
                }
            }
        }

        try {
            JSON model = FormsBuilder.instance.buildForm(entity, user, record);
            // 填充前端设定的初始值
            if (record == null && initialVal != null) {
                FormsBuilder.instance.setFormInitialValue(MetadataHelper.getEntity(entity), model, (JSONObject) initialVal);
            }
            writeSuccess(response, model);
        } finally {
            FormsBuilder.setCurrentMainId(null);
        }
    }

    @GetMapping("view-model")
    public void entityView(@PathVariable String entity,
                           HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        ID record = getIdParameterNotNull(request, "id");
        JSON modal = FormsBuilder.instance.buildView(entity, user, record);
        writeSuccess(response, modal);
    }

    @GetMapping("record-meta")
    public void fetchRecordMeta(HttpServletRequest request, HttpServletResponse response) {
        final ID id = getIdParameterNotNull(request, "id");
        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        String sql = "select createdOn,modifiedOn from %s where %s = '%s'";
        if (MetadataHelper.hasPrivilegesField(entity)) {
            sql = sql.replaceFirst("modifiedOn", "modifiedOn,owningUser");
        }

        sql = String.format(sql, entity.getName(), entity.getPrimaryField().getName(), id);
        Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
        if (recordMeta == null) {
            writeFailure(response, "记录不存在");
            return;
        }

        recordMeta[0] = I18nUtils.formatDate((Date) recordMeta[0]);
        recordMeta[1] = I18nUtils.formatDate((Date) recordMeta[1]);

        String[] owning = null;
        List<String[]> sharingList = null;
        if (recordMeta.length == 3) {
            User user = Application.getUserStore().getUser((ID) recordMeta[2]);
            String dept = user.getOwningDept() == null ? null : user.getOwningDept().getName();
            owning = new String[]{user.getIdentity().toString(), user.getFullName(), dept};

            Object[][] shareTo = Application.createQueryNoFilter(
                    "select shareTo from ShareAccess where belongEntity = ? and recordId = ?")
                    .setParameter(1, entity.getName())
                    .setParameter(2, id)
                    .setLimit(9)  // 最多显示9个
                    .array();
            sharingList = new ArrayList<>();
            for (Object[] st : shareTo) {
                sharingList.add(new String[]{st[0].toString(), UserHelper.getName((ID) st[0])});
            }
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"createdOn", "modifiedOn", "owningUser", "sharingList"},
                new Object[]{recordMeta[0], recordMeta[1], owning, sharingList});
        writeSuccess(response, ret);
    }

    @GetMapping("record-lastModified")
    public void fetchRecordLastModified(HttpServletRequest request, HttpServletResponse response) {
        final ID id = getIdParameterNotNull(request, "id");
        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        String sql = String.format("select modifiedOn from %s where %s = '%s'",
                entity.getName(), entity.getPrimaryField().getName(), id);
        Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
        if (recordMeta == null) {
            writeFailure(response, "NO_EXISTS");
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"lastModified"},
                new Object[]{((Date) recordMeta[0]).getTime()});
        writeSuccess(response, ret);
    }
}
