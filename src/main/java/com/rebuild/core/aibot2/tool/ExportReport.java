/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.MediaValue;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dataimport.DataExporter;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.ReportsFile;
import com.rebuild.core.service.datareport.TemplateFile;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.RbvFunction;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.AppUtils;
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

    private static final int MAX_LIST_EXPORT_ROWS = 1000;

    // 内置数据导出（无报表模板，同系统列表页的数据导出）
    private static final String BUILTIN_DATA_EXPORT_ID = "DATA-EXPORT";

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new KnownToolException("实体名称不能为空");
        }

        Entity entity = ToolHelper.resolveEntity(entityName);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }

        String report = args.getString("reportId");
        String record = args.getString("record");

        if (StringUtils.isBlank(report)) {
            return listReports(entity);
        }

        JSONArray filter = args.getJSONArray("filter");
        String equation = args.getString("equation");

        // 内置数据导出（无报表模板）
        if (BUILTIN_DATA_EXPORT_ID.equalsIgnoreCase(report)) {
            return exportBuiltInList(entity, args.getString("format"), args.getJSONArray("fields"), record, filter, equation);
        }

        if (!ID.isId(report)) {
            throw new KnownToolException("无效的报表模板 ID : " + report + "，请从可用报表列表中选取");
        }

        ID reportId = ID.valueOf(report);
        TemplateFile tt = DataReportManager.instance.buildTemplateFile(reportId, entity);

        // LIST 报表按条件导出多条记录，其他为单记录报表
        if (tt.type == DataReportManager.TYPE_LIST) {
            return exportListReport(entity, reportId, record, filter, equation);
        }

        if (StringUtils.isBlank(record)) {
            throw new KnownToolException("请提供要导出报表的记录名称或编号 (record)");
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

        // 内置数据导出（无需报表模板）
        JSONObject builtin = new JSONObject();
        builtin.put("id", BUILTIN_DATA_EXPORT_ID);
        builtin.put("name", "数据导出");
        builtin.put("type", "LIST");
        builtin.put("builtin", true);
        reports.add(builtin);

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "reports"},
                new Object[]{"ok", entity.getName(), reports});
    }

    private JSONObject searchAndExport(Entity entity, ID reportId, TemplateFile tt, String keyword) {
        Set<String> searchFields = ParseHelper.buildQuickFields(entity, null);
        if (searchFields.isEmpty()) {
            throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 无可搜索字段");
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
                throw new KnownToolException("当前环境不支持此类型报表的导出");
            }
            reportGenerator.setReportId(reportId);
            output = reportGenerator.generate();
        } catch (ToolException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Report export failed : {} / {}", reportId, recordId, ex);
            throw new KnownToolException("报表生成失败 : " + CommonsUtils.getRootMessage(ex));
        }

        if (output == null) {
            throw new KnownToolException("无法输出报表，请检查报表模板是否有误");
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
        JSONObject queryData = buildListQueryData(entity, keyword, filter, equation);

        File output;
        int exportCount;
        try {
            DataExporter exporter = new DataExporter(queryData);
            output = exporter.export(reportId);
            exportCount = exporter.getExportCount();
        } catch (Exception ex) {
            log.error("List report export failed : {} / {}", reportId, keyword, ex);
            throw new KnownToolException("列表报表生成失败 : " + CommonsUtils.getRootMessage(ex));
        }

        if (output == null) {
            throw new KnownToolException("无法输出报表，请检查报表模板是否有误");
        }

        String fileName = DataReportManager.getPrettyReportName(reportId, entity.getName(), output.getName());

        JSONObject result = buildDownloadResult(fileName, output);
        result.put("exportCount", exportCount);
        result.put("message", String.format("列表报表 [%s] 已生成，共导出 %d 条记录，[点击下载](%s)，请将此下载链接展示给用户", fileName, exportCount, result.getString("downloadUrl")));
        return result;
    }

    /**
     * 内置数据导出（无报表模板），同系统列表页的数据导出，支持 CSV/Excel
     *
     * @param entity
     * @param format xls/csv，默认 xls
     * @param exportFields 导出字段（名称或标签），为空则导出全部可导出字段
     * @param keyword 搜索关键词（可为空，为空则导出全部）
     * @return
     */
    private JSONObject exportBuiltInList(Entity entity, String format, JSONArray exportFields, String keyword, JSONArray filter, String equation) {
        final ID user = UserContextHolder.getUser();
        if (!Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport)) {
            throw new KnownToolException("无数据导出权限");
        }

        if ("csv".equalsIgnoreCase(format)) format = "csv";
        else format = "xls";

        JSONObject queryData = buildListQueryData(entity, keyword, filter, equation);
        if (exportFields != null && !exportFields.isEmpty()) {
            // 指定导出字段，名称或标签均可，需可导出
            JSONArray fields = new JSONArray();
            for (Object o : exportFields) {
                Field f = ToolHelper.resolveField(entity, o.toString());
                EasyField ef = EasyMetaFactory.valueOf(f);
                if (!ef.getDisplayType().isExportable() || ef instanceof MediaValue) {
                    throw new KnownToolException("字段不可导出 : " + o);
                }
                fields.add(f.getName());
            }
            queryData.put("fields", fields);
        } else {
            // 空 fields 会填充全部可导出字段，同系统数据导出的"全部列"
            queryData.put("fields", new JSONArray());
        }
        queryData = new BatchOperatorQuery(BatchOperatorQuery.DR_QUERYED, queryData)
                .wrapQueryData(MAX_LIST_EXPORT_ROWS, false);

        File output;
        int exportCount;
        try {
            DataExporter exporter = (DataExporter) new DataExporter(queryData).setUser(user);
            output = exporter.export(format);
            exportCount = exporter.getExportCount();
        } catch (Exception ex) {
            log.error("Built-in data export failed : {} / {}", entity.getName(), keyword, ex);
            throw new KnownToolException("数据导出失败 : " + CommonsUtils.getRootMessage(ex));
        }

        if (output == null) {
            throw new KnownToolException("无法输出文件");
        }

        String fileName = String.format("%s-%s.%s",
                EasyMetaFactory.getLabel(entity),
                CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()),
                FileUtil.getSuffix(output));

        JSONObject result = buildDownloadResult(fileName, output);
        result.put("exportCount", exportCount);
        result.put("message", String.format("[%s] 数据导出已生成，共导出 %d 条记录，[点击下载](%s)，请将此下载链接展示给用户", EasyMetaFactory.getLabel(entity), exportCount, result.getString("downloadUrl")));
        return result;
    }

    /**
     * 构建列表查询数据（含过滤条件：优先 filter 参数，其次关键词/记录 ID）
     *
     * @param entity
     * @param keyword
     * @param filter
     * @param equation
     * @return
     */
    private JSONObject buildListQueryData(Entity entity, String keyword, JSONArray filter, String equation) {
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
            ToolHelper.validateFilter(entity, filterObj);  // 校验模型传入的字段名是否真实存在
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
        return queryData;
    }

    /**
     * 构建下载结果
     */
    private JSONObject buildDownloadResult(String fileName, File output) {
        if (output instanceof ReportsFile) {
            try {
                output = ((ReportsFile) output).toZip(false);
            } catch (IOException e) {
                throw new KnownToolException("报表打包失败 : " + e.getMessage());
            }
        }

        String fileUrl = String.format("/filex/download/%s?temp=yes&_csrfToken=%s&attname=%s",
                CodecUtils.urlEncode(output.getName()),
                AuthTokenManager.generateCsrfToken(90),
                CodecUtils.urlEncode(fileName));
        fileUrl = AppUtils.getContextPath(fileUrl);

        JSONObject result = new JSONObject();
        result.put("status", "ok");
        result.put("fileName", fileName);
        result.put("downloadUrl", fileUrl);
        result.put("message", String.format("报表 [%s] 已生成，[点击下载](%s)，请将此下载链接展示给用户", fileName, fileUrl));
        return result;
    }
}
