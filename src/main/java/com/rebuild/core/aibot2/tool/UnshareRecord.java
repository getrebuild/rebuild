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
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.InternalPermission;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 取消共享工具，取消记录已共享给全部或指定用户的访问权限
 *
 * @author RB
 * @since 2026/10/10
 */
public class UnshareRecord implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        final ID user = UserContextHolder.getUser();
        List<ID> records = ToolHelper.resolveRecordIds(args.get("recordIds"), 100);

        Entity entity = MetadataHelper.getEntity(records.get(0).getEntityCode());
        for (ID id : records) {
            if (id.getEntityCode() != entity.getEntityCode()) {
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
            if (!Application.getPrivilegesManager().allowShare(user, id)) {
                throw new KnownToolException("你没有共享记录 [" + id + "] 的权限，无法执行");
            }
        }

        // 不传 toUsers 表示取消全部用户的共享
        List<ID> toUsers = ToolHelper.resolveUsers(args.get("toUsers"));
        List<String> toUserNames = new ArrayList<>();
        for (ID to : toUsers) {
            toUserNames.add(Application.getUserStore().getUser(to).getFullName());
        }

        // 查询现有共享（与 Web 端 record-unshare-batch 一致）
        String accessSql = String.format("select accessId,recordId,shareTo,rights from ShareAccess where recordId in ('%s')",
                StringUtils.join(records, "','"));
        Object[][] accessArray = Application.createQueryNoFilter(accessSql).array();

        Set<ID> toSet = new HashSet<>(toUsers);
        Map<ID, List<ID>> accessIdMap = new LinkedHashMap<>();        // 记录 -> 共享记录 ID
        Map<ID, List<String>> shareDescMap = new LinkedHashMap<>();   // 记录 -> 共享用户描述
        for (Object[] o : accessArray) {
            ID recordId = (ID) o[1];
            ID shareTo = (ID) o[2];
            int rights = ((Number) o[3]).intValue();
            if (!toSet.isEmpty() && !toSet.contains(shareTo)) continue;

            // 共享用户可能已被删除，名称取不到时回退为记录 ID，避免摘要出现 null
            String shareToName = StringUtils.defaultIfBlank(UserHelper.getName(shareTo), shareTo.toString());

            accessIdMap.computeIfAbsent(recordId, k -> new ArrayList<>()).add((ID) o[0]);
            shareDescMap.computeIfAbsent(recordId, k -> new ArrayList<>())
                    .add(shareToName + "(" + ((rights & BizzPermission.UPDATE.getMask()) != 0 ? "可编辑" : "读取") + ")");
        }

        // 无匹配共享时无需确认，直接返回
        if (accessIdMap.isEmpty()) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "unshared", "records", "message"},
                    new Object[]{"ok", 0, 0,
                            toUsers.isEmpty() ? "所选记录当前没有共享，无需取消" : "所选记录未共享给指定用户，无需取消"});
        }

        if (!args.getBooleanValue("confirmed")) {
            List<String> recordNames = ToolHelper.recordNames(entity, records, 10);
            if (records.size() > 10) recordNames.add("等共 " + records.size() + " 条");

            // 当前共享：逐条记录展示共享用户与权限
            List<String> currentShared = new ArrayList<>();
            List<String> sharedRecordNames = ToolHelper.recordNames(
                    entity, new ArrayList<>(accessIdMap.keySet()), 10);
            int index = 0;
            for (Map.Entry<ID, List<String>> e : shareDescMap.entrySet()) {
                if (index >= sharedRecordNames.size()) break;

                currentShared.add(sharedRecordNames.get(index) + " ← " + StringUtils.join(e.getValue(), "、"));
                index++;
            }

            JSONObject changes = new JSONObject(true);
            changes.put("操作", "取消共享");
            changes.put("实体", EasyMetaFactory.getLabel(entity));
            changes.put("记录", recordNames);
            changes.put("当前共享", currentShared);
            changes.put("本次取消", toUsers.isEmpty() ? "全部用户" : toUserNames);

            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。取消共享会移除指定用户对记录的访问权限，请先将变更摘要（当前共享用户与本次取消范围）完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行。用户未确认或要求调整时不得执行。"});
        }

        EntityService ies = Application.getEntityService(entity.getEntityCode());
        int removed = 0;
        for (Map.Entry<ID, List<ID>> e : accessIdMap.entrySet()) {
            BulkContext context = new BulkContext(user, InternalPermission.UNSHARE,
                    e.getValue().toArray(new ID[0]), e.getKey());
            removed += ies.bulk(context);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "unshared", "records", "message"},
                new Object[]{"ok", removed, accessIdMap.size(),
                        String.format("已取消 %d 项共享（涉及 %d 条记录）", removed, accessIdMap.size())});
    }
}
