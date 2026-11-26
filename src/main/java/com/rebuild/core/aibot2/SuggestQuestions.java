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

    private static final int MAX_QUESTIONS = 7;
    private static final int MAX_GUIDE_QUESTIONS = 4;
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

            List<EntityData> withData = collectEntityData(user);

            if (admin) {
                addQuestion(questions, "帮我搭建一个业务系统");
                addQuestion(questions, "帮我创建一个新的业务实体");
                if (!withData.isEmpty()) {
                    addQuestion(questions, String.format("给%s实体添加一个新字段", withData.get(0).label));
                    addQuestion(questions, String.format("给%s实体创建一个触发器", withData.get(0).label));
                } else {
                    addQuestion(questions, "给实体添加一个新字段");
                    addQuestion(questions, "给实体创建一个触发器");
                }
                addQuestion(questions, "帮我制作一个微应用");
                addQuestion(questions, "记住我的工作偏好");
            } else {
                // 普通用户侧重日常业务操作类问题
                if (!withData.isEmpty()) {
                    EntityData top = withData.get(0);
                    addQuestion(questions, String.format("查询%s列表", top.label));
                    EntityData stats = withData.size() > 1 ? withData.get(1) : top;
                    addQuestion(questions, String.format("导出%s报表", stats.label));
                }
                addQuestion(questions, "帮我设置一个定时任务");
                addQuestion(questions, "记住我的工作偏好");
            }
        }

        return questions;
    }

    /**
     * 引导弹窗用，按用户权限生成少量可立即尝试的问题
     *
     * @param user 当前用户
     * @return
     */
    public static JSONArray generateGuide(ID user) {
        boolean admin = user != null && UserHelper.isAdmin(user);

        JSONArray questions = new JSONArray();

        if (admin) {
            addQuestion(questions, "系统里都有哪些业务实体", MAX_GUIDE_QUESTIONS);
            addQuestion(questions, "帮我创建一个新的业务实体", MAX_GUIDE_QUESTIONS);

            List<EntityData> withData = collectEntityData(user);
            addQuestion(questions, withData.isEmpty() ? "帮我搭建一个业务系统"
                    : String.format("给%s实体创建一个触发器", withData.get(0).label), MAX_GUIDE_QUESTIONS);
            addQuestion(questions, "帮我制作一个微应用", MAX_GUIDE_QUESTIONS);
        } else {
            List<EntityData> withData = collectEntityData(user);
            EntityData queryEntity = withData.isEmpty() ? null : withData.get(0);
            addQuestion(questions, queryEntity != null
                    ? String.format("查询%s列表", queryEntity.label) : "你可以做什么", MAX_GUIDE_QUESTIONS);

            EntityData creatable = null;
            for (EntityData e : withData) {
                if (e.creatable) {
                    creatable = e;
                    break;
                }
            }
            if (creatable == null) creatable = findCreatableEntity(user);
            if (creatable != null) {
                addQuestion(questions, String.format("帮我新建一条%s记录", creatable.label), MAX_GUIDE_QUESTIONS);
            } else if (queryEntity != null) {
                addQuestion(questions, String.format("导出%s报表", queryEntity.label), MAX_GUIDE_QUESTIONS);
            }

            addQuestion(questions, "帮我设置一个定时任务", MAX_GUIDE_QUESTIONS);
            addQuestion(questions, "记住我的工作偏好", MAX_GUIDE_QUESTIONS);
        }

        return questions;
    }

    /**
     * @param questions
     * @param question
     */
    private static void addQuestion(JSONArray questions, String question) {
        addQuestion(questions, question, MAX_QUESTIONS);
    }

    /**
     * @param questions
     * @param question
     * @param max
     */
    private static void addQuestion(JSONArray questions, String question, int max) {
        if (questions.size() < max && !questions.contains(question)) {
            questions.add(question);
        }
    }

    /**
     * 收集有数据的业务实体（按元数据顺序，仅限当前用户可读的）
     *
     * @param user
     * @return
     */
    private static List<EntityData> collectEntityData(ID user) {
        List<EntityData> list = new ArrayList<>();
        int checked = 0;

        for (Entity e : MetadataHelper.getEntities()) {
            if (!MetadataHelper.isBusinessEntity(e)) continue;
            if (e.getMainEntity() != null) continue;
            if (checked >= MAX_ENTITIES_TO_CHECK) break;
            checked++;

            if (user != null && !Application.getPrivilegesManager().allowRead(user, e.getEntityCode())) continue;
            if (!hasRecords(e)) continue;

            String label = EasyMetaFactory.getLabel(e);
            boolean creatable = user != null && Application.getPrivilegesManager().allowCreate(user, e.getEntityCode());
            list.add(new EntityData(label, creatable));
        }

        return list;
    }

    /**
     * 查找当前用户可新建的主业务实体（不要求已有数据，用于新建记录引导）
     *
     * @param user
     * @return
     */
    private static EntityData findCreatableEntity(ID user) {
        if (user == null) return null;

        for (Entity e : MetadataHelper.getEntities()) {
            if (!MetadataHelper.isBusinessEntity(e)) continue;
            if (e.getMainEntity() != null) continue;
            if (Application.getPrivilegesManager().allowCreate(user, e.getEntityCode())) {
                return new EntityData(EasyMetaFactory.getLabel(e), true);
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
        final String label;
        final boolean creatable;

        EntityData(String label, boolean creatable) {
            this.label = label;
            this.creatable = creatable;
        }
    }
}
