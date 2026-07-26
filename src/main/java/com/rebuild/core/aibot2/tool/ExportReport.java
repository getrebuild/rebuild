/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.dataimport.DataExporter;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.ReportsFile;
import com.rebuild.core.service.datareport.TemplateFile;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.RbvFunction;
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

import static com.rebuild.core.service.datareport.DataReportManager.TYPE_LIST;
import static com.rebuild.core.service.datareport.DataReportManager.TYPE_RECORD;

/**
 * 报表导出工具
 *
 * @author Zixin
 * @since 2026/7/20
 */
@Slf4j
public class ExportReport implements Tool {

    private static final int MAX_SEARCH_RESULTS = 10;

    // LIST 报表最大导出行数
    private static final int MAX_LIST_EXPORT_ROWS = 1000;

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new ToolException("实体名称不能为空");
        }

        Entity entity = ListEntities.resolveEntity(entityName);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }

        String report = args.getString("report");
        String record = args.getString("record");

        if (StringUtils.isBlank(report)) {
            return listReports(entity);
        }

        ID reportId = ID.valueOf(report);
        TemplateFile tt = DataReportManager.instance.buildTemplateFile(reportId, entity);

        JSONArray filter = args.getJSONArray("filter");
        String equation = args.getString("equation");

        // LIST 报表按条件导出多条记录，其他为单记录报表
        if (tt.type == DataReportManager.TYPE_LIST) {
            return exportListReport(entity, reportId, record, filter, equation);
        }

        if (StringUtils.isBlank(record)) {
            throw new ToolException("请提供要导出报表的记录名称或编号 (record)");
        }

        if (ID.isId(record)) {
            return exportReport(entity, reportId, tt, ID.valueOf(record));
        }

        return searchAndExport(entity, reportId, tt, record);
    }

    private JSONObject listReports(Entity entity) {
        JSONArray reports = DataReportManager.instance.getReportTemplates(entity, TYPE_RECORD, null);
        for (Object o : reports) ((JSONObject) o).put("type", "RECORD");

        JSONArray listReports = DataReportManager.instance.getReportTemplates(entity, TYPE_LIST, null);
        for (Object o : listReports) ((JSONObject) o).put("type", "LIST");
        reports.addAll(listReports);

        if (reports.isEmpty()) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "实体 [" + EasyMetaFactory.getLabel(entity) + "] 下暂无可用报表模板"});
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "reports"},
                new Object[]{"ok", entity.getName(), reports});
    }

    private JSONObject searchAndExport(Entity entity, ID reportId, TemplateFile tt, String keyword) {
        Set<String> searchFields = ParseHelper.buildQuickFields(entity, null);
        if (searchFields.isEmpty()) {
            throw new ToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 无可搜索字段");
        }

        String like = " like '%" + CommonsUtils.escapeSql(keyword) + "%'";
        String where = StringUtils.join(searchFields.iterator(), like + " or ") + like;

        String sql = String.format("select %s from %s where %s order by modifiedOn desc",
                entity.getPrimaryField().getName(), entity.getName(), where);

        Object[][] array = Application.createQuery(sql).setLimit(MAX_SEARCH_RESULTS).array();

        if (array == null || array.length == 0) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", String.format("未找到匹配 \"%s\" 的记录，请尝试其他关键词", keyword)});
        }

        if (array.length == 1) {
            ID recordId = (ID) array[0][0];
            return exportReport(entity, reportId, tt, recordId);
        }

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

    private JSONObject exportReport(Entity entity, ID reportId, TemplateFile tt, ID recordId) {
        File output;
        try {
            EasyExcelGenerator reportGenerator;

            if (tt.type == DataReportManager.TYPE_WORD) {
                reportGenerator = RbvFunction.call().createWord(reportId, new ID[]{recordId});
            } else if (tt.type == DataReportManager.TYPE_HTML5) {
                reportGenerator = RbvFunction.call().createHtml5(reportId, new ID[]{recordId}, false);
            } else {
                reportGenerator = EasyExcelGenerator.create(reportId, Collections.singletonList(recordId));
            }

            if (reportGenerator == null) {
                throw new ToolException("当前环境不支持此类型报表的导出");
            }
            reportGenerator.setReportId(reportId);
            output = reportGenerator.generate();
        } catch (ToolException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Report export failed : {} / {}", reportId, recordId, ex);
            throw new ToolException("报表生成失败 : " + CommonsUtils.getRootMessage(ex));
        }

        if (output == null) {
            throw new ToolException("无法输出报表，请检查报表模板是否有误");
        }

        String fileName = DataReportManager.getPrettyReportName(reportId, recordId, output.getName());
        return buildDownloadResult(fileName, output);
    }

    /**
     * 导出 LIST 报表（列表报表），可按条件导出多条记录
     *
     * @param entity
     * @param reportId
     * @param keyword 搜索关键词（可为空，为空则导出全部）
     * @return
     */
    private JSONObject exportListReport(Entity entity, ID reportId, String keyword, JSONArray filter, String equation) {
        // 构建查询数据
        JSONObject queryData = new JSONObject();
        queryData.put("entity", entity.getName());
        queryData.put("pageNo", 1);
        queryData.put("pageSize", MAX_LIST_EXPORT_ROWS);

        JSONArray fields = new JSONArray();
        fields.add(entity.getPrimaryField().getName());
        queryData.put("fields", fields);

        JSONObject filterObj = ToolHelper.buildFilterExpr(entity, null, equation);
        queryData.put("filter", filterObj);

        // 优先使用 filter 条件，其次使用关键词搜索
        if (filter != null && !filter.isEmpty()) {
            filterObj.put("items", filter);
        } else if (StringUtils.isNotBlank(keyword)) {
            if (ID.isId(keyword)) {
                JSONObject item = new JSONObject();
                item.put("op", ParseHelper.IN);
                item.put("field", entity.getPrimaryField().getName());
                item.put("value", keyword);

                JSONArray items = new JSONArray();
                items.add(item);
                filterObj.put("items", items);
            } else {
                Set<String> searchFields = ParseHelper.buildQuickFields(entity, null);
                if (!searchFields.isEmpty()) {
                    JSONArray items = new JSONArray();
                    for (String field : searchFields) {
                        JSONObject item = new JSONObject();
                        item.put("op", ParseHelper.LK);
                        item.put("field", field);
                        item.put("value", keyword);
                        items.add(item);
                    }
                    filterObj.put("items", items);
                    filterObj.put("equation", "OR");
                }
            }
        }

        File output;
        int exportCount;
        try {
            DataExporter exporter = new DataExporter(queryData);
            output = exporter.export(reportId);
            exportCount = exporter.getExportCount();
        } catch (Exception ex) {
            log.error("List report export failed : {} / {}", reportId, keyword, ex);
            throw new ToolException("列表报表生成失败 : " + CommonsUtils.getRootMessage(ex));
        }

        if (output == null) {
            throw new ToolException("无法输出报表，请检查报表模板是否有误");
        }

        String fileName = DataReportManager.getPrettyReportName(reportId, entity.getName(), output.getName());

        JSONObject result = buildDownloadResult(fileName, output);
        result.put("exportCount", exportCount);
        result.put("message", String.format("列表报表 [%s] 已生成，共导出 %d 条记录，请点击下载链接获取文件", fileName, exportCount));
        return result;
    }

    /**
     * 构建下载结果
     */
    private JSONObject buildDownloadResult(String fileName, File output) {
        if (output instanceof ReportsFile) {
            try {
                output = ((ReportsFile) output).toZip(false);
            } catch (IOException e) {
                throw new ToolException("报表打包失败 : " + e.getMessage());
            }
        }

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
