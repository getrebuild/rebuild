/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.JsonSchemaValidator;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dashboard.ChartConfigService;
import com.rebuild.core.service.dashboard.DashboardConfigService;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 2026/11/4
 */
@Slf4j
public class BuildChart implements Tool {

    // 合法图表类型
    private static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList(
            "TABLE", "INDEX", "LINE", "BAR", "BAR2", "BAR3", "PARETO", "PIE",
            "FUNNEL", "TREEMAP", "RADAR", "SCATTER", "CNMAP", "DATALIST2", "DOLOR", "RANK"
    ));

    // 各类型轴数量约束：{minDim, maxDim, minNum, maxNum}
    private static final Map<String, int[]> TYPE_AXIS_LIMITS = new HashMap<>();
    static {
        TYPE_AXIS_LIMITS.put("INDEX", new int[]{0, 0, 1, 2});
        TYPE_AXIS_LIMITS.put("PIE", new int[]{1, 1, 1, 1});
        TYPE_AXIS_LIMITS.put("TREEMAP", new int[]{1, 3, 1, 1});
        TYPE_AXIS_LIMITS.put("RADAR", new int[]{1, 1, 1, 3});
        TYPE_AXIS_LIMITS.put("SCATTER", new int[]{0, 3, 2, 2});
        TYPE_AXIS_LIMITS.put("CNMAP", new int[]{1, 1, 0, 9});
        TYPE_AXIS_LIMITS.put("DATALIST2", new int[]{1, 9, 0, 0});
        TYPE_AXIS_LIMITS.put("DOLOR", new int[]{1, 1, 0, 0});
        TYPE_AXIS_LIMITS.put("PARETO", new int[]{1, 1, 1, 1});
        TYPE_AXIS_LIMITS.put("TABLE", new int[]{0, 9, 0, 99});
        TYPE_AXIS_LIMITS.put("LINE", new int[]{1, 2, 1, 9});
        TYPE_AXIS_LIMITS.put("BAR", new int[]{1, 2, 1, 9});
        TYPE_AXIS_LIMITS.put("BAR2", new int[]{1, 2, 1, 9});
        TYPE_AXIS_LIMITS.put("BAR3", new int[]{1, 2, 1, 9});
        TYPE_AXIS_LIMITS.put("FUNNEL", new int[]{0, 1, 1, 9});
        TYPE_AXIS_LIMITS.put("RANK", new int[]{1, 2, 1, 9});
    }

    // 合法维度 calc（日期/分类字段的计算方式）
    private static final Set<String> VALID_DIM_CALCS = new HashSet<>(Arrays.asList(
            "Y", "Q", "M", "W", "D", "H", "I", "L1", "L2", "L3", "L4"
    ));

    // 合法数值 calc（聚合方式）
    private static final Set<String> VALID_NUM_CALCS = new HashSet<>(Arrays.asList(
            "SUM", "AVG", "MAX", "MIN", "COUNT", "COUNT2", "COUNT_DISTINCT", "_FORMULA"
    ));

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        // 权限校验
        ID user = UserContextHolder.getUser();
        if (!Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomChart)) {
            throw new KnownToolException("无图表创建权限");
        }

        // 解析实体
        String entityIdent = args.getString("entity");
        if (StringUtils.isBlank(entityIdent)) {
            throw new KnownToolException("实体 (entity) 不能为空");
        }
        Entity entity = ToolHelper.resolveEntity(entityIdent);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        // 解析标题
        String title = args.getString("title");
        if (StringUtils.isBlank(title)) {
            throw new KnownToolException("图表标题 (title) 不能为空");
        }

        // 解析类型
        String type = args.getString("type");
        if (StringUtils.isBlank(type)) {
            throw new KnownToolException("图表类型 (type) 不能为空");
        }
        type = type.toUpperCase();
        if (!VALID_TYPES.contains(type)) {
            throw new KnownToolException("无效图表类型 : " + type
                    + "，可用值 : " + StringUtils.join(VALID_TYPES, ", "));
        }

        // 解析轴配置
        JSONObject axis = args.getJSONObject("axis");
        if (axis == null || axis.isEmpty()) {
            throw new KnownToolException("轴配置 (axis) 不能为空，含 dimension（维度数组）和 numerical（数值数组）");
        }

        JSONArray dimensions = axis.getJSONArray("dimension");
        JSONArray numericals = axis.getJSONArray("numerical");
        if (dimensions == null) dimensions = new JSONArray();
        if (numericals == null) numericals = new JSONArray();

        // 内置轴数量校验（免费版也能生效）
        int[] limits = TYPE_AXIS_LIMITS.get(type);
        if (limits != null) {
            if (dimensions.size() < limits[0] || dimensions.size() > limits[1]) {
                throw new KnownToolException(String.format(
                        "图表类型 %s 要求维度数量 %d~%d，当前 %d",
                        type, limits[0], limits[1], dimensions.size()));
            }
            if (numericals.size() < limits[2] || numericals.size() > limits[3]) {
                throw new KnownToolException(String.format(
                        "图表类型 %s 要求数值数量 %d~%d，当前 %d",
                        type, limits[2], limits[3], numericals.size()));
            }
        }

        // 解析维度字段
        for (int i = 0; i < dimensions.size(); i++) {
            JSONObject dim = dimensions.getJSONObject(i);
            String field = dim.getString("field");
            if (StringUtils.isBlank(field)) {
                throw new KnownToolException("第 " + (i + 1) + " 个维度的 field 不能为空");
            }
            String resolved = ToolHelper.resolveFieldPath(entity, field);
            dim.put("field", resolved);

            // 兜底校验 calc（免费版跳过 Schema 校验时生效）
            String calc = dim.getString("calc");
            if (StringUtils.isNotBlank(calc)) {
                calc = calc.toUpperCase();
                if (!VALID_DIM_CALCS.contains(calc)) {
                    throw new KnownToolException("第 " + (i + 1) + " 个维度的 calc 无效 : " + calc
                            + "，可用值 : " + StringUtils.join(VALID_DIM_CALCS, "/"));
                }
                dim.put("calc", calc);
            }
        }

        // 解析数值字段
        for (int i = 0; i < numericals.size(); i++) {
            JSONObject num = numericals.getJSONObject(i);
            String field = num.getString("field");
            if (StringUtils.isBlank(field)) {
                throw new KnownToolException("第 " + (i + 1) + " 个数值的 field 不能为空");
            }
            String resolved = ToolHelper.resolveFieldPath(entity, field);
            num.put("field", resolved);

            String calc = num.getString("calc");
            if (StringUtils.isBlank(calc)) {
                throw new KnownToolException("第 " + (i + 1) + " 个数值的 calc 不能为空（SUM/AVG/MAX/MIN/COUNT/COUNT2/COUNT_DISTINCT/_FORMULA）");
            }
            calc = calc.toUpperCase();
            if (!VALID_NUM_CALCS.contains(calc)) {
                throw new KnownToolException("第 " + (i + 1) + " 个数值的 calc 无效 : " + calc
                        + "，可用值 : " + StringUtils.join(VALID_NUM_CALCS, "/"));
            }
            num.put("calc", calc);

            // 数值级过滤条件校验
            JSONObject numFilter = num.getJSONObject("filter");
            if (numFilter != null && !numFilter.isEmpty()) {
                ToolHelper.validateFilter(entity, numFilter);
            }
        }

        // 重组 axis（确保 dimension/numerical 数组存在）
        JSONObject cleanAxis = new JSONObject(true);
        cleanAxis.put("dimension", dimensions);
        cleanAxis.put("numerical", numericals);

        // 解析可选 option
        JSONObject option = args.getJSONObject("option");

        // 解析可选 filter
        JSONObject filter = args.getJSONObject("filter");
        if (filter != null && !filter.isEmpty()) {
            ToolHelper.validateFilter(entity, filter);
        }

        // 组装 chart config
        JSONObject config = new JSONObject(true);
        config.put("entity", entity.getName());
        config.put("title", title);
        config.put("type", type);
        config.put("axis", cleanAxis);
        if (option != null && !option.isEmpty()) config.put("option", option);
        if (filter != null && !filter.isEmpty()) config.put("filter", filter);

        // Schema 校验（商业版生效，免费版返回 null 跳过）
        List<String> schemaErrors = JsonSchemaValidator.validateErrors(JsonSchemaValidator.CHART_CONFIG, config);
        if (schemaErrors != null && !schemaErrors.isEmpty()) {
            throw new KnownToolException("图表配置不符合规范 : " + ToolHelper.joinErrors(schemaErrors)
                    + "。请修正后重试，可用 GetConfigSchema(schema=chart-config) 查看完整定义");
        }

        // 解析可选仪表盘 ID（先校验存在与权限，避免图表创建后才失败留下孤儿图表）
        String dashidStr = args.getString("dashid");
        ID dashid = null;
        Object[] dash = null;
        if (StringUtils.isNotBlank(dashidStr)) {
            dashid = ToolHelper.resolveId(dashidStr, "dashid", EntityHelper.DashboardConfig);
            dash = Application.createQueryNoFilter(
                    "select config,createdBy from DashboardConfig where configId = ?")
                    .setParameter(1, dashid)
                    .unique();
            if (dash == null) {
                throw new KnownToolException("指定的仪表盘不存在 : " + dashidStr);
            }
            // 校验仪表盘编辑权限：管理员或创建者本人
            if (!UserHelper.isAdmin(user) && !user.equals(dash[1])) {
                throw new KnownToolException("无权操作他人仪表盘");
            }
        }

        // 创建图表配置记录
        Record record = EntityHelper.forNew(EntityHelper.ChartConfig, user);
        record.setString("config", config.toJSONString());
        record.setString("title", title);
        record.setString("chartType", type);
        record.setString("belongEntity", entity.getName());
        record = Application.getBean(ChartConfigService.class).createOrUpdate(record);
        ID chartId = record.getPrimary();

        log.info("Chart created via AI : {} on {} ({})", chartId, entity.getName(), type);

        // 添加到仪表盘
        if (dash != null) {
            JSONArray dashConfig = JSON.parseArray((String) dash[0]);
            JSONObject item = JSONUtils.toJSONObject("chart", chartId);
            item.put("w", 4);
            item.put("h", 4);
            dashConfig.add(item);

            Record dashRecord = EntityHelper.forUpdate(dashid, user);
            dashRecord.setString("config", dashConfig.toJSONString());
            Application.getBean(DashboardConfigService.class).createOrUpdate(dashRecord);
        }

        String designUrl = AppUtils.getContextPath("/dashboard/chart-design?id=" + chartId);

        StringBuilder message = new StringBuilder();
        message.append(String.format("已成功创建图表 [%s]（%s - %s），[点击查看/编辑图表](%s)，请将此链接展示给用户，以便其核对配置是否符合预期",
                title, EasyMetaFactory.getLabel(entity), type, designUrl));
        if (dashid != null) {
            String dashUrl = AppUtils.getContextPath("/dashboard/home?d=" + dashid);
            message.append(String.format("，图表已添加到仪表盘中（默认尺寸 4x4），[点击查看仪表盘](%s)", dashUrl));
        }

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("id", chartId.toLiteral());
        result.put("entity", entity.getName());
        result.put("title", title);
        result.put("type", type);
        result.put("url", designUrl);
        if (dashid != null) {
            result.put("dashid", dashid.toLiteral());
            result.put("dashUrl", AppUtils.getContextPath("/dashboard/home?d=" + dashid));
        }
        result.put("message", message.toString());
        return result;
    }
}
