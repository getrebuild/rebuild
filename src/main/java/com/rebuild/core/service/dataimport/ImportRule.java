/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.RebuildConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 导入规则
 *
 * @author devezhao
 * @since 01/10/2019
 */
public class ImportRule {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRule.class);

    public static final int REPEAT_OPT_UPDATE = 1;
    public static final int REPEAT_OPT_SKIP = 2;
    public static final int REPEAT_OPT_IGNORE = 3;

    private File sourceFile;
    private Entity toEntity;

    private int repeatOpt;
    private Field[] repeatFields;

    private ID defaultOwningUser;

    private Map<Field, Integer> filedsMapping;

    /**
     * @param sourceFile
     * @param toEntity
     * @param repeatOpt
     * @param repeatFields
     * @param defaultOwningUser
     * @param filedsMapping
     */
    protected ImportRule(File sourceFile, Entity toEntity, int repeatOpt, Field[] repeatFields, ID defaultOwningUser,
                         Map<Field, Integer> filedsMapping) {
        this.sourceFile = sourceFile;
        this.toEntity = toEntity;
        this.repeatOpt = repeatOpt;
        this.repeatFields = repeatFields;
        this.defaultOwningUser = defaultOwningUser;
        this.filedsMapping = filedsMapping;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public Entity getToEntity() {
        return toEntity;
    }

    public int getRepeatOpt() {
        return repeatOpt;
    }

    public Field[] getRepeatFields() {
        return repeatFields;
    }

    public ID getDefaultOwningUser() {
        return defaultOwningUser;
    }

    public Map<Field, Integer> getFiledsMapping() {
        return filedsMapping;
    }

    // --

    /**
     * 解析导入规则
     *
     * @param rule
     * @return
     * @throws IllegalArgumentException
     */
    public static ImportRule parse(JSONObject rule) throws IllegalArgumentException {
        Assert.notNull(rule.getString("entity"), "Node `entity` cannot be null");
        Assert.notNull(rule.getString("file"), "Node `file` cannot be null");
        Assert.notNull(rule.getInteger("repeat_opt"), "Node `repeat_opt` cannot be null");
        Assert.notNull(rule.getJSONObject("fields_mapping"), "Node `fields_mapping` cannot be null");

        Entity entity = MetadataHelper.getEntity(rule.getString("entity"));
        File file = RebuildConfiguration.getFileOfTemp(rule.getString("file"));

        // from TestCase
        if (!file.exists()) {
            URL testFile = ImportRule.class.getClassLoader().getResource(rule.getString("file"));
            if (testFile != null) {
                try {
                    file = new File(testFile.toURI());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("File not found : " + file, e);
                }
            }
            LOG.warn("Use file from TestCase : " + file);
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found : " + file);
        }

        int repeatOpt = rule.getIntValue("repeat_opt");
        Field[] repeatFields = null;
        if (repeatOpt != 3) {
            Assert.notNull(rule.getJSONArray("repeat_fields"), "Node `repeat_fields`");
            Set<Field> rfs = new HashSet<>();
            for (Object field : rule.getJSONArray("repeat_fields")) {
                rfs.add(entity.getField((String) field));
            }
            Assert.isTrue(!rfs.isEmpty(), "Node `repeat_fields`");
            repeatFields = rfs.toArray(new Field[0]);
        }

        String user = rule.getString("owning_user");
        ID ownUser = ID.isId(user) ? ID.valueOf(user) : null;

        JSONObject fm = rule.getJSONObject("fields_mapping");
        Map<Field, Integer> filedsMapping = new HashMap<>();
        for (Map.Entry<String, Object> e : fm.entrySet()) {
            filedsMapping.put(entity.getField(e.getKey()), (Integer) e.getValue());
        }

        return new ImportRule(file, entity, repeatOpt, repeatFields, ownUser, filedsMapping);
    }
}
