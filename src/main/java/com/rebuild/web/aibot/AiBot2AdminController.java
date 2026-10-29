/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openai.client.OpenAIClient;
import com.openai.models.models.Model;
import com.openai.models.models.ModelListPage;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.AgentQuota;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.aibot2.SkillDefs;
import com.rebuild.core.aibot2.knowledge.KnowledgeBuilder;
import com.rebuild.core.aibot2.service.AibotConfigService;
import com.rebuild.core.aibot2.tool.ToolDefs;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.rbstore.RBStore;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.DataDesensitized;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.RebuildWebConfigurer;
import com.rebuild.web.admin.ConfigurationController;
import com.rebuild.web.commons.RbvMissingController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author devezhao
 * @since 2026/8/5
 */
@Slf4j
@RestController
@RequestMapping("/admin/integration")
public class AiBot2AdminController extends BaseController {

    private static final String CHAT_VISIBLE_WHERE = String.format(
            " where createdBy <> '%s' and (createdBy <> '%s' or source = '%s')",
            UserService.SYSTEM_USER, UserService.AIBOT_USER, ChatManager.SOURCE_PUB);

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
        return mv;
    }

    @GetMapping("aibot/stats")
    public JSON statsAibot(HttpServletRequest request) {
        final ID agent = getIdParameter(request, "agent");
        final Date xday = CalendarUtils.clearTime(CalendarUtils.addDay(-90));

        Query query = Application.createQueryNoFilter(
                "select createdOn, createdBy, token, source from AibotChat where createdOn > ?"
                        + (agent == null ? "" : " and agentId = ?"))
                .setParameter(1, xday);
        if (agent != null) query.setParameter(2, agent);
        Object[][] rows = query.array();

        // 按日（词元/会话数/使用人数）与按用户聚合
        Map<String, Long> tokensByDay = new TreeMap<>();
        Map<String, Integer> chatsByDay = new TreeMap<>();
        Map<String, Set<ID>> usersByDay = new TreeMap<>();
        Map<ID, Long> tokenMap = new HashMap<>();
        Set<ID> realUsers = new HashSet<>();
        long tokenTotal = 0;
        int realChats = 0;

        for (Object[] o : rows) {
            String day = CalendarUtils.format("yyyy-MM-dd", (Date) o[0]);
            ID user = (ID) o[1];
            long token = o[2] == null ? 0L : ((Number) o[2]).longValue();
            boolean visitor = isVisitorChat(user, (String) o[3]);

            // 词元全量统计（含内部调用会话），会话数统计真实用户与访客（与 chat-list 口径一致），使用人数不含访客
            tokensByDay.merge(day, token, Long::sum);
            tokenMap.merge(user, token, Long::sum);
            tokenTotal += token;

            if (!visitor && UserHelper.isSystemUser(user)) continue;
            realChats++;
            chatsByDay.merge(day, 1, Integer::sum);

            if (visitor) continue;
            realUsers.add(user);
            usersByDay.computeIfAbsent(day, k -> new HashSet<>()).add(user);
        }

        List<Object[]> aibot = new ArrayList<>();
        for (Map.Entry<String, Long> e : tokensByDay.entrySet()) {
            aibot.add(new Object[]{e.getKey(), ObjectUtils.round(e.getValue() / 10000d, 1)});
        }
        List<Object[]> aibotDailyChats = new ArrayList<>();
        for (Map.Entry<String, Integer> e : chatsByDay.entrySet()) {
            aibotDailyChats.add(new Object[]{e.getKey(), e.getValue()});
        }
        List<Object[]> aibotDailyUsers = new ArrayList<>();
        for (Map.Entry<String, Set<ID>> e : usersByDay.entrySet()) {
            aibotDailyUsers.add(new Object[]{e.getKey(), e.getValue().size()});
        }

        List<Object[]> usersList = new ArrayList<>();
        for (User u : Application.getUserStore().getAllUsers()) {
            if (!u.isActive()) continue;
            if (UserHelper.isSystemUser(u.getId())) continue;

            long tokenSum = tokenMap.getOrDefault(u.getId(), 0L);
            usersList.add(new Object[]{u.getId().toString(), u.getFullName(),
                    ObjectUtils.round(tokenSum / 10000d, 1)});
        }
        usersList.sort((a, b) -> Double.compare((Double) b[2], (Double) a[2]));

        JSONObject stats = JSONUtils.toJSONObject(
                new String[]{"aibot", "aibotCount", "aibotUsers"},
                new Object[]{aibot.toArray(), ObjectUtils.round(tokenTotal / 10000d, 1), usersList.toArray()});
        stats.put("aibotDailyChats", aibotDailyChats.toArray());
        stats.put("aibotDailyUsers", aibotDailyUsers.toArray());
        stats.put("chats", realChats);
        stats.put("users", realUsers.size());
        return stats;
    }

    @GetMapping("aibot/chat-list")
    public RespBody adminChatList(HttpServletRequest request) {
        if (!UserHelper.isAdmin(getRequestUser(request))) return RespBody.error(403);

        final ID user = getIdParameter(request, "user");
        final ID agent = getIdParameter(request, "agent");
        final int pageNo = getIntParameter(request, "pageNo", 1);
        final int pageSize = 20;

        String where = CHAT_VISIBLE_WHERE;
        if (user != null) where += " and createdBy = '" + user + "'";
        if (agent != null) where += " and agentId = '" + agent + "'";
        String q = StringUtils.trimToEmpty(getParameter(request, "q"));
        if (StringUtils.isNotBlank(q)) {
            where += String.format(" and subject like '%%%s%%'", CommonsUtils.escapeSql(q));
        }

        long total = -1L;
        if (pageNo == 1) {
            total = ((Number) Application.createQueryNoFilter(
                    "select count(chatId) from AibotChat" + where).unique()[0]).longValue();
        }

        Object[][] array = Application.createQueryNoFilter(
                "select chatId,subject,createdBy,token,modifiedOn from AibotChat" + where + " order by modifiedOn desc")
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        JSONArray data = new JSONArray();
        for (Object[] o : array) {
            ID createdBy = (ID) o[2];
            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "subject", "userName", "token", "modifiedOn"},
                    new Object[]{o[0], o[1], UserService.AIBOT_USER.equals(createdBy)
                            ? Language.L("访客") : UserHelper.getName(createdBy),
                            o[3] == null ? 0L : o[3], I18nUtils.formatDate((Date) o[4])});
            data.add(item);
        }

        JSONObject res = JSONUtils.toJSONObject(
                new String[]{"total", "data"}, new Object[]{total, data});
        return RespBody.ok(res);
    }

    // 公开访问的匿名会话（登录用户走公开入口归属本人），内部调用会话归属 Rex 但无来源标记
    private static boolean isVisitorChat(ID user, String source) {
        return UserService.AIBOT_USER.equals(user) && ChatManager.SOURCE_PUB.equals(source);
    }

    @GetMapping("aibot/models")
    public RespBody modelList(HttpServletRequest request) {
        String baseUrl = getParameter(request, "baseUrl");
        String apiKey = getParameter(request, "apiKey");

        OpenAIClient client;
        if (StringUtils.isNotBlank(baseUrl) || StringUtils.isNotBlank(apiKey)) {
            if (StringUtils.isBlank(baseUrl)) baseUrl = Config.getServerUrl(null);
            if (StringUtils.isBlank(apiKey)) apiKey = Config.getSecret();
            client = Config.getClient(baseUrl, apiKey);
        } else {
            client = Config.getClient();
        }

        JSONObject contextMap = null;
        try {
            contextMap = (JSONObject) RBStore.fetchAibotModelContext(null);
        } catch (Exception ignored) {
        }

        try {
            ModelListPage page = client.models().list();
            List<JSONObject> models = new ArrayList<>();
            for (Model m : page.data()) {
                JSONObject item = new JSONObject(true);
                item.put("id", m.id());
                Long contextWindow = contextMap != null ? contextMap.getLong(m.id()) : null;
                item.put("contextWindow", contextWindow);
                models.add(item);
            }
            return RespBody.ok(models);
        } catch (Exception e) {
            log.error("Failed to list available models", e);
            return RespBody.error(e);
        }
    }

    // -- KITS

    @GetMapping("aibot-kits")
    public ModelAndView pageIntegrationAibotKits() {
        ModelAndView mv = createModelAndView("/admin/integration/aibot-kits");
        mv.getModel().put("_McpServerUrl", RebuildConfiguration.getHomeUrl("/gw/mcp/sse"));
        return mv;
    }

    @GetMapping("aibot-agents")
    public ModelAndView pageIntegrationAibotAgents() {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("智能体"));
        }
        return createModelAndView("/admin/integration/aibot-agents");
    }

    @GetMapping("aibot-minapps")
    public ModelAndView pageIntegrationAibotMinApps() {
        if (!License.isCommercial()) {
            return RbvMissingController.errorUnsupported(Language.L("微应用"));
        }

        return createModelAndView("/admin/integration/aibot-minapps");
    }

    @GetMapping("aibot/minapp-list")
    public RespBody minappList() {
        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,isDisabled,shareTo,config from AibotConfig where type = 'MINAPP' order by name")
                .array();

        // 近 90 日各微应用页面访问量
        Map<ID, Integer> visitCountMap = new HashMap<>();
        Object[][] visits = Application.createQueryNoFilter(
                "select source from CommonsLog where type = ? and logTime > ?")
                .setParameter(1, CommonsLog.TYPE_MINAPP)
                .setParameter(2, CalendarUtils.clearTime(CalendarUtils.addDay(-90)))
                .array();
        for (Object[] v : visits) {
            if (v[0] != null) visitCountMap.merge((ID) v[0], 1, Integer::sum);
        }

        List<JSONObject> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) o[4]);
            JSONObject publish = conf == null ? null : conf.getJSONObject("publish");

            list.add(JSONUtils.toJSONObject(
                    new String[]{"id", "name", "isDisabled", "shareTo", "icon", "bindUser", "publishEnabled", "url", "visitCount"},
                    new Object[]{o[0], o[1], o[2], o[3],
                            conf == null ? null : conf.getString("icon"),
                            conf == null ? null : conf.getString("bindUser"),
                            publish != null && publish.getBooleanValue("enabled"),
                            RebuildConfiguration.getHomeUrl("pages/minapp/" + o[0]),
                            visitCountMap.getOrDefault((ID) o[0], 0)}));
        }
        return RespBody.ok(list);
    }

    @GetMapping("aibot/minapp-stats")
    public RespBody minappStats(HttpServletRequest request) {
        ID minapp = getIdParameter(request, "minapp");
        if (minapp == null) return RespBody.error(404);

        Object[][] logs = Application.createQueryNoFilter(
                "select logTime from CommonsLog where type = ? and source = ? and logTime > ?")
                .setParameter(1, CommonsLog.TYPE_MINAPP)
                .setParameter(2, minapp)
                .setParameter(3, CalendarUtils.clearTime(CalendarUtils.addDay(-90)))
                .array();

        Map<String, Integer> visitsByDay = new TreeMap<>();
        for (Object[] o : logs) {
            visitsByDay.merge(CalendarUtils.format("yyyy-MM-dd", (Date) o[0]), 1, Integer::sum);
        }

        List<Object[]> dailyVisits = new ArrayList<>();
        for (Map.Entry<String, Integer> e : visitsByDay.entrySet()) {
            dailyVisits.add(new Object[]{e.getKey(), e.getValue()});
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[]{"visits", "dailyVisits"},
                new Object[]{logs.length, dailyVisits.toArray()}));
    }

    @PostMapping("aibot/minapp-save")
    public RespBody minappSave(@RequestBody JSONObject data) {
        ID id = ID.isId(data.getString("id")) ? ID.valueOf(data.getString("id")) : null;
        if (id == null) return RespBody.error(404);

        String name = StringUtils.trimToEmpty(data.getString("name"));
        if (name.length() > 60) return RespBody.error(Language.L("名称过长"));
        String icon = StringUtils.trimToNull(data.getString("icon"));
        String shareTo = StringUtils.defaultIfBlank(data.getString("shareTo"), ShareToManager.SHARE_ALL);

        boolean enabled = data.getBooleanValue("enabled");
        String bindUser = data.getString("bindUser");
        String accessPassword = StringUtils.trimToNull(data.getString("accessPassword"));
        if (!enabled) {
            bindUser = null;
            accessPassword = null;
        }

        Object[] x = Application.createQueryNoFilter(
                "select config from AibotConfig where configId = ? and type = 'MINAPP'")
                .setParameter(1, id)
                .unique();
        if (x == null) return RespBody.error(404);

        JSONObject config = (JSONObject) JSONUtils.parseSafe((String) x[0]);
        if (config == null) config = new JSONObject();

        config.put("icon", icon);
        config.put("bindUser", bindUser);
        config.put("accessPassword", accessPassword);
        config.put("publish", JSONUtils.toJSONObject("enabled", enabled));

        Long minuteVisitLimit = data.getLong("minuteVisitLimit");
        Long hourVisitLimit = data.getLong("hourVisitLimit");
        config.put("minuteVisitLimit", minuteVisitLimit != null && minuteVisitLimit > 0 ? minuteVisitLimit : null);
        config.put("hourVisitLimit", hourVisitLimit != null && hourVisitLimit > 0 ? hourVisitLimit : null);

        Record record = EntityHelper.forUpdate(id, UserContextHolder.getUser());
        record.setString("name", name);
        record.setString("shareTo", shareTo);
        record.setString("config", config.toJSONString());
        Application.getBean(AibotConfigService.class).update(record);
        return RespBody.ok();
    }

    @GetMapping("aibot-minapp/{minappId}")
    public ModelAndView pageAibotMinApp(@PathVariable String minappId, HttpServletResponse response) throws IOException {
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
        // 源码由 AI 维护，页面无需展示
        conf.remove("html");

        ModelAndView mv = createModelAndView("/admin/integration/aibot-minapp-editor");
        mv.getModelMap().put("minappDisplayName", StringUtils.defaultString((String) minapp[0]));
        mv.getModelMap().put("minappId", minappId2);
        mv.getModelMap().put("minappName", StringUtils.trimToEmpty((String) minapp[0]));
        mv.getModelMap().put("minappConfig", conf.toJSONString());
        mv.getModelMap().put("minappShareTo", StringUtils.defaultIfBlank((String) minapp[3], ShareToManager.SHARE_ALL));
        mv.getModelMap().put("pubUrl", RebuildConfiguration.getHomeUrl("pages/minapp/" + minappId2));
        return mv;
    }

    @GetMapping("aibot/agent-list")
    public RespBody agentList() {
        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,config,isDisabled from AibotConfig where type = 'AGENT' order by name")
                .array();

        // 近 90 日各智能体会话数，含访客会话（口径同 stats/chat-list）
        final Date xday = CalendarUtils.clearTime(CalendarUtils.addDay(-90));
        Map<ID, Integer> chatCountMap = new HashMap<>();
        Object[][] chats = Application.createQueryNoFilter(
                "select agentId from AibotChat" + CHAT_VISIBLE_WHERE + " and createdOn > ?")
                .setParameter(1, xday)
                .array();
        for (Object[] c : chats) {
            if (c[0] != null) chatCountMap.merge((ID) c[0], 1, Integer::sum);
        }

        List<JSONObject> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = new JSONObject(true);
            item.put("id", o[0]);
            item.put("name", StringUtils.isBlank((String) o[1]) ? RebuildConfiguration.get(ConfigurationItem.AibotName) : o[1]);
            JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) o[2]);
            if (conf == null) conf = new JSONObject();
            conf.remove("AibotDSSecret");
            item.put("config", conf);
            item.put("isDisabled", o[3]);
            item.put("chatCount", chatCountMap.getOrDefault((ID) o[0], 0));

            // 设置了日限额的才返回今日用量（列表卡片显示使用百分比）
            Long dailyLimit = conf.getLong("dailyTokenLimit");
            if (dailyLimit != null && dailyLimit > 0) {
                item.put("usageToday", AgentQuota.usage((ID) o[0])[0]);
            }
            list.add(item);
        }
        return RespBody.ok(list);
    }

    @GetMapping("aibot-agent/{agentId}")
    public ModelAndView pageAibotAgent(@PathVariable String agentId, HttpServletResponse response) throws IOException {
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

        ModelAndView mv = createModelAndView("/admin/integration/aibot-agent-editor");
        mv.getModelMap().put("agentDisplayName",
                StringUtils.defaultIfBlank((String) agent[0], RebuildConfiguration.get(ConfigurationItem.AibotName)));
        mv.getModelMap().put("agentId", agentId2);
        mv.getModelMap().put("pubUrl", RebuildConfiguration.getHomeUrl("/aibot/pub/" + agentId2));
        mv.getModelMap().put("agentName", StringUtils.trimToEmpty((String) agent[0]));
        JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) agent[1]);
        mv.getModelMap().put("agentConfig", conf != null ? conf.toJSONString() : null);
        mv.getModelMap().put("agentShareTo", StringUtils.defaultIfBlank((String) agent[3], ShareToManager.SHARE_ALL));

        long[] usage = AgentQuota.usage(agentId2);
        mv.getModelMap().put("usageToday", ObjectUtils.round(usage[0] / 10000d, 2));
        mv.getModelMap().put("usageMonth", ObjectUtils.round(usage[1] / 10000d, 2));
        return mv;
    }

    @GetMapping("aibot/tools")
    public RespBody toolList() {
        return RespBody.ok(ToolDefs.listTools(true, false));
    }

    @GetMapping("aibot/skill-list")
    public RespBody skillList() {
        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,config,isDisabled from AibotConfig where type = 'SKILL' order by name")
                .array();

        List<JSONObject> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = new JSONObject(true);
            item.put("id", o[0]);
            item.put("name", o[1]);
            JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) o[2]);
            item.put("config", conf != null ? conf : new JSONObject());
            item.put("isDisabled", o[3]);
            list.add(item);
        }
        return RespBody.ok(list);
    }

    @GetMapping("aibot/skills")
    public RespBody skills() {
        return RespBody.ok(SkillDefs.listSkills());
    }

    @GetMapping("aibot/kb-list")
    public RespBody kbList() {
        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,config,isDisabled from AibotConfig where type = 'KNOWLEDGE' order by name")
                .array();

        List<JSONObject> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = new JSONObject(true);
            item.put("id", o[0]);
            item.put("name", o[1]);
            JSONObject conf = (JSONObject) JSONUtils.parseSafe((String) o[2]);
            if (conf != null) {
                item.put("description", conf.getString("description"));
                item.put("sourceType", conf.getString("sourceType"));
                item.put("sourceConfig", conf.getString("sourceConfig"));
                item.put("chunkCount", conf.getIntValue("chunkCount"));
            }
            item.put("isDisabled", o[3]);
            list.add(item);
        }
        return RespBody.ok(list);
    }

    @PostMapping("aibot/kb-build")
    public RespBody build(HttpServletRequest req) {
        ID knowledgeId = getIdParameterNotNull(req, "id");
        Object[] knowledge = Application.createQueryNoFilter(
                "select configId from AibotConfig where configId = ? and type = 'KNOWLEDGE'")
                .setParameter(1, knowledgeId)
                .unique();
        if (knowledge == null) return RespBody.error();

        // 已有排队中的构建任务（执行时会重读最新配置），无需重复提交
        if (KnowledgeBuilder.isBuildPending(knowledgeId)) return RespBody.ok();

        // -1=构建中 0=构建失败。任务开始执行时会再次重置，避免被前一个任务完成状态覆盖
        KnowledgeBuilder.updateChunkCount(knowledgeId, -1);
        KnowledgeBuilder.buildAsync(knowledgeId);
        return RespBody.ok();
    }
}