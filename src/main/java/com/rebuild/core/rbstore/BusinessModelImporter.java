/*
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 批量导入业务模块
 *
 * @author devezhao
 * @since 2020/9/29
 */
public class BusinessModelImporter extends HeavyTask<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessModelImporter.class);

    private String[] modelFiles;

    private List<String> createdEntity = new ArrayList<>();

    public BusinessModelImporter() {
    }

    public BusinessModelImporter(String[] modelFiles) {
        this.modelFiles = modelFiles;
    }

    public void setModelFiles(String[] modelFiles) {
        this.modelFiles = modelFiles;
    }

    @Override
    protected Integer exec() {
        Assert.notNull(modelFiles, "[modelFiles] cannot be null");

        DynamicMetadataContextHolder.setSkipRefentityCheck();
        DynamicMetadataContextHolder.setSkipLanguageRefresh();
        this.setTotal(modelFiles.length);

        for (String fileUrl : modelFiles) {
            JSONObject data;
            if (fileUrl.startsWith("http")) {
                data = (JSONObject) RBStore.fetchRemoteJson(fileUrl);
            } else {
                data = (JSONObject) RBStore.fetchMetaschema(fileUrl);
            }

            String created = new MetaschemaImporter(data).exec();
            createdEntity.add(created);
            LOG.info("Entity created : " + created);
            this.addCompleted();
            this.addSucceeded();
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

        MetadataHelper.getMetadataFactory().refresh(false);
    }

    /**
     * 获取所有依赖实体
     *
     * @param mainKey
     * @return
     */
    public Map<String, String> findRefs(String mainKey) {
        JSONArray index = (JSONArray) RBStore.fetchMetaschema("index-2.0.json");

        Set<String> refs = new HashSet<>();
        findRefs(index, mainKey, refs);

        Map<String, String> map = new HashMap<>();
        for (String key : refs) {
            map.put(key, findFile(index, key));
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
                        if (!into.contains(refKey)) {
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
}
