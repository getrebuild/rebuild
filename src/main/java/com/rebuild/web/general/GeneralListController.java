/*!
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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.configuration.general.EasyActionManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.web.EntityController;
import com.rebuild.web.KnownExceptionConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@Slf4j
@RestController
@RequestMapping("/app/{entity}/")
public class GeneralListController extends EntityController {

    @GetMapping("list")
    public ModelAndView pageList(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final ID user = getRequestUser(request);
        int status = getCanAccessStatus(entity, user, response);
        if (status > 0) return null;

        final Entity listEntity = MetadataHelper.getEntity(entity);
        final EasyEntity easyEntity = EasyMetaFactory.valueOf(listEntity);

        int listMode = ObjectUtils.toInt(easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE), 1);
        int listModeForce = getIntParameter(request, "mode", 0);
        if (listModeForce >= 1 && listModeForce <= 3) listMode = listModeForce;
        String listPage = listEntity.getMainEntity() != null ? "/general/detail-list" : "/general/record-list";
        if (listMode == 2) listPage = "/general/record-list2";  // Mode2
        if (listMode == 3) listPage = "/general/record-list3";  // Mode3

        ModelAndView mv = createModelAndView(listPage, entity, user);

        // 列表相关权限
        mv.getModel().put(ZeroEntry.AllowCustomDataList.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomDataList));
        mv.getModel().put(ZeroEntry.AllowDataExport.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport));
        // v3.3
        if (Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataImport)) {
            boolean allow = listEntity.getMainEntity() == null
                    ? Application.getPrivilegesManager().allowCreate(user, listEntity.getEntityCode())
                    : Application.getPrivilegesManager().allowUpdate(user, listEntity.getMainEntity().getEntityCode());
            mv.getModel().put(ZeroEntry.AllowDataImport.name(), allow);
        }
        mv.getModel().put(ZeroEntry.AllowBatchUpdate.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate));

        // v3.5
        boolean hadApproval = RobotApprovalManager.instance.hadApproval(listEntity, null) != null;
        mv.getModel().put("_HadApproval", hadApproval);

        JSON listConfig = null;

        if (listMode == 1) {
            listConfig = DataListManager.instance.getListFields(entity, user);

            // 侧栏

            String advListShowCategory = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
            String advListHideFilters = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_HIDE_FILTERS);
            String advListHideCharts = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_HIDE_CHARTS);
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY, StringUtils.isNotBlank(advListShowCategory));
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_HIDE_FILTERS, advListHideFilters);
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_HIDE_CHARTS, advListHideCharts);

            mv.getModel().put("hideAside",
                    BooleanUtils.toBoolean(advListHideFilters) && BooleanUtils.toBoolean(advListHideCharts) && StringUtils.isBlank(advListShowCategory));

            // 查询面板

            String advListFilterPane = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_FILTERPANE);
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_FILTERPANE, advListFilterPane);

            if (BooleanUtils.toBoolean(advListFilterPane)) {
                JSONArray paneFields = new JSONArray();
                for (String field : DataListManager.instance.getListFilterPaneFields(user, entity)) {
                    if (AdvFilterParser.VF_ACU.equals(field)) {
//                        JSONObject vf = (JSONObject) EasyMetaFactory.valueOf(listEntity.getField(EntityHelper.ApprovalLastUser)).toJSON();
//                        vf.put("name", AdvFilterParser.VF_ACU);
//                        vf.put("label", Language.L("当前审批人"));
//                        paneFields.add(vf);
                        log.warn("{} is deprecated", AdvFilterParser.VF_ACU);
                    } else {
                        paneFields.add(EasyMetaFactory.valueOf(listEntity.getField(field)).toJSON());
                    }
                }

                if (!paneFields.isEmpty()) mv.getModel().put("paneFields", paneFields);
            }

            // v3.3 查询页签
            String advListFilterTabs = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_FILTERTABS);
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_FILTERTABS, advListFilterTabs);

            // v3.6 记录合并
            String enableRecordMerger = easyEntity.getExtraAttr(EasyEntityConfigProps.ENABLE_RECORD_MERGER);
            if (BooleanUtils.toBoolean(enableRecordMerger)) {
                if (Application.getPrivilegesManager().allow(user, ZeroEntry.AllowRecordMerge)) {
                    mv.getModel().put(EasyEntityConfigProps.ENABLE_RECORD_MERGER, true);
                }
            }

        } else if (listMode == 2) {
            listConfig = DataListManager.instance.getFieldsLayoutMode2(listEntity);
            // 明细
            if (listEntity.getMainEntity() != null) mv.getModel().put("DataListType", "DetailList");

        } else if (listMode == 3) {
            listConfig = DataListManager.instance.getFieldsLayoutMode3(listEntity);
            // 明细
            if (listEntity.getMainEntity() != null) mv.getModel().put("DataListType", "DetailList");

            // 侧栏
            String mode3ShowCategory = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE3_SHOWCATEGORY);
            String mode3ShowFilters = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE3_SHOWFILTERS);
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_MODE3_SHOWCATEGORY, StringUtils.isNotBlank(mode3ShowCategory));
            mv.getModel().put(EasyEntityConfigProps.ADVLIST_MODE3_SHOWFILTERS, mode3ShowFilters);
            mv.getModel().put("hideAside",
                    !(BooleanUtils.toBoolean(mode3ShowFilters) || StringUtils.isNotBlank(mode3ShowCategory)));
        }

        mv.getModel().put("DataListConfig", JSON.toJSONString(listConfig));

        // 快速查询
        mv.getModel().put("quickFieldsLabel", getQuickFieldsLabel(listEntity));

        // EasyAction
        mv.getModel().put("easyAction", EasyActionManager.instance.getEasyAction(listEntity.getName(), user));

        return mv;
    }

    @PostMapping("data-list")
    public RespBody dataList(@PathVariable String entity, HttpServletRequest request) {
        JSONObject query = (JSONObject) ServletUtils.getRequestJson(request);
        DataListBuilder builder = new DataListBuilderImpl(query, getRequestUser(request));

        try {
            return RespBody.ok(builder.getJSONResult());

        } catch (Exception ex) {
            String known = KnownExceptionConverter.convert2ErrorMsg(ex);
            if (known != null) return RespBody.error(known);

            log.error(null, ex);
            return RespBody.error(ex.getLocalizedMessage());
        }
    }

    /**
     * 快速查询字段
     *
     * @param entity
     * @return
     */
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
