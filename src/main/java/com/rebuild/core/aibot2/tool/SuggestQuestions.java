/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 根据系统当前状态生成推荐问题，帮助用户快速开始对话
 *
 * @author devezhao
 * @since 2026/8/4
 */
@Slf4j
public class SuggestQuestions implements Tool {

    private static final int MAX_QUESTIONS = 4;
    private static final int MAX_ENTITIES_TO_CHECK = 8;

    @Override
    public Object tool(String arguments) throws Exception {
        JSONArray questions = new JSONArray();

        List<EntityData> entityDataList = collectEntityData();
        entityDataList.sort(Comparator.comparingLong(EntityData::getCount).reversed());

        // 取有数据的实体
        List<EntityData> withData = new ArrayList<>();
        for (EntityData ed : entityDataList) {
            if (ed.count > 0) withData.add(ed);
        }

        if (!withData.isEmpty()) {
            EntityData top = withData.get(0);
            // 查询（QueryRecords）
            questions.add(String.format("查询%s列表", top.label));

            // 创建跟进（CreateFeed），客户类实体优先
            if (questions.size() < MAX_QUESTIONS - 1) {
                EntityData customer = findCustomerEntity(withData);
                if (customer != null) {
                    questions.add(String.format("创建%s跟进", customer.label));
                }
            }

            // 统计分析（DataStatistics），用另一个实体避免重复
            if (questions.size() < MAX_QUESTIONS - 1 && withData.size() > 1) {
                questions.add(String.format("统计分析%s", withData.get(1).label));
            }

            // 审批（Approval）
            if (questions.size() < MAX_QUESTIONS - 1) {
                questions.add("查询待审批记录");
            }
        }

        if (questions.size() < MAX_QUESTIONS) {
            // 兜底：读取管理员配置（每行一个问题）
            String sqConfig = RebuildConfiguration.get(ConfigurationItem.AibotSuggestQuestions);
            if (StringUtils.isNotBlank(sqConfig)) {
                for (String line : sqConfig.split("\n")) {
                    if (questions.size() >= MAX_QUESTIONS) break;
                    String q = line.trim();
                    if (!q.isEmpty()) questions.add(q);
                }
            }
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "questions"},
                new Object[]{"ok", questions});
    }

    /**
     * 收集业务实体及其数据量
     *
     * @return
     */
    private List<EntityData> collectEntityData() {
        List<EntityData> list = new ArrayList<>();
        int checked = 0;

        for (Entity e : MetadataHelper.getEntities()) {
            if (!MetadataHelper.isBusinessEntity(e)) continue;
            if (e.getMainEntity() != null) continue;
            if (checked >= MAX_ENTITIES_TO_CHECK) break;
            checked++;

            String label = EasyMetaFactory.getLabel(e);
            long count = countRecords(e);
            list.add(new EntityData(e.getName(), label, count));
        }

        return list;
    }

    /**
     * 查找客户类实体（标签含客户/供应商/联系等关键词）
     *
     * @param list
     * @return
     */
    private EntityData findCustomerEntity(List<EntityData> list) {
        for (EntityData ed : list) {
            String label = ed.label.toLowerCase();
            if (label.contains("客户") || label.contains("customer")
                    || label.contains("供应商") || label.contains("supplier")
                    || label.contains("联系人") || label.contains("contact")) {
                return ed;
            }
        }
        return null;
    }

    private long countRecords(Entity entity) {
        try {
            String sql = String.format("select count(%s) from %s",
                    entity.getPrimaryField().getName(), entity.getName());
            Object[] result = Application.createQuery(sql).unique();
            if (result != null && result.length > 0 && result[0] instanceof Long) {
                return (Long) result[0];
            }
        } catch (Exception ex) {
            log.warn("Failed to count records for entity: {}", entity.getName(), ex);
        }
        return 0;
    }

    private static class EntityData {
        final String name;
        final String label;
        final long count;

        EntityData(String name, String label, long count) {
            this.name = name;
            this.label = label;
            this.count = count;
        }

        long getCount() {
            return count;
        }
    }
}
