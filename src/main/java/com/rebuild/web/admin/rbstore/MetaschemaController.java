/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.rbstore.BusinessModelImporter;
import com.rebuild.core.rbstore.RBStore;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 导入元数据模型
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@RestController
public class MetaschemaController extends BaseController {

    @RequestMapping("/admin/metadata/imports")
    public RespBody imports(HttpServletRequest request) {
        final String mainKey = getParameterNotNull(request, "key");
        final JSONArray index = (JSONArray) RBStore.fetchMetaschema("index-2.0.json");

        Set<String> refs = new HashSet<>();
        findRefs(index, mainKey, refs);

        List<String> entityFiles = new ArrayList<>();
        for (String refKey : refs) {
            if (!MetadataHelper.containsEntity(refKey)) {
                entityFiles.add(findFile(index, refKey));
            }
        }

        BusinessModelImporter importer = new BusinessModelImporter(entityFiles.toArray(new String[0]));
        try {
            TaskExecutors.run(importer);

            if (importer.getSucceeded() > 0) {
                return RespBody.ok(mainKey);
            } else {
                return RespBody.error();
            }

        } catch (Exception ex) {
            LOG.error("Cannot import entity : " + mainKey, ex);
            return RespBody.error(ex.getLocalizedMessage());
        }
    }

    /**
     * 获取所有依赖实体
     *
     * @param index
     * @param key
     * @param into
     * @return
     */
    protected void findRefs(JSONArray index, String key, Set<String> into) {
        into.add(key);

        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            if (key.equalsIgnoreCase(item.getString("key"))) {
                JSONArray refs = item.getJSONArray("refs");
                if (refs != null) {
                    for (Object refKey : refs) {
                        if (!into.contains(refKey)) {
                            findRefs(index, (String) refKey, into);
                        }
                    }
                }
                break;
            }
        }
    }

    private String findFile(JSONArray index, String key) {
        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            if (key.equalsIgnoreCase(item.getString("key"))) {
                return item.getString("file");
            }
        }
        throw new RebuildException("No metaschema found : " + key);
    }
}
