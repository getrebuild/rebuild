/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.core.service.project.ProjectTaskTagService;
import com.rebuild.core.service.project.TaskTagManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2020/6/29
 */
@RestController
@RequestMapping("/project/tags/")
public class TaskTagController extends BaseController {

    @GetMapping("list")
    public JSON listTags(@IdParam(name = "project") ID projectId, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSONArray array = TaskTagManager.instance.getTagList(projectId);

        for (Object o : array) {
            JSONObject item = (JSONObject) o;
            ID tagId = ID.valueOf(item.getString("id"));
            item.put("isManageable", ProjectHelper.isManageable(tagId, user));
        }
        return array;
    }

    @PostMapping("create")
    public JSON createOrGetTag(@IdParam(name = "task", required = false) ID taskId, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
        String tagName = data.getString("tagName");
        ID projectId = ID.valueOf(data.getString("projectId"));

        Record record;

        // 是否存在
        ID existsTag = TaskTagManager.instance.findTagByName(tagName, projectId);

        if (existsTag != null) {
            record = EntityHelper.forUpdate(existsTag, user);
            record.setString("color", data.getString("color"));
        } else {
            record = EntityHelper.parse(data, user);
        }

        record = Application.getBean(ProjectTaskTagService.class).createOrUpdate(record);

        if (taskId != null) {
            TaskTagManager.instance.createRelated(taskId, record.getPrimary());
        }
        return JSONUtils.toJSONObject("tag", record.getPrimary());
    }

    @RequestMapping("related-add")
    public JSON createRelated(@IdParam(name = "task") ID taskId,
                              @IdParam(name = "tag") ID tagId) {
        ID relatedId = TaskTagManager.instance.createRelated(taskId, tagId);
        // `relatedId` 可能为空
        return JSONUtils.toJSONObject("rid", relatedId);
    }

    @RequestMapping("related-del")
    public RespBody deleteRelated(@IdParam(name = "rid") ID relatedId) {
        Application.getCommonsService().delete(relatedId);
        return RespBody.ok();
    }

    @GetMapping("task-tags")
    public JSON getTagsById(@IdParam(name = "task") ID taskId) {
        return getTaskTags(taskId);
    }

    /**
     * 获取任务标签
     *
     * @param taskId
     * @return
     */
    static JSONArray getTaskTags(ID taskId) {
        Object[][] tags = Application.createQueryNoFilter(
                "select tagId.tagName,tagId.color,relationId,tagId from ProjectTaskTagRelation where taskId = ? order by createdOn")
                .setParameter(1, taskId)
                .array();
        return JSONUtils.toJSONObjectArray(new String[] { "name", "color" , "rid", "id" }, tags);
    }
}
