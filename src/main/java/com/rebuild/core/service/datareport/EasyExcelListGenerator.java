/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.general.QueryParser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.rebuild.core.service.datareport.TemplateExtractor.*;

/**
 * Excel 列表导出
 *
 * @author devezhao
 * @since 2021/8/25
 */
@Slf4j
public class EasyExcelListGenerator extends EasyExcelGenerator {

    private JSONObject queryData;

    public EasyExcelListGenerator(ID reportId, JSONObject queryData) {
        this(DataReportManager.instance.getTemplateFile(
                MetadataHelper.getEntity(queryData.getString("entity")), reportId), queryData);
    }

    public EasyExcelListGenerator(File template, JSONObject queryData) {
        super(template, null);
        this.queryData = queryData;
    }

    /**
     * @see com.rebuild.core.service.dataimport.DataExporter
     * @see DataListBuilderImpl#getJSONResult()
     */
    @Override
    protected List<Map<String, Object>> buildData() {
        Entity entity = MetadataHelper.getEntity(queryData.getString("entity"));
        TemplateExtractor templateExtractor = new TemplateExtractor(this.template, true, false);
        Map<String, String> varsMap = templateExtractor.transformVars(entity);

        List<String> validFields = new ArrayList<>();

        for (Map.Entry<String, String> e : varsMap.entrySet()) {
            String varName = e.getKey();
            if (varName.startsWith(NROW_PREFIX + PLACEHOLDER)) {
                continue;
            }
            
            String validField = e.getValue();
            if (validField != null && e.getKey().startsWith(NROW_PREFIX)) {
                validFields.add(validField);
            } else {
                log.warn("Invalid field `{}` in template : {}", e.getKey(), this.template);
            }
        }

        if (validFields.isEmpty()) return Collections.emptyList();

        queryData.put("fields", validFields);  // 使用模板字段

        QueryParser queryParser = new QueryParser(queryData);
        int[] limits = queryParser.getSqlLimit();
        List<Record> list = Application.createQuery(queryParser.toSql(), getUser())
                .setLimit(limits[0], limits[1])
                .list();

        List<Map<String, Object>> datas = new ArrayList<>();

        phNumber = 1;
        for (Record c : list) {
            datas.add(buildData(c, varsMap));
            phNumber++;
        }
        return datas;
    }

    public int getExportCount() {
        return phNumber - 1;
    }
}
