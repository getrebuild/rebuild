/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 列表字段分类（分组）数据
 *
 * @author ZHAO
 * @since 07/23/2022
 */
public class DataListCategory {

    public static final DataListCategory instance = new DataListCategory();

    private DataListCategory() {}

    /**
     * FIXME 存在性能问题
     *
     * @param entity
     * @param user
     * @return
     */
    public JSON datas(Entity entity, ID user) {
        final Field categoryField = getFieldOfCategory(entity);
        if (categoryField == null) return null;

        EasyField easyField = EasyMetaFactory.valueOf(categoryField);
        DisplayType dt = easyField.getDisplayType();

        String conf = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
        String[] ff = conf.split(":");
        String ffField = ff[0];
        String ffFormat = ff.length > 1 ? ff[1] : null;

        // Set, Sorted
        Collection<Item> dataList = new LinkedHashSet<>();

        // 使用全部
        if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST) {
            ConfigBean[] cbs = MultiSelectManager.instance.getPickListRaw(categoryField, true);
            for (ConfigBean cb : cbs) {
                Object id = cb.getID("id");
                if (dt == DisplayType.MULTISELECT) id = cb.getLong("mask");
                dataList.add(new Item(id, cb.getString("text")));
            }

        } else if (dt == DisplayType.CLASSIFICATION) {
            // 前端使用树桩组件
            ID classid = ClassificationManager.instance.getUseClassification(categoryField, false);
            int level = ClassificationManager.instance.getOpenLevel(categoryField);
            int levelSpec = ffFormat == null ? level : ObjectUtils.toInt(ffFormat);
            if (levelSpec < level) level = levelSpec;

            // L0
            Object[][] level0 = getClassificationItems(classid, null);
            for (Object[] L0 : level0) {
                Item item0 = new Item(L0);
                // L1
                if (level > 0) {
                    Object[][] level1 = getClassificationItems(classid, (ID) item0.id);
                    for (Object[] L1 : level1) {
                        Item item1 = item0.addChild(L1);
                        // L2
                        if (level > 1) {
                            Object[][] level2 = getClassificationItems(classid, (ID) item1.id);
                            for (Object[] L2 : level2) {
                                Item item2 = item1.addChild(L2);
                                // L3
                                if (level > 2) {
                                    Object[][] level3 = getClassificationItems(classid, (ID) item2.id);
                                    for (Object[] L3 : level3) {
                                        item2.addChild(L3);
                                    }
                                }
                            }
                        }
                    }
                }
                dataList.add(item0);
            }

        } else {

            // 动态获取

            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
                // FIXME 无权限查询
                sql = MessageFormat.format(
                        "select distinct referenceId from NreferenceItem where belongEntity = ''{0}'' and belongField = ''{1}''",
                        entity.getName(), ffField);
            } else {
                String wrapField = ffField;
                if (dt == DisplayType.DATETIME) {
                    wrapField = String.format("DATE_FORMAT(%s, '%%Y-%%m-%%d')", wrapField);
                }

                sql = MessageFormat.format(
                        "select {0} from {1} where {2} is not null group by {0}",
                        wrapField, entity.getName(), categoryField.getName());
            }

            final boolean isDate = dt == DisplayType.DATE || dt == DisplayType.DATETIME;

            Query query = user == null
                    ? Application.createQueryNoFilter(sql)
                    : Application.getQueryFactory().createQuery(sql, user);
            Object[][] array = query.array();

            for (Object[] o : array) {
                Object id = o[0];
                String label;
                if (isDate) {
                    ffFormat = StringUtils.defaultIfBlank(ffFormat, CalendarUtils.UTC_DATE_FORMAT);
                    if (id instanceof Date) {
                        label = CalendarUtils.format(ffFormat, (Date) id);
                    } else {
                        label = id.toString().substring(0, ffFormat.length());
                    }
                    id = label;
                } else {
                    // REF
                    label = FieldValueHelper.getLabelNotry((ID) id);
                }

                dataList.add(new Item(id, label));
            }

            // 排序
            List<Item> dataListSorted = new ArrayList<>(dataList);
            if (isDate) {
                // 日期倒序
                dataListSorted.sort((o1, o2) -> o2.text.compareTo(o1.text));
            } else {
                dataListSorted.sort(Comparator.comparing(o -> o.text));
            }
            dataList = dataListSorted;
        }

        JSONArray res = new JSONArray();
        for (Item i : dataList) res.add(i.toJSON());
        return res;
    }

    /**
     * 获取分类字段（若有）
     *
     * @param entity
     * @return
     */
    public Field getFieldOfCategory(Entity entity) {
        String categoryField = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
        if (categoryField != null) categoryField = categoryField.split(":")[0];
        if (categoryField != null && entity.containsField(categoryField)) return entity.getField(categoryField);
        return null;
    }

    /**
     * @param classid
     * @param parent
     * @return
     */
    private Object[][] getClassificationItems(ID classid, ID parent) {
        String sql = "select itemId,name from ClassificationData where dataId = ? and parent";
        if (parent != null) sql += " = '" + parent + "'";
        else sql += " is null";

        sql += " order by code,fullName";
        return Application.createQueryNoFilter(sql).setParameter(1, classid).array();
    }

    // Bean
    @EqualsAndHashCode(of = {"id"})
    static class Item implements Serializable {
        private static final long serialVersionUID = 6317330509242709409L;

        Object id;
        String text;
        List<Item> children;

        Item(Object id, String text) {
            this.id = id;
            this.text = text;
        }

        Item(Object[] o) {
            this(o[0], (String) o[1]);
        }

        Item addChild(Object[] o) {
            Item c = new Item(o);
            if (children == null) children = new ArrayList<>();
            children.add(c);
            return c;
        }

        JSONObject toJSON() {
            JSONObject item = JSONUtils.toJSONObject(new String[] { "text", "id" }, new Object[] { text, id } );
            if (children == null || children.isEmpty()) return item;

            JSONArray cs = new JSONArray();
            for (Item c : children) cs.add(c.toJSON());
            item.put("children", cs);
            return item;
        }
    }
}
