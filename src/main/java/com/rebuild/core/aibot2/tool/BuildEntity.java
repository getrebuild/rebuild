/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.utils.AppUtils;
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
public class BuildEntity implements Tool, AdminGuard {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityLabel = args.getString("entityLabel");
        if (StringUtils.isBlank(entityLabel)) {
            throw new KnownToolException("实体名称 (entityLabel) 不能为空");
        }

        // 主实体（可选，指定后创建为明细实体）
        String mainEntity = args.getString("mainEntity");
        if (StringUtils.isNotBlank(mainEntity)) {
            Entity useMain = ToolHelper.resolveEntity(mainEntity);
            if (useMain == null) {
                throw new KnownToolException("无效主实体 : " + mainEntity + ToolHelper.suggestEntity(mainEntity));
            }
            if (useMain.getMainEntity() != null) {
                throw new KnownToolException("明细实体不能作为主实体");
            }
            mainEntity = useMain.getName();
        }

        // 名称字段默认创建，业务实体通常需要
        Boolean nameField = args.getBoolean("nameField");
        boolean haveNameField = nameField == null || nameField;
        boolean haveSeriesField = args.getBooleanValue("seriesField");

        // 实体是系统核心，未确认时仅返回改动清单，须用户二次确认后才创建
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = buildChanges(entityLabel, mainEntity, haveNameField, haveSeriesField, args.getString("comments"));
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。新建实体属于重大改动，请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        String entityName = new Entity2Schema(UserContextHolder.getUser()).createEntity(
                null, entityLabel, args.getString("comments"), mainEntity, haveNameField, haveSeriesField);

        String entityUrl = AppUtils.getContextPath("/admin/entity/" + entityName + "/base");
        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "label", "url", "message"},
                new Object[]{"ok", entityName, entityLabel, entityUrl,
                        String.format("已成功创建%s [%s](%s)，[点击前往实体管理](%s)配置表单和布局",
                                mainEntity != null ? "明细实体" : "实体", entityLabel, entityName, entityUrl)});
    }

    /**
     * 构建改动清单（供用户确认）
     *
     * @param entityLabel
     * @param mainEntity
     * @param haveNameField
     * @param haveSeriesField
     * @param comments
     * @return
     */
    private JSONObject buildChanges(String entityLabel, String mainEntity,
                                    boolean haveNameField, boolean haveSeriesField, String comments) {
        JSONObject changes = new JSONObject(true);
        changes.put("操作", mainEntity != null
                ? "新建明细实体（主实体：" + EasyMetaFactory.getLabel(MetadataHelper.getEntity(mainEntity)) + "）"
                : "新建实体");
        changes.put("实体名称", entityLabel);
        if (haveNameField) changes.put("同时创建", "名称字段" + (haveSeriesField ? "、自动编号字段" : ""));
        else if (haveSeriesField) changes.put("同时创建", "自动编号字段");
        if (StringUtils.isNotBlank(comments)) changes.put("描述", comments);

        // 提示同名实体，避免用户误操作
        for (Entity e : MetadataHelper.getEntities()) {
            if (entityLabel.equalsIgnoreCase(EasyMetaFactory.getLabel(e))) {
                changes.put("注意", "已存在同名实体「" + EasyMetaFactory.getLabel(e) + "」，创建后将自动区分命名");
                break;
            }
        }
        return changes;
    }
}
