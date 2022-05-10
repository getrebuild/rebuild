/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.EntityController;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 数据列表
 *
 * @author Zixin (RB)
 * @since 08/22/2018
 */
@RestController
@RequestMapping("/app/{entity}/")
public class GeneralListController extends EntityController {

    @GetMapping("list")
    public ModelAndView pageList(@PathVariable String entity,
                                 HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final ID user = getRequestUser(request);
        final Entity listEntity = checkPageOfEntity(user, entity, response);
        if (listEntity == null) return null;

        final EasyEntity easyEntity = EasyMetaFactory.valueOf(listEntity);
        // 使用主实体列表配置
        final EasyEntity mainEntity = listEntity.getMainEntity() == null
                ? easyEntity : EasyMetaFactory.valueOf(listEntity.getMainEntity());

        String listPage = listEntity.getMainEntity() != null ? "/general/detail-list" : "/general/record-list";
        Integer listMode = getIntParameter(request, "forceListMode");
        if (listMode == null) {
            listMode = ObjectUtils.toInt(mainEntity.getExtraAttr(EasyEntityConfigProps.ADV_LIST_MODE), 1);
        }
        if (listMode == 2) {
            listPage = "/general/record-list2";  // Mode2
        }

        ModelAndView mv = createModelAndView(listPage, entity, user);

        // 列表相关权限
        mv.getModel().put(ZeroEntry.AllowCustomDataList.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList));
        mv.getModel().put(ZeroEntry.AllowDataExport.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport));
        mv.getModel().put(ZeroEntry.AllowBatchUpdate.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate));

        JSON listConfig = null;

        if (listMode == 1) {
            listConfig = DataListManager.instance.getFieldsLayout(entity, user);

            // 扩展配置
            String advListHideFilters = mainEntity.getExtraAttr(EasyEntityConfigProps.ADV_LIST_HIDE_FILTERS);
            String advListHideCharts = mainEntity.getExtraAttr(EasyEntityConfigProps.ADV_LIST_HIDE_CHARTS);
            mv.getModel().put(EasyEntityConfigProps.ADV_LIST_HIDE_FILTERS, advListHideFilters);
            mv.getModel().put(EasyEntityConfigProps.ADV_LIST_HIDE_CHARTS, advListHideCharts);
            mv.getModel().put("hideAside",
                    BooleanUtils.toBoolean(advListHideFilters) && BooleanUtils.toBoolean(advListHideCharts));

        } else if (listMode == 2) {
            listConfig = DataListManager.instance.getFieldsLayoutMode2(listEntity);

            // 明细列表
            if (listEntity.getMainEntity() != null) mv.getModel().put("DataListType", "DetailList");
        }

        mv.getModel().put("DataListConfig", JSON.toJSONString(listConfig));

        // 快速查询
        mv.getModel().put("quickFieldsLabel", getQuickFieldsLabel(listEntity));

        return mv;
    }

    @PostMapping("data-list")
    public JSON dataList(@PathVariable String entity, HttpServletRequest request) {
        JSONObject query = (JSONObject) ServletUtils.getRequestJson(request);
        DataListBuilder builder = new DataListBuilderImpl(query, getRequestUser(request));
        return builder.getJSONResult();
    }

    // 检查实体页面
    static Entity checkPageOfEntity(ID user, String entity, HttpServletResponse response) throws IOException {
        if (!MetadataHelper.containsEntity(entity)) {
            response.sendError(404);
            return null;
        }

        final Entity checkEntity = MetadataHelper.getEntity(entity);
        if (!checkEntity.isQueryable()) {
            response.sendError(404);
            return null;
        }

        if (!Application.getPrivilegesManager().allowRead(user, checkEntity.getEntityCode())) {
            response.sendError(403, Language.L("你没有访问此页面的权限"));
            return null;
        }

        return checkEntity;
    }

    // 快速查询
    static String getQuickFieldsLabel(Entity entity) {
        Set<String> quickFields = ParseHelper.buildQuickFields(entity, null);

        List<String> quickFieldsLabel = new ArrayList<>();
        for (String qf : quickFields) {
            if (qf.equalsIgnoreCase(EntityHelper.QuickCode)) continue;
            if (qf.startsWith("&")) qf = qf.substring(1);
            quickFieldsLabel.add(EasyMetaFactory.getLabel(entity, qf));
        }

        return StringUtils.join(quickFieldsLabel, " / ");
    }
}
