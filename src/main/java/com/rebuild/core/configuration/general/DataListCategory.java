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
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CacheTemplate;
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
     * NOTE 有 5m 缓存
     *
     * @param entity
     * @return
     */
    public JSON datas(Entity entity) {
        final Field categoryField = getFieldOfCategory(entity);
        if (categoryField == null) return null;

        String conf = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_SHOWCATEGORY);
        String[] ff = conf.split(":");
        String ffField = ff[0];
        String ffFormat = ff.length > 1 ? ff[1] : null;

        final String ckey = "DataListCategory-" + conf;
        Object cached = Application.getCommonsCache().getx(ckey);
        if (Application.devMode()) cached = null;
        if (cached != null) return (JSON) cached;

        EasyField easyField = EasyMetaFactory.valueOf(categoryField);
        DisplayType dt = easyField.getDisplayType();
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
            // 分类
            dataList = datasClassification(categoryField, ffFormat);

        } else if (dt == DisplayType.REFERENCE && ffFormat != null && categoryField.getReferenceEntity().containsField(ffFormat)) {
            // 引用-父级
            dataList = datasReference(categoryField, ffFormat);

        } else {

            // 动态获取

            String sql;
            if (dt == DisplayType.N2NREFERENCE) {
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

            // FIXME 无权限查询，以便使用缓存
            Object[][] array = Application.createQueryNoFilter(sql).array();

            for (Object[] o : array) {
                Object id = o[0];
                String label;
                if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
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
            // 日期倒序
            if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                dataListSorted.sort((o1, o2) -> o2.text.compareTo(o1.text));
            } else {
                dataListSorted.sort(Comparator.comparing(o -> o.text));
            }
            dataList = dataListSorted;
        }

        JSONArray res = new JSONArray();
        for (Item i : dataList) res.add(i.toJSON());

        Application.getCommonsCache().putx(ckey, res, CacheTemplate.TS_MINTE * 5);
        return res;
    }

    /**
     * Max. 4L
     * @param field
     * @param format
     * @return
     */
    protected Collection<Item> datasClassification(Field field, String format) {
        final ID classid = ClassificationManager.instance.getUseClassification(field, false);
        int level = ClassificationManager.instance.getOpenLevel(field);
        int levelSpec = format == null ? level : ObjectUtils.toInt(format);
        if (levelSpec < level) level = levelSpec;

        Collection<Item> dataList = new LinkedHashSet<>();

        // L0-1
        Object[][] level0 = getClassificationItems(classid, null);
        for (Object[] L0 : level0) {
            Item item0 = new Item(L0);

            // L1-2
            if (level > 0) {
                Object[][] level1 = getClassificationItems(classid, (ID) item0.id);
                for (Object[] L1 : level1) {
                    Item item1 = item0.addChild(L1);

                    // L2-3
                    if (level > 1) {
                        Object[][] level2 = getClassificationItems(classid, (ID) item1.id);
                        for (Object[] L2 : level2) {
                            Item item2 = item1.addChild(L2);

                            // L3-4
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
        return dataList;
    }

    // 分类子级
    private Object[][] getClassificationItems(ID classid, ID parent) {
        String sql = "select itemId,name from ClassificationData where dataId = ? and parent";
        if (parent != null) sql += " = '" + parent + "'";
        else sql += " is null";

        sql += " order by code,fullName";
        return Application.createQueryNoFilter(sql).setParameter(1, classid).array();
    }

    /**
     * Max. 9L
     * @param field
     * @param format
     * @return
     */
    protected Collection<Item> datasReference(Field field, String format) {
        final Field parentField = field.getReferenceEntity().getField(format);

        Collection<Item> dataList = new LinkedHashSet<>();

        // L0-1
        Object[][] level0 = getReferenceItems(parentField, null);
        for (Object[] L0 : level0) {
            Item item0 = new Item(L0[0], null);

            // L1-2
            Object[][] level1 = getReferenceItems(parentField, (ID) L0[0]);
            for (Object[] L1 : level1) {
                Item item1 = item0.addChild(L1[0], null);

                // L2-3
                Object[][] level2 = getReferenceItems(parentField, (ID) L1[0]);
                for (Object[] L2 : level2) {
                    Item item2 = item1.addChild(L2[0], null);

                    // L3-4
                    Object[][] level3 = getReferenceItems(parentField, (ID) L2[0]);
                    for (Object[] L3 : level3) {
                        Item item3 = item2.addChild(L3[0], null);

                        // L4-5
                        Object[][] level4 = getReferenceItems(parentField, (ID) L3[0]);
                        for (Object[] L4 : level4) {
                            Item item4 = item3.addChild(L4[0], null);

                            // L5-6
                            Object[][] level5 = getReferenceItems(parentField, (ID) L4[0]);
                            for (Object[] L5 : level5) {
                                Item item5 = item4.addChild(L5[0], null);

                                // L6-7
                                Object[][] level6 = getReferenceItems(parentField, (ID) L5[0]);
                                for (Object[] L6 : level6) {
                                    Item item6 = item5.addChild(L6[0], null);

                                    // L7-8
                                    Object[][] level7 = getReferenceItems(parentField, (ID) L6[0]);
                                    for (Object[] L7 : level7) {
                                        Item item7 = item6.addChild(L7[0], null);

                                        // L8-9
                                        Object[][] level8 = getReferenceItems(parentField, (ID) L7[0]);
                                        for (Object[] L8 : level8) {
                                            item7.addChild(L8[0], null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            dataList.add(item0);
        }
        return dataList;
    }

    // 引用的子级
    private Object[][] getReferenceItems(Field parentField, ID parent) {
        String sql = MessageFormat.format(
                "select {0}Id from {0} where {1}",
                parentField.getOwnEntity().getName(), parentField.getName());
        if (parent == null) sql += " is null";
        else sql += " = '" + parent + "'";

        return Application.createQueryNoFilter(sql).array();
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

    // Bean
    @EqualsAndHashCode(of = {"id"})
    protected static class Item implements Serializable {
        private static final long serialVersionUID = 6317330509242709409L;

        Object id;
        String text;
        List<Item> children;

        Item(Object id, String text) {
            this.id = id;
            this.text = text;
            // 补充名称
            if (text == null && id instanceof ID) {
                this.text = FieldValueHelper.getLabelNotry((ID) id);
            }
        }

        Item(Object[] o) {
            this(o[0], (String) o[1]);
        }

        Item addChild(Object[] o) {
            return this.addChild(o[0], (String) o[1]);
        }

        Item addChild(Object id, String text) {
            Item c = new Item(id, text);
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
