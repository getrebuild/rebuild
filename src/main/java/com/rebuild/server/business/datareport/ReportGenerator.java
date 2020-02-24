/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表生成 jxls
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/13
 * @deprecated Use {@link EasyExcelGenerator}
 */
@Deprecated
public class ReportGenerator extends SetUser<ReportGenerator> {

    private File template;
    private ID recordId;

    /**
     * @param template
     * @param recordId
     */
    public ReportGenerator(File template, ID recordId) {
        this.template = template;
        this.recordId = recordId;
    }

    /**
     * @return file in temp
     */
    public File generate() {
        String excelSuffix = this.template.getName().endsWith(".xlsx") ? ".xlsx" : ".xls";
        File dest = SysConfiguration.getFileOfTemp("REPORT-" + System.currentTimeMillis() + excelSuffix);

        try(InputStream is = new FileInputStream(template)) {
            try (OutputStream os = new FileOutputStream(dest)) {
                Map<String, Object> data = getDataContext();
                Context context = new Context(data);

                JxlsHelper.getInstance().processTemplate(is, os, context);
            }
        } catch (IOException ex) {
            throw new RebuildException(ex);
        }
        return dest;
    }

    /**
     * 从模板中读取变量并查询数据
     *
     * @return
     */
    protected Map<String, Object> getDataContext() {
        Entity entity = MetadataHelper.getEntity(this.recordId.getEntityCode());

        TemplateExtractor templateExtractor = new TemplateExtractor(this.template, false);
        final Map<String, String> varsMap = templateExtractor.transformVars(entity);

        final Map<String, Object> data = new HashMap<>();

        List<String> validFields = new ArrayList<>();
        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            if (e.getValue() == null) {
                data.put(e.getKey(), "[无效变量]");
            } else {
                validFields.add(e.getValue());
            }
        }
        if (validFields.isEmpty()) {
            return data;
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(validFields, ","), entity.getName(), entity.getPrimaryField().getName());

        Record record = Application.createQuery(sql, this.getUser())
                .setParameter(1, this.recordId)
                .record();

        Assert.notNull(record, "No record found : " + this.recordId);
        data.putAll(new EasyExcelGenerator().buildData(record, varsMap));
        return data;
    }
}
