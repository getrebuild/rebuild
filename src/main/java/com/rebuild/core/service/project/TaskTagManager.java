/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.RecordBuilder;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 任务标签管理
 *
 * @author devezhao
 * @since 2020/11/21
 */
@Slf4j
public class TaskTagManager implements ConfigManager {

    public static final TaskTagManager instance = new TaskTagManager();

    private TaskTagManager() {
    }

    /**
     * @param projectId
     * @return
     */
    public JSONArray getTagList(ID projectId) {
        final String cKey = "TASKTAG-" + projectId;

        Serializable value = Application.getCommonsCache().getx(cKey);
        if (value == null) {
            Object[][] array = Application.createQueryNoFilter(
                    "select tagId,tagName,color,createdBy from ProjectTaskTag where projectId = ? order by tagName")
                    .setParameter(1, projectId)
                    .array();

            value = JSONUtils.toJSONObjectArray(
                    new String[] { "id", "name", "color", "createdBy" }, array);
            Application.getCommonsCache().putx(cKey, value);
        }

        return (JSONArray) JSONUtils.clone((JSON) value);
    }

    /**
     * @param tagName
     * @param projectId
     * @return
     */
    public ID findTagByName(String tagName, ID projectId) {
        Object[] exists = Application.createQueryNoFilter(
                "select tagId from ProjectTaskTag where projectId = ? and tagName = ?")
                .setParameter(1, projectId)
                .setParameter(2, tagName)
                .unique();
        return exists == null ? null : (ID) exists[0];
    }

    /**
     * @param taskId
     * @param tagId
     * @return
     */
    public ID createRelated(ID taskId, ID tagId) {
        Object[] exists = Application.createQueryNoFilter(
                "select relationId from ProjectTaskTagRelation where taskId = ? and tagId = ?")
                .setParameter(1, taskId)
                .setParameter(2, tagId)
                .unique();
        if (exists != null) {
            log.debug("ProjectTaskTagRelation exists : {} <> {}", taskId, tagId);
            return null;
        }

        RecordBuilder builder = RecordBuilder
                .builder(EntityHelper.ProjectTaskTagRelation)
                .add("taskId", taskId)
                .add("tagId", tagId);

        return Application.getCommonsService()
                .create(builder.build(UserContextHolder.getUser()))
                .getPrimary();
    }

    @Override
    public void clean(Object projectId) {
        final String cKey = "TASKTAG-" + projectId;
        Application.getCommonsCache().evict(cKey);
    }
}
