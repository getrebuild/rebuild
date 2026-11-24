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
import com.rebuild.core.rbstore.RBStore;
import com.rebuild.core.rbstore.SkillImporter;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.DataDesensitized;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.RebuildWebConfigurer;
import com.rebuild.web.admin.ConfigurationController;
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

    // 会话列表仅显示真实用户会话与公开访问会话，定时任务、集成等系统调用产生的会话不显示
    private static final String CHAT_VISIBLE_WHERE = " where " + ChatManager.VISIBLE_WHERE;

    @PostMapping("aibot")
    public RespBody postIntegrationAibot(@RequestBody JSONObject data) {
        if (data.getBooleanValue("__clear__")) {
            ConfigurationController.clearConfigurationByPrefix("Aibot");
            return RespBody.ok();
        }

        // API 相关配置（地址、秘钥、模型）有变更时，保存前测试连通性
        String error = testApiConnectivity(data);
        if (error != null) return RespBody.error(error);

        ConfigurationController.setValues(data);
        Application.getBean(RebuildWebConfigurer.class).init();
        Config.getClient(true);
        return RespBody.ok();
    }

    /**
     * 测试 API 连通性，通过返回 null
     *
     * @param data
     * @return
     */
    private String testApiConnectivity(JSONObject data) {
        boolean apiChanged = data.containsKey("AibotDSUrl") || data.containsKey("AibotDSSecret") || data.containsKey("AibotBaseDefModel");
        if (!apiChanged) return null;
        return Config.testApiConnectivity(
                data.getString("AibotDSUrl"), data.getString("AibotDSSecret"), data.getString("AibotBaseDefModel"));
    }

    @GetMapping("aibot-overview")
    public ModelAndView pageIntegrationAibotOverview() {
        return new ModelAndView("redirect:/admin/aibot2/overview");
    }

    @GetMapping("aibot-chats")
    public ModelAndView pageIntegrationAibotChats() {
        return new ModelAndView("redirect:/admin/aibot2/chats");
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
        mv.getModel().put("_McpServerUrl", RebuildConfiguration.getHomeUrl("/gw/mcp/sse"));
        return mv;
    }

    @GetMapping("aibot/stats")
    public JSON statsAibot(HttpServletRequest request) {
        final ID agent = getIdParameter(request, "agent");
        final Date xday = CalendarUtils.clearTime(CalendarUtils.addDay(-90));

        // 词元统计全量（含内部调用会话）
        Query tokenQuery = Application.createQueryNoFilter(
                        "select createdOn, createdBy, token from AibotChat where createdOn > ?"
                                + (agent == null ? "" : " and agentId = ?"))
                .setParameter(1, xday);
        if (agent != null) tokenQuery.setParameter(2, agent);
        Object[][] tokenRows = tokenQuery.array();

        // 会话数/使用人数与会话列表同口径
        Query chatQuery = Application.createQueryNoFilter(
                        "select createdOn, createdBy, source from AibotChat where " + ChatManager.VISIBLE_WHERE
                                + " and createdOn > ?"
                                + (agent == null ? "" : " and agentId = ?"))
                .setParameter(1, xday);
        if (agent != null) chatQuery.setParameter(2, agent);
        Object[][] chatRows = chatQuery.array();

        // 按日（词元/会话数/使用人数）聚合
        Map<String, Long> tokensByDay = new TreeMap<>();
        Map<String, Integer> chatsByDay = new TreeMap<>();
        Map<String, Set<ID>> usersByDay = new TreeMap<>();
        Set<ID> realUsers = new HashSet<>();
        long tokenTotal = 0;
        int realChats = 0;

        for (Object[] o : tokenRows) {
            String day = CalendarUtils.format("yyyy-MM-dd", (Date) o[0]);
            long token = o[2] == null ? 0L : ((Number) o[2]).longValue();

            tokensByDay.merge(day, token, Long::sum);
            tokenTotal += token;
        }

        // 会话数含真实用户与访客，使用人数不含访客
        for (Object[] o : chatRows) {
            String day = CalendarUtils.format("yyyy-MM-dd", (Date) o[0]);
            ID user = (ID) o[1];
            boolean visitor = isVisitorChat(user, (String) o[2]);

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

        JSONObject stats = JSONUtils.toJSONObject(
                new String[]{"aibot", "aibotCount"},
                new Object[]{aibot.toArray(), ObjectUtils.round(tokenTotal / 10000d, 1)});
        stats.put("aibotDailyChats", aibotDailyChats.toArray());
        stats.put("aibotDailyUsers", aibotDailyUsers.toArray());
        stats.put("chats", realChats);
        stats.put("users", realUsers.size());
        return stats;
    }

    @PostMapping("aibot/models")
    public RespBody modelList(@RequestBody JSONObject data) {
        String baseUrl = data.getString("baseUrl");
        String apiKey = data.getString("apiKey");

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

    // -- MODELS

    @GetMapping("aibot-models")
    public ModelAndView pageIntegrationAibotModels() {
        return new ModelAndView("redirect:/admin/aibot2/models");
    }

    // -- KITS

    @GetMapping("aibot-kits")
    public ModelAndView pageIntegrationAibotKits() {
        return new ModelAndView("redirect:/admin/aibot2/skills");
    }

    @GetMapping("aibot-kb")
    public ModelAndView pageIntegrationAibotKb() {
        return new ModelAndView("redirect:/admin/aibot2/kb");
    }

    @GetMapping("aibot-skills")
    public ModelAndView pageIntegrationAibotSkills() {
        return new ModelAndView("redirect:/admin/aibot2/skills");
    }

    @GetMapping("aibot-tools")
    public ModelAndView pageIntegrationAibotTools() {
        return new ModelAndView("redirect:/admin/aibot2/tools");
    }


    @GetMapping("aibot/tool-list")
    public RespBody toolList() {
        return RespBody.ok(ToolDefs.listTools(true, true, false));
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

    @PostMapping("aibot/import-skills")
    public RespBody importSkills(HttpServletRequest request) {
        String[] names = getParameterNotNull(request, "names").split(",");

        SkillImporter importer = new SkillImporter();
        importer.setSkillNames(names);
        importer.setUser(getRequestUser(request));

        try {
            TaskExecutors.run(importer);
            return importer.getSucceeded() > 0
                    ? RespBody.ok() : RespBody.error(importer.getErrorMessage());

        } catch (Exception ex) {
            log.error("Cannot import skills : {}", getParameter(request, "names"), ex);
            return RespBody.error(CommonsUtils.getRootMessage(ex));
        }
    }

    @GetMapping("aibot/kb-list")
    public RespBody kbList(HttpServletRequest request) {
        boolean lite = getBoolParameter(request, "lite");
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
                if (!lite) item.put("sourceConfig", conf.getString("sourceConfig"));
                item.put("chunkCount", conf.getIntValue("chunkCount"));
            }
            item.put("isDisabled", o[3]);
            list.add(item);
        }
        return RespBody.ok(list);
    }

    @PostMapping("aibot/kb-build")
    public RespBody kbBuild(HttpServletRequest req) {
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

    // - Agent

    @GetMapping("aibot-agents")
    public ModelAndView pageIntegrationAibotAgents() {
        return new ModelAndView("redirect:/admin/aibot2/agents");
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
    public ModelAndView pageAibotAgent(@PathVariable String agentId) {
        return new ModelAndView("redirect:/admin/aibot2/agent/" + agentId);
    }

    // - MinApp

    @GetMapping("aibot-minapps")
    public ModelAndView pageIntegrationAibotMinApps() {
        return new ModelAndView("redirect:/admin/aibot2/minapps");
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
    public ModelAndView pageAibotMinApp(@PathVariable String minappId) {
        return new ModelAndView("redirect:/admin/aibot2/minapp/" + minappId);
    }

    // -- Others

    @GetMapping("aibot/chat-list")
    public RespBody adminChatList(HttpServletRequest request) {
        if (!UserHelper.isAdmin(getRequestUser(request))) return RespBody.error(403);

        final ID user = getIdParameter(request, "user");
        final ID agent = getIdParameter(request, "agent");
        final boolean defaultAgent = agent == null && "default".equals(getParameter(request, "agent"));
        final int pageNo = getIntParameter(request, "pageNo", 1);
        final int pageSize = 20;

        String where = CHAT_VISIBLE_WHERE;
        if (user != null) where += " and createdBy = '" + user + "'";
        if (agent != null) where += " and agentId = '" + agent + "'";
        else if (defaultAgent) where += " and agentId is null";
        String q = StringUtils.trimToEmpty(getParameter(request, "q"));
        if (StringUtils.isNotBlank(q)) {
            where += String.format(" and subject like '%%%s%%'", CommonsUtils.escapeSql(q));
        }
        List<Object> dateParams = new ArrayList<>();
        String startDateStr = StringUtils.trimToNull(getParameter(request, "startDate"));
        Date startDate = startDateStr == null ? null : CalendarUtils.parse(startDateStr, "yyyy-MM-dd");
        if (startDate != null) {
            where += " and modifiedOn >= ?";
            dateParams.add(CalendarUtils.clearTime(startDate));
        }
        String endDateStr = StringUtils.trimToNull(getParameter(request, "endDate"));
        Date endDate = endDateStr == null ? null : CalendarUtils.parse(endDateStr, "yyyy-MM-dd");
        if (endDate != null) {
            where += " and modifiedOn < ?";
            dateParams.add(CalendarUtils.addDay(endDate, 1));
        }

        long total = -1L;
        if (pageNo == 1) {
            Query countQuery = Application.createQueryNoFilter("select count(chatId) from AibotChat" + where);
            for (int i = 0; i < dateParams.size(); i++) countQuery.setParameter(i + 1, dateParams.get(i));
            total = ((Number) countQuery.unique()[0]).longValue();
        }

        Query listQuery = Application.createQueryNoFilter(
                "select chatId,subject,createdBy,token,modifiedOn,summary from AibotChat" + where + " order by modifiedOn desc");
        for (int i = 0; i < dateParams.size(); i++) listQuery.setParameter(i + 1, dateParams.get(i));
        Object[][] array = listQuery.setLimit(pageSize, pageNo * pageSize - pageSize).array();

        JSONArray data = new JSONArray();
        for (Object[] o : array) {
            ID createdBy = (ID) o[2];
            String summaryText = null;
            if (JSONUtils.wellFormat(o[5])) {
                JSON summary = JSONUtils.parseSafe((String) o[5]);
                if (summary instanceof JSONObject) summaryText = ((JSONObject) summary).getString("short");
            }
            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "subject", "userName", "token", "modifiedOn", "summary"},
                    new Object[]{o[0], o[1], UserService.AIBOT_USER.equals(createdBy)
                            ? Language.L("访客") : UserHelper.getName(createdBy),
                            o[3] == null ? 0L : o[3], o[4] == null ? null
                                    : CalendarUtils.getDateFormat("yyyy-MM-dd HH:mm").format((Date) o[4]), summaryText});
            data.add(item);
        }

        JSONObject res = JSONUtils.toJSONObject(
                new String[]{"total", "data", "pageSize"}, new Object[]{total, data, pageSize});
        return RespBody.ok(res);
    }

    // 公开访问的匿名会话（登录用户走公开入口归属本人），内部调用会话归属 Rex 但无来源标记
    private static boolean isVisitorChat(ID user, String source) {
        return UserService.AIBOT_USER.equals(user) && ChatManager.SOURCE_PUB.equals(source);
    }
}
