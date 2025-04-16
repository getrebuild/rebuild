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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

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
        Item item = getItem(itemId);
        return item == null ? null : item.Name;
    }

    /**
     * 获取全名称（包括父级）
     *
     * @param itemId
     * @return
     */
    public String getFullName(ID itemId) {
        Item item = getItem(itemId);
        return item == null ? null : item.FullName;
    }

    /**
     * @param itemId
     * @return
     */
    public Item getItem(ID itemId) {
        final String ckey = "ClassificationITEM40-" + itemId;
        Item ditem = (Item) Application.getCommonsCache().getx(ckey);
        if (ditem != null) {
            return DELETED_ITEM.equals(ditem.Name) ? null : ditem;
        }

        Object[] o = Application.createQueryNoFilter(
                "select name,fullName,code,color,isHide from ClassificationData where itemId = ?")
                .setParameter(1, itemId)
                .unique();
        if (o != null) ditem = new Item((String) o[0], (String) o[1], (String) o[2], (String) o[3], ObjectUtils.toBool(o[4]));
        // 可能已删除
        if (ditem == null) ditem = new Item(DELETED_ITEM, null, null, null, true);

        Application.getCommonsCache().putx(ckey, ditem);
        return DELETED_ITEM.equals(ditem.Name) ? null : ditem;
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
            Application.getCommonsCache().evict("ClassificationITEM38-" + cid);
        } else if (id2.getEntityCode() == EntityHelper.Classification) {
            Application.getCommonsCache().evict("ClassificationLEVEL-" + cid);
        }
    }

    // Bean
    @Getter
    public static class Item implements Serializable {
        private static final long serialVersionUID = -1903227875771376652L;
        Item(String name, String fullName, String code, String color, boolean isHide) {
            this.Name = name;
            this.FullName = fullName;
            this.Code = code;
            this.Color = color;
            this.Hide = isHide;
        }

        final String Name;
        final String FullName;
        final String Code;
        final String Color;
        final boolean Hide;
    }
}
