/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/13
 */
public class ReportGenerator {

    private File template;
    private ID record;

    private ID user;

    /**
     * @param reportId
     * @param record
     */
    public ReportGenerator(ID reportId, ID record) {
        this(DataReportManager.instance.getTemplate(MetadataHelper.getEntity(record.getEntityCode()), reportId), record);
    }

    /**
     * @param template
     * @param record
     */
    public ReportGenerator(File template, ID record) {
        this.template = template;
        this.record = record;
    }

    /**
     * @param user
     */
    public void setUser(ID user) {
        this.user = user;
    }

    /**
     * @return file in temp
     */
    public File generate() {
        String excelSuffix = this.template.getName().endsWith(".xlsx") ? ".xlsx" : ".xls";
        File dest = SysConfiguration.getFileOfTemp("REPORT-" + System.currentTimeMillis() + excelSuffix);

        try(InputStream is = new FileInputStream(template)) {
            try (OutputStream os = new FileOutputStream(dest)) {
                Map<String, Object> data = getDataOfRecord();
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
    protected Map<String, Object> getDataOfRecord() {
        Entity entity = MetadataHelper.getEntity(this.record.getEntityCode());
        Set<String> vars = new ExtractTemplateVars(this.template).extract();

        List<String> validFields = new ArrayList<>();
        for (String field : vars) {
            if (MetadataHelper.getLastJoinField(entity, field) != null) {
                validFields.add(field);
            }
        }

        if (validFields.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(validFields, ","), entity.getName(), entity.getPrimaryField().getName());
        Record record = Application.getQueryFactory().createQuery(sql, this.user).setParameter(1, this.record).record();

        Map<String, Object> data = new HashMap<>();
        for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
            String name = iter.next();
            Field field = MetadataHelper.getLastJoinField(entity, name);
            Object fieldValue = FieldValueWrapper.instance.wrapFieldValue(record.getObjectValue(name), field);

            // TODO 不同类型的处理

            data.put(name, fieldValue);
        }
        return data;
    }
}
