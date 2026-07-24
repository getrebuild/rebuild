/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.ReportsFile;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * 报表导出工具
 *
 * @author Zixin
 * @since 2026/7/20
 */
@Slf4j
public class ExportReport implements Tool {

    private static final int MAX_SEARCH_RESULTS = 10;

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new ToolException("实体名称不能为空");
        }

        Entity entity = ListEntities.resolveEntity(entityName);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityName);
        }

        String report = args.getString("report");
        String record = args.getString("record");

        // 未指定报表，返回可用报表列表
        if (StringUtils.isBlank(report)) {
            return listReports(entity);
        }

        // 未指定记录，提示需要
        if (StringUtils.isBlank(record)) {
            throw new ToolException("请提供要导出报表的记录名称或编号 (record)");
        }

        // 精确 ID 直接导出
        if (ID.isId(record)) {
            return exportReport(entity, ID.valueOf(report), ID.valueOf(record));
        }

        // 按名称/编号搜索记录
        return searchAndExport(entity, ID.valueOf(report), record);
    }

    /**
     * 列出实体下可用报表
     */
    private JSONObject listReports(Entity entity) {
        JSONArray reports = DataReportManager.instance.getReportTemplates(
                entity, DataReportManager.TYPE_RECORD, null);

        // 也获取列表类型报表
        JSONArray listReports = DataReportManager.instance.getReportTemplates(
                entity, DataReportManager.TYPE_LIST, null);
        reports.addAll(listReports);

        if (reports.isEmpty()) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "实体 [" + entity.getName() + "] 下暂无可用报表模板"});
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "reports"},
                new Object[]{"ok", entity.getName(), reports});
    }

    /**
     * 按名称/编号搜索记录，匹配唯一则直接导出，否则返回列表供用户选择
     */
    private JSONObject searchAndExport(Entity entity, ID reportId, String keyword) {
        Set<String> searchFields = ParseHelper.buildQuickFields(entity, null);
        if (searchFields.isEmpty()) {
            throw new ToolException("实体 [" + entity.getName() + "] 无可搜索字段");
        }

        String like = " like '%" + CommonsUtils.escapeSql(keyword) + "%'";
        String where = StringUtils.join(searchFields.iterator(), like + " or ") + like;

        String sql = String.format("select %s from %s where %s order by modifiedOn desc",
                entity.getPrimaryField().getName(), entity.getName(), where);

        Object[][] array = Application.createQueryNoFilter(sql).setLimit(MAX_SEARCH_RESULTS).array();

        if (array == null || array.length == 0) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", String.format("未找到匹配 \"%s\" 的记录，请尝试其他关键词", keyword)});
        }

        // 唯一匹配，直接导出
        if (array.length == 1) {
            ID recordId = (ID) array[0][0];
            return exportReport(entity, reportId, recordId);
        }

        // 多个匹配，返回列表供用户选择
        JSONArray records = new JSONArray();
        for (int i = 0; i < array.length; i++) {
            ID recordId = (ID) array[i][0];
            String label = FieldValueHelper.getLabelNotry(recordId);

            JSONObject item = new JSONObject();
            item.put("no", i + 1);
            item.put("id", recordId.toLiteral());
            item.put("name", label);
            records.add(item);
        }

        JSONObject result = new JSONObject();
        result.put("status", "ok");
        result.put("message", String.format("找到 %d 条匹配 \"%s\" 的记录，请告诉用户选择要导出哪一条（回复编号 1-%d）",
                array.length, keyword, array.length));
        result.put("records", records);
        return result;
    }

    /**
     * 导出报表并返回下载链接
     */
    private JSONObject exportReport(Entity entity, ID reportId, ID recordId) {
        File output;
        try {
            EasyExcelGenerator reportGenerator = EasyExcelGenerator.create(reportId, Collections.singletonList(recordId));
            reportGenerator.setReportId(reportId);
            output = reportGenerator.generate();
        } catch (Exception ex) {
            log.error("Report export failed : {} / {}", reportId, recordId, ex);
            throw new ToolException("报表生成失败 : " + ex.getMessage());
        }

        if (output == null) {
            throw new ToolException("无法输出报表，请检查报表模板是否有误");
        }

        if (output instanceof ReportsFile) {
            try {
                output = ((ReportsFile) output).toZip(false);
            } catch (IOException e) {
                throw new ToolException("报表打包失败 : " + e.getMessage());
            }
        }

        String fileName = DataReportManager.getPrettyReportName(reportId, recordId, output.getName());

        // 构建下载链接
        String fileUrl = String.format("/filex/download/%s?temp=yes&_csrfToken=%s&attname=%s",
                CodecUtils.urlEncode(output.getName()),
                AuthTokenManager.generateCsrfToken(90),
                CodecUtils.urlEncode(fileName));
        fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);

        JSONObject result = new JSONObject();
        result.put("status", "ok");
        result.put("fileName", fileName);
        result.put("downloadUrl", fileUrl);
        result.put("message", String.format("报表 [%s] 已生成，请点击下载链接获取文件", fileName));
        return result;
    }
}
