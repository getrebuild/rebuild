/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.aibot2.knowledge.KnowledgeBuilder;
import com.rebuild.core.aibot2.tool.ToolDefs;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.DataDesensitized;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.RebuildWebConfigurer;
import com.rebuild.web.admin.ConfigurationController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * @author devezhao
 * @since 2026/8/5
 */
@Slf4j
@RestController
@RequestMapping("/admin/integration")
public class AiBot2AdminController extends BaseController {

    @PostMapping("aibot")
    public RespBody postIntegrationAibot(@RequestBody JSONObject data) {
        if (data.getBooleanValue("__clear__")) {
            ConfigurationController.clearConfigurationByPrefix("Aibot");
            return RespBody.ok();
        }

        ConfigurationController.setValues(data);
        Application.getBean(RebuildWebConfigurer.class).init();
        Config.getClient(true);
        return RespBody.ok();
    }

    @GetMapping("aibot")
    public ModelAndView pageIntegrationAibot() {
        ModelAndView mv = createModelAndView("/admin/integration/aibot");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String name = item.name();
            if (name.startsWith("Aibot")) {
                String value = RebuildConfiguration.get(item);
                if (value != null && item == ConfigurationItem.AibotDSSecret) {
                    value = DataDesensitized.any(value);
                }
                mv.getModel().put(name, value);
            }
        }
        mv.getModel().put("HomeUrl", RebuildConfiguration.getHomeUrl());
        return mv;
    }

    @GetMapping("aibot/stats")
    public JSON statsAibot() {
        final Date xday = CalendarUtils.clearTime(CalendarUtils.addDay(-90));
        final String sql = "select date_format(createdOn,'%Y-%m-%d'),sum(token) from AibotChat" +
                " where createdOn > ? group by date_format(createdOn,'%Y-%m-%d')";

        Object[][] aibot = Application.createQueryNoFilter(sql)
                .setParameter(1, xday)
                .array();
        Arrays.sort(aibot, Comparator.comparing(o -> o[0].toString()));

        double aibotCount = 0;
        for (Object[] o : aibot) {
            o[1] = o[1] == null ? 0L : o[1];
            o[1] = ObjectUtils.round((Long) o[1] / 10000d, 2);
            aibotCount += (Double) o[1];
        }

        return JSONUtils.toJSONObject(
                new String[]{"aibot", "aibotCount"},
                new Object[]{aibot, ObjectUtils.round(aibotCount, 2)});
    }

    @GetMapping("aibot/tools")
    public RespBody toolList() {
        return RespBody.ok(ToolDefs.listTools(true, false));
    }

    @GetMapping("aibot/kb-list")
    public RespBody kbList() {
        Object[][] array = Application.createQueryNoFilter(
                "select knowledgeId, name, description, sourceType, sourceConfig, chunkCount, isDisabled, modifiedOn, createdBy from AibotKnowledge order by modifiedOn desc")
                .array();

        return RespBody.ok(JSONUtils.toJSONObjectArray(
                new String[]{"id", "name", "description", "sourceType", "sourceConfig", "chunkCount", "isDisabled", "modifiedOn", "createdBy"},
                array));
    }

    @PostMapping("aibot/kb-build")
    public RespBody build(HttpServletRequest req) {
        ID knowledgeId = getIdParameterNotNull(req, "id");
        Object[] knowledge = Application.createQueryNoFilter(
                "select name, sourceType, sourceConfig from AibotKnowledge where knowledgeId = ?")
                .setParameter(1, knowledgeId)
                .unique();

        if (knowledge == null) {
            return RespBody.error("知识库不存在");
        }

        String name = (String) knowledge[0];
        String sourceType = (String) knowledge[1];
        String sourceConfig = (String) knowledge[2];

        // 后台异步构建（-1 构建中，0 构建失败）
        KnowledgeBuilder.updateChunkCount(knowledgeId, -1);
        TaskExecutors.queue(() -> {
            try {
                KnowledgeBuilder.build(knowledgeId, sourceType, sourceConfig, name);
            } catch (Exception e) {
                KnowledgeBuilder.updateChunkCount(knowledgeId, 0);
                log.error("Failed to build knowledge: {}", knowledgeId, e);
            }
        });
        return RespBody.ok();
    }
}
