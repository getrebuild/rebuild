/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.AgentQuota;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.RbvMissingController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rex AI 管理页面
 *
 * @author devezhao
 * @since 2026/11/24
 */
@RestController
@RequestMapping("/admin/aibot2")
public class AiBot2PageController extends BaseController {

    @GetMapping("overview")
    public ModelAndView pageOverview() {
        return createModelAndView("/admin/aibot2/overview");
    }

    @GetMapping("chats")
    public ModelAndView pageChats() {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("会话列表"));
        }
        return createModelAndView("/admin/aibot2/chats");
    }

    @GetMapping("kb")
    public ModelAndView pageKb() {
        return createModelAndView("/admin/aibot2/kb");
    }

    @GetMapping("skills")
    public ModelAndView pageSkills() {
        return createModelAndView("/admin/aibot2/skills");
    }

    @GetMapping("tools")
    public ModelAndView pageTools() {
        return createModelAndView("/admin/aibot2/tools");
    }

    @GetMapping("models")
    public ModelAndView pageModels() {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("模型"));
        }
        return createModelAndView("/admin/aibot2/models");
    }

    // - Agent

    @GetMapping("agents")
    public ModelAndView pageAgents() {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("智能体"));
        }
        return createModelAndView("/admin/aibot2/agents");
    }

    @GetMapping("agent/{agentId}")
    public ModelAndView pageAgent(@PathVariable String agentId, HttpServletResponse response) throws IOException {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("智能体"));
        }

        ID agentId2 = ID.isId(agentId) ? ID.valueOf(agentId) : null;
        if (agentId2 == null) {
            response.sendError(404);
            return null;
        }

        Object[] agent = Application.createQueryNoFilter(
                        "select name,config,isDisabled,shareTo from AibotConfig where configId = ? and type = 'AGENT'")
                .setParameter(1, agentId2)
                .unique();
        if (agent == null) {
            response.sendError(404);
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/aibot2/agent-editor");
        mv.getModelMap().put("agentDisplayName",
                StringUtils.defaultIfBlank((String) agent[0], RebuildConfiguration.get(ConfigurationItem.AibotName)));
        mv.getModelMap().put("agentId", agentId2);
        mv.getModelMap().put("pubUrl", RebuildConfiguration.getHomeUrl("/aibot/pub/" + agentId2));
        mv.getModelMap().put("agentName", StringUtils.trimToEmpty((String) agent[0]));
        JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) agent[1]);
        mv.getModelMap().put("agentConfig", conf == null ? null : conf.toJSONString().replace("<", "\\u003C"));
        mv.getModelMap().put("agentShareTo", StringUtils.defaultIfBlank((String) agent[3], ShareToManager.SHARE_ALL));

        long[] usage = AgentQuota.usage(agentId2);
        mv.getModelMap().put("usageToday", ObjectUtils.round(usage[0] / 10000d, 2));
        mv.getModelMap().put("usageMonth", ObjectUtils.round(usage[1] / 10000d, 2));
        return mv;
    }

    // - MinApp

    @GetMapping("minapps")
    public ModelAndView pageMinApps() {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("微应用"));
        }
        return createModelAndView("/admin/aibot2/minapps");
    }

    @GetMapping("minapp/{minappId}")
    public ModelAndView pageMinApp(@PathVariable String minappId, HttpServletResponse response) throws IOException {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("微应用"));
        }

        ID minappId2 = ID.isId(minappId) ? ID.valueOf(minappId) : null;
        if (minappId2 == null) {
            response.sendError(404);
            return null;
        }

        Object[] minapp = Application.createQueryNoFilter(
                        "select name,config,isDisabled,shareTo from AibotConfig where configId = ? and type = 'MINAPP'")
                .setParameter(1, minappId2)
                .unique();
        if (minapp == null) {
            response.sendError(404);
            return null;
        }

        JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) minapp[1]);
        if (conf == null) conf = new JSONObject();
        conf.remove("html");

        ModelAndView mv = createModelAndView("/admin/aibot2/minapp-editor");
        mv.getModelMap().put("minappDisplayName", StringUtils.defaultString((String) minapp[0]));
        mv.getModelMap().put("minappId", minappId2);
        mv.getModelMap().put("minappName", StringUtils.trimToEmpty((String) minapp[0]));
        mv.getModelMap().put("minappConfig", conf.toJSONString());
        mv.getModelMap().put("minappShareTo", StringUtils.defaultIfBlank((String) minapp[3], ShareToManager.SHARE_ALL));
        mv.getModelMap().put("pubUrl", RebuildConfiguration.getHomeUrl("pages/minapp/" + minappId2));
        return mv;
    }
}
