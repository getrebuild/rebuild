/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import lombok.extern.slf4j.Slf4j;

/**
 * 分类数据
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/28
 */
@Slf4j
public class ClassificationManager implements ConfigManager {

    public static final ClassificationManager instance = new ClassificationManager();

    private ClassificationManager() {
    }

    public static final int BAD_CLASSIFICATION = -1;

    /**
     * 获取名称
     *
     * @param itemId
     * @return
     */
    public String getName(ID itemId) {
        String[] ns = getName2(itemId);
        return ns == null ? null : ns[0];
    }

    /**
     * 获取全名称（包括父级，用 . 分割）
     *
     * @param itemId
     * @return
     */
    public String getFullName(ID itemId) {
        String[] ns = getName2(itemId);
        return ns == null ? null : ns[1];
    }

    /**
     * @param itemId
     * @return [名称, 全名称]
     */
    private String[] getName2(ID itemId) {
        final String ckey = "ClassificationNAME-" + itemId;
        String[] cached = (String[]) Application.getCommonsCache().getx(ckey);
        if (cached != null) {
            return cached[0].equals(DELETED_ITEM) ? null : cached;
        }

        Object[] o = Application.createQueryNoFilter(
                "select name,fullName from ClassificationData where itemId = ?")
                .setParameter(1, itemId)
                .unique();
        if (o != null) cached = new String[]{(String) o[0], (String) o[1]};
        // 可能已删除
        if (cached == null) cached = new String[]{DELETED_ITEM, DELETED_ITEM};

        Application.getCommonsCache().putx(ckey, cached);
        return cached[0].equals(DELETED_ITEM) ? null : cached;
    }

    /**
     * 根据名称搜索对应的分类项 ID（后段匹配优先）
     *
     * @param name
     * @param field
     * @return
     */
    public ID findItemByName(String name, Field field) {
        ID dataId = getUseClassification(field, false);
        if (dataId == null) return null;

        int openLevel = getOpenLevel(field);

        // 后匹配
        String ql = String.format(
                "select itemId from ClassificationData where dataId = ? and level = ? and fullName like '%%%s'", name);
        Object[][] found = Application.createQueryNoFilter(ql)
                .setParameter(1, dataId)
                .setParameter(2, openLevel)
                .array();

        if (found.length == 0) {
            return null;
        } else {
            // TODO 找到多个匹配的优选
            return (ID) found[0][0];
        }
    }

    /**
     * 获取开放级别（注意从 0 开始）
     *
     * @param field
     * @return
     */
    public int getOpenLevel(Field field) {
        ID dataId = getUseClassification(field, false);
        if (dataId == null) return BAD_CLASSIFICATION;

        final String ckey = "ClassificationLEVEL-" + dataId;
        Integer cLevel = (Integer) Application.getCommonsCache().getx(ckey);

        if (cLevel == null) {
            Object[] o = Application.createQueryNoFilter(
                    "select openLevel from Classification where dataId = ?")
                    .setParameter(1, dataId)
                    .unique();

            cLevel = o == null ? BAD_CLASSIFICATION : (Integer) o[0];
            Application.getCommonsCache().putx(ckey, cLevel);
        }

        // 字段指定
        String specLevel = EasyMetaFactory.valueOf(field).getExtraAttr(EasyFieldConfigProps.CLASSIFICATION_LEVEL);
        int specLevelAsInt = ObjectUtils.toInt(specLevel, -1);
        if (specLevelAsInt > BAD_CLASSIFICATION && specLevelAsInt <= cLevel) {
            return specLevelAsInt;
        }

        return cLevel;
    }

    /**
     * 获取指定字段所使用的分类
     *
     * @param field
     * @param checkBad
     * @return
     */
    public ID getUseClassification(Field field, boolean checkBad) {
        String classUse = EasyMetaFactory.valueOf(field).getExtraAttr(EasyFieldConfigProps.CLASSIFICATION_USE);
        ID dataId = ID.isId(classUse) ? ID.valueOf(classUse) : null;
        if (dataId == null) return null;

        if (checkBad && getOpenLevel(field) == BAD_CLASSIFICATION) return null;
        else return dataId;
    }

    @Override
    public void clean(Object cid) {
        ID id2 = (ID) cid;
        if (id2.getEntityCode() == EntityHelper.ClassificationData) {
            Application.getCommonsCache().evict("ClassificationNAME-" + cid);
        } else if (id2.getEntityCode() == EntityHelper.Classification) {
            Application.getCommonsCache().evict("ClassificationLEVEL-" + cid);
        }
    }
}
