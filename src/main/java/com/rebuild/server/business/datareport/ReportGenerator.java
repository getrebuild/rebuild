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
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 报表生成
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
        this(DataReportManager.instance.getTemplateFile(MetadataHelper.getEntity(record.getEntityCode()), reportId), record);
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
        Entity entity = MetadataHelper.getEntity(this.record.getEntityCode());

        TemplateExtractor templateExtractor = new TemplateExtractor(this.template);
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

        Query query = this.user == null ?  Application.createQuery(sql)
                : Application.getQueryFactory().createQuery(sql, this.user);
        Record record = query.setParameter(1, this.record).record();

        for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
            String name = iter.next();
            Field field = MetadataHelper.getLastJoinField(entity, name);
            EasyMeta easyMeta = EasyMeta.valueOf(field);
            DisplayType dt = easyMeta.getDisplayType();
            if (dt == DisplayType.IMAGE || dt == DisplayType.AVATAR
    				|| dt == DisplayType.FILE || dt == DisplayType.LOCATION) {
            	data.put(name, "[暂不支持" + dt.getDisplayName() + "]");
            	continue;
    		}

            String varName = name;
            for (Map.Entry<String, String> e : varsMap.entrySet()) {
                if (name.equalsIgnoreCase(e.getValue())) {
                    varName = e.getKey();
                    break;
                }
            }

            Object fieldValue = record.getObjectValue(name);
            if (fieldValue == null) {
                data.put(varName, StringUtils.EMPTY);
                continue;
            }

            if (easyMeta.getDisplayType() == DisplayType.REFERENCE && fieldValue instanceof ID) {
                fieldValue = FieldValueWrapper.getLabelNotry((ID) fieldValue);
            } else {
                fieldValue = FieldValueWrapper.instance.wrapFieldValue(fieldValue, easyMeta);
            }
            data.put(varName, fieldValue.toString());
        }
        return data;
    }
}
