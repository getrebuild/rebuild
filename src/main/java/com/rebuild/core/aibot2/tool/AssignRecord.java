/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 分配记录工具，将记录的所属用户变更为指定用户（支持单条或批量）
 *
 * @author RB
 * @since 2026/10/10
 */
public class AssignRecord implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        final ID user = UserContextHolder.getUser();
        List<ID> records = ToolHelper.resolveRecordIds(args.get("recordIds"), 100);

        Entity entity = MetadataHelper.getEntity(records.get(0).getEntityCode());
        for (ID id : records) {
            if (!ToolHelper.isSameEntity(id, entity)) {
                throw new KnownToolException("只能处理同一实体的记录，记录 " + id + " 属于其他实体");
            }
        }
        if (!MetadataHelper.isBusinessEntity(entity)) {
            throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 不支持此操作");
        }
        if (entity.getMainEntity() != null) {
            throw new KnownToolException("明细记录不支持此操作，请指定其主记录");
        }
        for (ID id : records) {
            if (!QueryHelper.exists(id)) {
                throw new KnownToolException("记录不存在或已被删除 : " + id);
            }
        }
        // 权限预检先于确认摘要执行，避免让用户确认一个必然失败的操作
        for (ID id : records) {
            if (!Application.getPrivilegesManager().allowAssign(user, id)) {
                throw new KnownToolException("你没有分配记录 [" + id + "] 的权限，无法执行");
            }
        }

        String toUserIdent = args.getString("toUser");
        if (StringUtils.isBlank(toUserIdent)) {
            throw new KnownToolException("目标用户 (toUser) 不能为空，请填写用户全名或用户 ID");
        }
        ID toUser = ToolHelper.resolveUser(toUserIdent);
        if (toUser == null) {
            throw new KnownToolException("未找到用户 : " + toUserIdent + "，请填写用户全名或用户 ID");
        }
        User toUserObj = Application.getUserStore().getUser(toUser);
        if (!toUserObj.isActive()) {
            throw new KnownToolException("用户 [" + toUserObj.getFullName() + "] 已停用，无法分配");
        }
        String[] cascades = ToolHelper.resolveCascades(args.getJSONArray("cascades"));

        if (!args.getBooleanValue("confirmed")) {
            List<String> recordNames = ToolHelper.recordNames(entity, records, 10);
            if (records.size() > 10) recordNames.add("等共 " + records.size() + " 条");

            JSONObject changes = new JSONObject(true);
            changes.put("操作", "分配记录");
            changes.put("实体", EasyMetaFactory.getLabel(entity));
            changes.put("记录", recordNames);
            changes.put("分配给", toUserObj.getFullName());
            if (cascades.length > 0) {
                changes.put("同时分配相关记录", Arrays.asList(cascades));
            }

            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。分配会变更记录的所属用户并通知相关用户，请先将变更摘要（记录与目标用户）完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行。用户未确认或要求调整时不得执行。"});
        }

        EntityService ies = Application.getEntityService(entity.getEntityCode());
        int affected;
        if (records.size() == 1) {
            affected = ies.assign(records.get(0), toUser, cascades);
        } else {
            BulkContext context = new BulkContext(user, BizzPermission.ASSIGN, toUser, cascades, records.toArray(new ID[0]));
            affected = ies.bulk(context);
        }

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("assigned", affected);
        result.put("requests", records.size());
        result.put("message", String.format("已分配 %d/%d 条%s给 %s",
                affected, records.size(), EasyMetaFactory.getLabel(entity), toUserObj.getFullName()));
        if (records.size() == 1) {
            result.put("url", AppUtils.getContextPath("/app/redirect?id=" + records.get(0) + "&type=newtab"));
        }
        return result;
    }
}
