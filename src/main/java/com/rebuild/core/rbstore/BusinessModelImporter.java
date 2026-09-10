/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.CommonsUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 批量导入业务模块
 *
 * @author devezhao
 * @since 2020/9/29
 * @see MetaschemaExporter
 * @see MetaschemaImporter
 */
@Slf4j
public class BusinessModelImporter extends HeavyTask<Integer> {

    @Setter
    private String[] modelFiles;
    private List<String> createdEntity = new ArrayList<>();

    private JSONArray indexSchemas;
    private JSONObject indexTypes;

    @Override
    protected Integer exec() {
        Assert.notNull(modelFiles, "[modelFiles] cannot be null");

        DynamicMetadataContextHolder.setSkipRefentityCheck();
        DynamicMetadataContextHolder.setSkipLanguageRefresh();
        this.setTotal(modelFiles.length);

        for (String fileUrl : modelFiles) {
            JSONObject data = null;
            try {
                data = CommonsUtils.isExternalUrl(fileUrl)
                        ? (JSONObject) RBStore.fetchRemoteJson(fileUrl)
                        : (JSONObject) RBStore.fetchMetaschema(fileUrl);

                MetaschemaImporter importer = new MetaschemaImporter(data);
                importer.setImportTag(findTypeGroup(data.getString("entity")));
                String created = importer.exec();
                createdEntity.add(created);
                log.info("Entity imported : {}", created);
                this.addSucceeded();

            } catch (Exception ex) {
                log.error("Cannot import entity : {}", (data == null ? "<null>" : data), ex);
                this.errorMessage = ex.getLocalizedMessage();
            }
            this.addCompleted();
        }

        return modelFiles.length;
    }

    /**
     * @return
     */
    public List<String> getCreatedEntity() {
        return createdEntity;
    }

    @Override
    protected void completedAfter() {
        super.completedAfter();

        DynamicMetadataContextHolder.isSkipRefentityCheck(true);
        DynamicMetadataContextHolder.isSkipLanguageRefresh(true);

        MetadataHelper.getMetadataFactory().refresh();
    }

    /**
     * 获取所有依赖实体
     *
     * @param mainKey
     * @return
     */
    public Map<String, String> findRefs(String mainKey) {
        if (indexSchemas == null) {
            JSONObject index = (JSONObject) RBStore.fetchMetaschema(null);
            this.indexSchemas = index.getJSONArray("schemas");
            this.indexTypes = index.getJSONObject("types");
        }

        Set<String> refs = new HashSet<>();
        findRefs(indexSchemas, mainKey, refs);

        Map<String, String> map = new HashMap<>();
        for (String key : refs) {
            map.put(key, findFile(indexSchemas, key));
        }
        return map;
    }

    /**
     * 获取所有依赖实体
     *
     * @param index
     * @param key
     * @param into
     */
    private void findRefs(JSONArray index, String key, Set<String> into) {
        into.add(key);

        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            if (key.equalsIgnoreCase(item.getString("key"))) {
                JSONArray refs = item.getJSONArray("refs");
                if (refs != null) {
                    for (Object refKey : refs) {
                        if (!into.contains(refKey.toString())) {
                            findRefs(index, (String) refKey, into);
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * @param index
     * @param key
     * @return
     */
    private String findFile(JSONArray index, String key) {
        for (Object o : index) {
            JSONObject item = (JSONObject) o;
            if (key.equalsIgnoreCase(item.getString("key"))) {
                return item.getString("file");
            }
        }
        throw new RebuildException("No metaschema found : " + key);
    }

    /**
     * 查找实体在 RB 仓库索引中的分组名
     *
     * @param entityKey
     * @return
     */
    private String findTypeGroup(String entityKey) {
        if (indexTypes == null || entityKey == null) return null;

        List<String> groups = new ArrayList<>();
        for (Map.Entry<String, Object> e : indexTypes.entrySet()) {
            Object v = e.getValue();
            if (v instanceof JSONArray) {
                for (Object o : (JSONArray) v) {
                    if (entityKey.equalsIgnoreCase(String.valueOf(o))) {
                        groups.add(e.getKey());
                        break;
                    }
                }
            }
        }
        return groups.isEmpty() ? null : String.join(",", groups);
    }

    /**
     * @return
     */
    public JSONArray getIndexSchemas() {
        return indexSchemas;
    }
}
