/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据系统当前状态生成推荐问题，帮助用户快速开始对话。
 * 针对管理员与普通用户生成不同的推荐问题
 *
 * @author devezhao
 * @since 2026/8/4
 */
@Slf4j
public class SuggestQuestions {

    private static final int MAX_QUESTIONS = 4;
    private static final int MAX_ENTITIES_TO_CHECK = 8;

    private SuggestQuestions() {}

    /**
     * 生成推荐问题
     *
     * @param user 当前用户（管理员与普通用户展示不同的推荐问题）
     * @return
     */
    public static JSONArray generate(ID user) {
        boolean admin = user != null && UserHelper.isAdmin(user);

        JSONArray questions = new JSONArray();

        // 优先配置的
        String sqConfig = RebuildConfiguration.get(ConfigurationItem.AibotSuggestQuestions);
        if (StringUtils.isNotBlank(sqConfig)) {
            for (String line : sqConfig.split("\n")) {
                if (questions.size() >= MAX_QUESTIONS) break;
                String q = line.trim();
                if (!q.isEmpty()) questions.add(q);
            }
        }

        if (questions.size() < MAX_QUESTIONS) {
            // 通用引导，管理员与普通用户均展示
            addQuestion(questions, "你可以做什么");

            List<EntityData> withData = collectEntityData();

            if (admin) {
                // 管理员侧重系统配置与管理类问题
                addQuestion(questions, "帮我创建一个新的业务实体");
                if (!withData.isEmpty()) {
                    addQuestion(questions, String.format("给%s实体添加一个新字段", withData.get(0).label));
                } else {
                    addQuestion(questions, "给实体添加一个新字段");
                }
                addQuestion(questions, "如何配置审批流程");
                addQuestion(questions, "如何设置用户角色权限");
            } else {
                // 普通用户侧重日常业务操作类问题
                if (!withData.isEmpty()) {
                    EntityData top = withData.get(0);
                    addQuestion(questions, String.format("查询%s列表", top.label));

                    EntityData customer = findCustomerEntity(withData);
                    if (customer != null) {
                        addQuestion(questions, String.format("创建%s跟进", customer.label));
                    }

                    if (withData.size() > 1) {
                        addQuestion(questions, String.format("统计分析%s数据", withData.get(1).label));
                    }
                }
                addQuestion(questions, "查询待审批记录");
            }
        }

        return questions;
    }

    /**
     * @param questions
     * @param question
     */
    private static void addQuestion(JSONArray questions, String question) {
        if (questions.size() < MAX_QUESTIONS && !questions.contains(question)) {
            questions.add(question);
        }
    }

    /**
     * 收集有数据的业务实体（按元数据顺序）
     *
     * @return
     */
    private static List<EntityData> collectEntityData() {
        List<EntityData> list = new ArrayList<>();
        int checked = 0;

        for (Entity e : MetadataHelper.getEntities()) {
            if (!MetadataHelper.isBusinessEntity(e)) continue;
            if (e.getMainEntity() != null) continue;
            if (checked >= MAX_ENTITIES_TO_CHECK) break;
            checked++;

            if (!hasRecords(e)) continue;
            String label = EasyMetaFactory.getLabel(e);
            list.add(new EntityData(e.getName(), label));
        }

        return list;
    }

    /**
     * 查找客户类实体（标签含客户/供应商/联系等关键词）
     *
     * @param list
     * @return
     */
    private static EntityData findCustomerEntity(List<EntityData> list) {
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

    /**
     * 轻量存在性检查（LIMIT 1），比 COUNT 快得多
     *
     * @param entity
     * @return
     */
    private static boolean hasRecords(Entity entity) {
        try {
            String sql = String.format("select %s from %s",
                    entity.getPrimaryField().getName(), entity.getName());
            return Application.createQuery(sql).setLimit(1).array().length > 0;
        } catch (Exception ex) {
            log.warn("Failed to check records for entity: {}", entity.getName(), ex);
        }
        return false;
    }

    private static class EntityData {
        final String name;
        final String label;

        EntityData(String name, String label) {
            this.name = name;
            this.label = label;
        }
    }
}
