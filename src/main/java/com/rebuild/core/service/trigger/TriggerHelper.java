/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2026/05/24
 */
public class TriggerHelper {

    /**
     * 解析目标实体
     *
     * @param config
     * @param sourceEntity
     * @return
     */
    public static String[] tryParseTargetEntity(String config, String sourceEntity) {
        if (!JSONUtils.wellFormat(config)) return null;

        JSONObject configJson = JSON.parseObject(config);

        String targetEntity = configJson.getString("targetEntity");
        if (StringUtils.isNotBlank(targetEntity)) {
            if (targetEntity.startsWith(TriggerAction.SOURCE_SELF)) targetEntity = sourceEntity;
            else if (targetEntity.contains(".")) targetEntity = targetEntity.split("\\.")[1];

            if (MetadataHelper.containsEntity(targetEntity)) {
                return new String[]{targetEntity, EasyMetaFactory.getLabel(targetEntity)};
            } else {
                return new String[]{null, String.format("[%s]", targetEntity.toUpperCase())};
            }
        }

        // 自动记录转换
        String useTransform = configJson.getString("useTransform");
        if (ID.isId(useTransform)) {
            try {
                ConfigBean cb = TransformManager.instance.getTransformConfig(ID.valueOf(useTransform), sourceEntity);
                return new String[]{useTransform, cb.getString("name")};
            } catch (Exception deleted) {
                return new String[]{null, String.format("[%s]", useTransform.toUpperCase())};
            }
        }

        // 自动审批
        String useApproval = configJson.getString("useApproval");
        if (ID.isId(useApproval)) {
            try {
                ConfigBean cb = RobotApprovalManager.instance.getFlowDefinition(ID.valueOf(useApproval));
                return new String[]{useApproval, cb.getString("name")};
            } catch (Exception deleted) {
                return new String[]{null, String.format("[%s]", useApproval.toUpperCase())};
            }
        }

        // 导出报表
        String useTemplate = configJson.getString("useTemplate");
        if (ID.isId(useTemplate)) {
            try {
                ConfigBean cb = DataReportManager.instance.getReportRaw(ID.valueOf(useTemplate));
                return new String[]{useTemplate, cb.getString("name")};
            } catch (Exception deleted) {
                return new String[]{null, String.format("[%s]", useTemplate.toUpperCase())};
            }
        }

        return null;
    }
}
