/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.rbstore.MetaschemaImporter;
import com.rebuild.core.rbstore.RBStore;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入元数据模型
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@Controller
public class MetaschemaController extends BaseController {

    @RequestMapping("/admin/metadata/imports")
    public void imports(HttpServletRequest request, HttpServletResponse response) {
        final String entityKey = getParameterNotNull(request, "key");

        JSONArray index = (JSONArray) RBStore.fetchMetaschema("index.json");
        String entityFile = null;
        List<String> refFiles = new ArrayList<>();
        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            if (!entityKey.equalsIgnoreCase(item.getString("key"))) {
                continue;
            }

            entityFile = item.getString("file");
            JSONArray refs = item.getJSONArray("refs");
            if (refs != null) {
                for (Object ref : refs) {
                    String refEntity = (String) ref;
                    if (!MetadataHelper.containsEntity(refEntity)) {
                        refFiles.add(foundFile(index, refEntity));
                    }
                }
            }
            break;
        }

        Assert.notNull(entityFile, "No metaschema found : " + entityKey);

        // 先处理引用实体
        // NOTE 失败后无回滚
        for (String file : refFiles) {
            MetaschemaImporter importer = new MetaschemaImporter(file);
            try {
                String hasError = importer.verfiy();
                if (hasError != null) {
                    writeFailure(response, hasError);
                    return;
                }

                TaskExecutors.exec(importer);
            } catch (Exception ex) {
                writeFailure(response, ex.getLocalizedMessage());
                return;
            }
        }

        MetaschemaImporter importer = new MetaschemaImporter(entityFile);
        try {
            String hasError = importer.verfiy();
            if (hasError != null) {
                writeFailure(response, hasError);
                return;
            }

            Object entityName = TaskExecutors.exec(importer);
            writeSuccess(response, entityName);

        } catch (Exception ex) {
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    private String foundFile(JSONArray index, String key) {
        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            if (key.equalsIgnoreCase(item.getString("key"))) {
                return item.getString("file");
            }
        }
        throw new RebuildException("No metaschema found : " + key);
    }
}
