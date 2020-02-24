/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.excel.EasyExcel;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.MetadataHelper;

import java.io.File;
import java.util.Map;

/**
 * 报表生成 easyexcel
 * https://alibaba-easyexcel.github.io/quickstart/fill.html
 *
 * @author devezhao
 * @since 2020/2/24
 */
public class EasyExcelGenerator extends SetUser<EasyExcelGenerator> {

    private File template;
    private ID record;

    /**
     * @param reportId
     * @param record
     */
    public EasyExcelGenerator(ID reportId, ID record) {
        this(DataReportManager.instance.getTemplateFile(MetadataHelper.getEntity(record.getEntityCode()), reportId), record);
    }

    /**
     * @param template
     * @param record
     */
    public EasyExcelGenerator(File template, ID record) {
        this.template = template;
        this.record = record;
    }

    /**
     * @return
     */
    public File generate() {
        String excelSuffix = this.template.getName().endsWith(".xlsx") ? ".xlsx" : ".xls";
        File dest = SysConfiguration.getFileOfTemp("REPORT-" + System.currentTimeMillis() + excelSuffix);

        Map<String, Object> data = getDataContext();
        EasyExcel.write(dest).withTemplate(template).sheet().doFill(data);
        return dest;
    }

    /**
     * @return
     */
    protected Map<String, Object> getDataContext() {
        return null;
    }
}
