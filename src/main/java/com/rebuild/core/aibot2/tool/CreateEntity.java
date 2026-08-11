/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 新建实体（仅管理员）
 *
 * @author devezhao
 * @since 2026/8/10
 */
@Slf4j
public class CreateEntity implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        final ID user = UserContextHolder.getUser();
        if (!UserHelper.isAdmin(user)) {
            throw new ToolException("仅管理员可新建实体");
        }

        String entityLabel = args.getString("entityLabel");
        if (StringUtils.isBlank(entityLabel)) {
            throw new ToolException("实体名称 (entityLabel) 不能为空");
        }

        // 主实体（可选，指定后创建为明细实体）
        String mainEntity = args.getString("mainEntity");
        if (StringUtils.isNotBlank(mainEntity)) {
            Entity useMain = ToolHelper.resolveEntity(mainEntity);
            if (useMain == null) {
                throw new ToolException("无效主实体 : " + mainEntity + ToolHelper.suggestEntity(mainEntity));
            }
            if (useMain.getMainEntity() != null) {
                throw new ToolException("明细实体不能作为主实体");
            }
            mainEntity = useMain.getName();
        }

        // 名称字段默认创建，业务实体通常需要
        Boolean nameField = args.getBoolean("nameField");
        boolean haveNameField = nameField == null || nameField;
        boolean haveSeriesField = args.getBooleanValue("seriesField");

        String entityName = new Entity2Schema(user).createEntity(
                null, entityLabel, args.getString("comments"), mainEntity, haveNameField, haveSeriesField);

        log.info("Entity created via AI : {} ({})", entityName, entityLabel);

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "label", "message"},
                new Object[]{"ok", entityName, entityLabel,
                        String.format("已成功创建%s [%s](%s)，可前往管理中心-实体管理配置表单和布局",
                                mainEntity != null ? "明细实体" : "实体", entityLabel, entityName)});
    }
}
