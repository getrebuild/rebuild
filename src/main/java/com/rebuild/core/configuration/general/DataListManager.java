/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_ASIDE_SHOWS;
import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_HIDE_CHARTS;
import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_HIDE_FILTERS;
import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_MODE3_SHOWCATEGORY;
import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_MODE3_SHOWCHARTS;
import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_MODE3_SHOWFILTERS;
import static com.rebuild.core.metadata.impl.EasyEntityConfigProps.ADVLIST_SHOWCATEGORY;

/**
 * 数据列表
 *
 * @author Zixin (RB)
 * @see com.rebuild.core.support.general.DataListBuilder
 * @since 08/30/2018
 */
@Slf4j
public class DataListManager extends BaseLayoutManager {

    // for 引用字段搜索
    public static final String SYS_REFERENCE = "SYS REFERENCE";
    // for 相关项
    public static final String SYS_RELATED = "SYS RELATED";

    public static final DataListManager instance = new DataListManager();

    private DataListManager() {}

    /**
     * @param entity
     * @param user
     * @return
     */
    public JSON getListFields(String entity, ID user) {
        return getListFields(entity, user, Boolean.TRUE, null);
    }

    /**
     * @param entity
     * @param user
     * @param useSysFlag 优先使用系统指定的 `SYS XXX` or ID
     * @return
     */
    public JSON getListFields(String entity, ID user, String useSysFlag) {
        return getListFields(entity, user, Boolean.TRUE, useSysFlag);
    }

    /**
     * @param entity
     * @param user
     * @param filterNoPriviFields 过滤无读取权限的字段
     * @param useSysFlag
     * @return
     * @see #formatListFields(String, ID, boolean, ConfigBean)
     */
    protected JSON getListFields(String entity, ID user, boolean filterNoPriviFields, String useSysFlag) {
        ConfigBean cb = getLayoutOfDatalist(user, entity, useSysFlag);
        JSONObject config = (JSONObject) formatListFields(entity, user, filterNoPriviFields, cb);
        JSONArray fields = config.getJSONArray("fields");

        for (Object o : fields) {
            JSONObject item = (JSONObject) o;
            String label2 = (String) item.remove("label2");
            if (StringUtils.isNotBlank(label2)) item.put("label", label2);
        }
        return config;
    }

    /**
     * @param entity
     * @param user
     * @param filterNoPriviFields
     * @param config
     * @return
     */
    public JSON formatListFields(String entity, ID user, boolean filterNoPriviFields, ConfigBean config) {
        List<Map<String, Object>> columnList = new ArrayList<>();
        Entity entityMeta = MetadataHelper.getEntity(entity);
        Field namedField = entityMeta.getNameField();

        // 默认配置
        if (config == null) {
            columnList.add(formatField(namedField));

            if (!StringUtils.equalsIgnoreCase(namedField.getName(), EntityHelper.CreatedBy)
                    && entityMeta.containsField(EntityHelper.CreatedBy)) {
                columnList.add(formatField(entityMeta.getField(EntityHelper.CreatedBy)));
            }
            if (!StringUtils.equalsIgnoreCase(namedField.getName(), EntityHelper.CreatedOn)
                    && entityMeta.containsField(EntityHelper.CreatedOn)) {
                columnList.add(formatField(entityMeta.getField(EntityHelper.CreatedOn)));
            }

        } else {
            for (Object o : (JSONArray) config.getJSON("config")) {
                JSONObject item = (JSONObject) o;
                String field = item.getString("field");
                Field lastField = MetadataHelper.getLastJoinField(entityMeta, field);
                if (lastField == null) {
                    log.warn("Invalid field : {} in {}", field, entity);
                    continue;
                }

                String[] fieldPath = field.split("\\.");
                Assert.isTrue(fieldPath.length <= 3, "Up to 3 levels of fields allowed");
                Map<String, Object> formatted = null;
                if (fieldPath.length == 1) {
                    formatted = formatField(lastField);
                } else {

                    // 如没有引用实体的读权限，则过滤该字段

                    final boolean isLevel3 = fieldPath.length == 3;
                    Field parentField = entityMeta.getField(fieldPath[0]);
                    Field[] parents;
                    if (isLevel3) {
                        parents = new Field[]{parentField, parentField.getReferenceEntity().getField(fieldPath[1])};
                    } else {
                        parents = new Field[]{parentField};
                    }

                    if (filterNoPriviFields) {
                        boolean lastFieldAllow = Application.getPrivilegesManager().allowRead(user, lastField.getOwnEntity().getEntityCode());
                        if (isLevel3) {
                            if (lastFieldAllow
                                    && Application.getPrivilegesManager().allowRead(user, parents[1].getOwnEntity().getEntityCode())) {
                                formatted = formatField(lastField, parents);
                            }
                        } else if (lastFieldAllow) {
                            formatted = formatField(lastField, parents);
                        }
                    } else {
                        formatted = formatField(lastField, parents);
                    }
                }

                if (formatted != null) {
                    Object width = item.get("width");
                    if (width != null) formatted.put("width", width);
                    Object label2 = item.get("label2");
                    if (label2 != null) formatted.put("label2", label2);
                    Object sort = item.get("sort");
                    if (sort != null) formatted.put("sort", sort);

                    columnList.add(formatted);
                }
            }
        }

        return JSONUtils.toJSONObject(
                new String[]{"entity", "nameField", "fields"},
                new Object[]{entity, namedField.getName(), columnList});
    }

    /**
     * @param field
     * @return
     */
    public Map<String, Object> formatField(Field field) {
        return formatField(field, new Field[0]);
    }

    /**
     * @param field
     * @param parent
     * @return
     */
    public Map<String, Object> formatField(Field field, Field parent) {
        if (parent != null) return formatField(field, new Field[]{parent});
        return formatField(field, new Field[0]);
    }

    /**
     * @param field
     * @param parents
     * @return
     */
    @SuppressWarnings("StringConcatenationInLoop")
    public Map<String, Object> formatField(Field field, Field[] parents) {
        String parentField = "";
        String parentLabel = "";
        if (parents != null) {
            for (Field f : parents) {
                parentField += f.getName() + ".";
                parentLabel += EasyMetaFactory.getLabel(f) + ".";
            }
        }

        EasyField easyField = EasyMetaFactory.valueOf(field);
        return JSONUtils.toJSONObject(
                new String[]{"field", "label", "type"},
                new Object[]{parentField + easyField.getName(), parentLabel + easyField.getLabel(), easyField.getDisplayType(false)});
    }

    /**
     * 获取可用列显示ID
     *
     * @param entity
     * @param user
     * @return
     */
    public ID[] getUsesDataListId(String entity, ID user) {
        Object[][] uses = getUsesConfig(user, entity, TYPE_DATALIST);
        List<ID> array = new ArrayList<>();
        for (Object[] c : uses) {
            array.add((ID) c[0]);
        }
        return array.toArray(new ID[0]);
    }

    // --

    /**
     * 列表-SIDE图表
     *
     * @param user
     * @param entity
     * @return
     */
    public ConfigBean getWidgetCharts(ID user, String entity) {
        ConfigBean e = getLayout(user, entity, TYPE_WCHARTS, null);
        if (e == null) return null;

        // 补充图表信息
        JSONArray charts = (JSONArray) e.getJSON("config");
        ChartManager.instance.richingCharts(charts, null);
        return e.set("config", charts)
                .remove("shareTo");
    }

    /**
     * 列表-统计列
     *
     * @param user
     * @param entity
     * @return
     */
    public ConfigBean getListStats(ID user, String entity) {
        ConfigBean e = getLayout(user, entity, TYPE_LISTSTATS, null);
        if (e == null) return null;
        return e.clone();
    }

    /**
     * 列表-查询面板
     *
     * @param user
     * @param entity
     * @return
     */
    public ConfigBean getListFilterPane(ID user, String entity) {
        ConfigBean e = getLayout(user, entity, TYPE_LISTFILTERPANE, null);
        if (e == null) return null;
        return e.clone();
    }

    /**
     * 列表-查询面板-字段
     *
     * @param user
     * @param entity
     * @return
     */
    public Set<String> getListFilterPaneFields(ID user, String entity) {
        ConfigBean cb = getListFilterPane(user, entity);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        Set<String> paneFields = new LinkedHashSet<>();

        if (cb != null && cb.getJSON("config") != null) {
            JSONObject configJson = (JSONObject) cb.getJSON("config");
            for (Object o : configJson.getJSONArray("items")) {
                JSONObject item = (JSONObject) o;
                String field = item.getString("field");
                if (entityMeta.containsField(field) /* v3.7 || AdvFilterParser.VF_ACU.equals(field)*/) {
                    paneFields.add(field);
                }
            }
        }

        // 使用快速查询字段
        if (paneFields.isEmpty()) {
            Set<String> quickFields = ParseHelper.buildQuickFields(entityMeta, null);
            quickFields.remove(EntityHelper.QuickCode);

            for (String s : quickFields) {
                if (s.startsWith("&")) s = s.substring(1);

                if (entityMeta.containsField(s)) {
                    paneFields.add(s);
                } else {
                    // 不支持二级
                    if (!s.contains(".")) log.warn("No field in filter pane : {}#{}", entity, s);
                }
            }
        }

        return paneFields;
    }

    /**
     * 自定义字段 MODE2
     *
     * @param entity
     * @return
     */
    public JSON getFieldsLayoutMode2(Entity entity) {
        final EasyEntity entityEasy = EasyMetaFactory.valueOf(entity);
        String showFields = entityEasy.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE2_SHOWFIELDS);
        JSONArray showFieldsConf;
        if (JSONUtils.wellFormat(showFields)) {
            showFieldsConf = JSON.parseArray(showFields);
        } else {
            showFieldsConf = JSON.parseArray("[null,null,null,null,null,null]");  // fix:6
        }

        String imgeField0 = showFieldsConf.getString(0);
        if (imgeField0 == null) {
            showFieldsConf.set(0, entity.getPrimaryField().getName());
        }
        String nameField1 = showFieldsConf.getString(1);
        if (nameField1 == null) {
            nameField1 = entity.getNameField().getName();
            showFieldsConf.set(1, nameField1);
        }
        String approvalField2 = showFieldsConf.getString(2);
        if (approvalField2 == null) {
            if (MetadataHelper.hasApprovalField(entity)) {
                showFieldsConf.set(2, EntityHelper.ApprovalState);
            } else {
                showFieldsConf.set(2, entity.getPrimaryField().getName());
            }
        }
        String createdOnField3 = showFieldsConf.getString(3);
        if (createdOnField3 == null) {
            showFieldsConf.set(3, EntityHelper.CreatedOn);
        }
        String createdByField4 = showFieldsConf.getString(4);
        if (createdByField4 == null) {
            showFieldsConf.set(4, EntityHelper.CreatedBy);
        }

        // v4.0-b3
        String tgf = entityEasy.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE2_ENABLETREEGROUPFIELD);
        if (StringUtils.isNotBlank(tgf)) {
            showFieldsConf = new JSONArray();
            showFieldsConf.add(tgf);
            JSONObject res = (JSONObject) formatShowFields(entity, showFieldsConf);
            res.put("treeGroupField", tgf);
            return res;
        }

        return formatShowFields(entity, showFieldsConf);
    }

    /**
     * 自定义字段 MODE3
     *
     * @param entity
     * @return
     */
    public JSON getFieldsLayoutMode3(Entity entity) {
        String showFields = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE3_SHOWFIELDS);
        JSONArray showFieldsConf;
        if (JSONUtils.wellFormat(showFields)) {
            showFieldsConf = JSON.parseArray(showFields);
        } else {
            showFieldsConf = JSON.parseArray("[null, null, null, null, null]");  // fix:5
        }

        String imgField0 = showFieldsConf.getString(0);
        if (imgField0 == null) {
            Field[] x = MetadataSorter.sortFields(entity, DisplayType.IMAGE);
            imgField0 = x.length > 0 ? x[0].getName() : entity.getPrimaryField().getName();
            showFieldsConf.set(0, imgField0);
        }
        String nameField1 = showFieldsConf.getString(1);
        if (nameField1 == null) {
            nameField1 = entity.getNameField().getName();
            showFieldsConf.set(1, nameField1);
        }

        return formatShowFields(entity, showFieldsConf);
    }

    private JSON formatShowFields(Entity entity, JSONArray showFields) {
        JSONObject emptyConfig = (JSONObject) formatListFields(entity.getName(), null, true, null);
        JSONArray fields = emptyConfig.getJSONArray("fields");
        fields.clear();
        for (Object name : showFields) {
            if (name != null) fields.add(formatField(entity.getField((String) name)));
        }
        return emptyConfig;
    }

    /**
     * 获取侧栏显示
     *
     * @param easyEntity
     * @param listMode
     * @return
     */
    public JSONArray getAdvListAsideShows(EasyEntity easyEntity, int listMode) {
        final JSONObject extraAttrs = easyEntity.getExtraAttrs();

        Map<String, Object[]> itemsMap = new HashMap<>();
        if (listMode == 3) {
            if (extraAttrs.getBooleanValue(ADVLIST_MODE3_SHOWFILTERS)) {
                itemsMap.put(ADVLIST_MODE3_SHOWFILTERS, new Object[]{Language.L("常用查询"), 1, "asideFilters"});
            }
            if (extraAttrs.getString(ADVLIST_MODE3_SHOWCATEGORY) != null) {
                itemsMap.put(ADVLIST_MODE3_SHOWCATEGORY, new Object[]{Language.L("分组"), 2, "asideCategory"});
            }
            if (extraAttrs.getBooleanValue(ADVLIST_MODE3_SHOWCHARTS)) {
                itemsMap.put(ADVLIST_MODE3_SHOWCHARTS, new Object[]{Language.L("图表"), 3, "asideCharts"});
            }
        } else {
            if (!extraAttrs.getBooleanValue(ADVLIST_HIDE_FILTERS)) {
                itemsMap.put(ADVLIST_HIDE_FILTERS, new Object[]{Language.L("常用查询"), 1, "asideFilters"});
            }
            if (extraAttrs.getString(ADVLIST_SHOWCATEGORY) != null) {
                itemsMap.put(ADVLIST_SHOWCATEGORY, new Object[]{Language.L("分组"), 2, "asideCategory"});
            }
            if (!extraAttrs.getBooleanValue(ADVLIST_HIDE_CHARTS)) {
                itemsMap.put(ADVLIST_HIDE_CHARTS, new Object[]{Language.L("图表"), 3, "asideCharts"});
            }
        }

        String shows = easyEntity.getExtraAttr(ADVLIST_ASIDE_SHOWS);
        JSONObject showsConf;
        if (JSONUtils.wellFormat(shows)) showsConf = JSON.parseObject(shows);
        else showsConf = new JSONObject();

        for (String name : showsConf.keySet()) {
            JSONObject o = showsConf.getJSONObject(name);
            Object[] item = itemsMap.get(name);
            if (o == null || item == null) continue;

            String label = o.getString("label");
            int order = o.getIntValue("order");
            if (StringUtils.isNotBlank(label)) item[0] = label;
            if (order > 0) item[1] = order;
            itemsMap.put(name, item);
        }

        List<Object[]> items = new ArrayList<>(itemsMap.values());
        // be:4.2.3 用户部门树
        if ("User".equals(easyEntity.getName())) {
            items.add(new Object[]{"部门", 2, "asideCategory"});
        }
        items.sort(Comparator.comparingInt(o -> (int) o[1]));
        return (JSONArray) JSON.toJSON(items);
    }

    /**
     * 自定义字段 MODE4
     *
     * @param entity
     * @return
     */
    public JSON getFieldsLayoutMode4(Entity entity) {
        EasyEntity easyEntity = EasyMetaFactory.valueOf(entity);

        String startField = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE4_FIELDOFSTART);
        if (StringUtils.isBlank(startField)
                || MetadataHelper.getLastJoinField(entity, startField) == null) {
            startField = EntityHelper.CreatedOn;
        }
        String endField = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE4_FIELDOFEND);
        if (StringUtils.isBlank(endField)
                || MetadataHelper.getLastJoinField(entity, endField) == null) {
            endField = null;
        }
        String titleField = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE4_FIELDOFTITLE);
        if (StringUtils.isBlank(titleField)
                || MetadataHelper.getLastJoinField(entity, titleField) == null) {
            titleField = entity.getNameField().getName();
        }
        String colorField = easyEntity.getExtraAttr(EasyEntityConfigProps.ADVLIST_MODE4_FIELDOFCOLOR);
        if (StringUtils.isBlank(colorField)
                || MetadataHelper.getLastJoinField(entity, colorField) == null) {
            colorField = null;
        }

        return JSONUtils.toJSONObject(
                new String[]{"startField", "endField", "titleField", "colorField"},
                new Object[]{startField, endField, titleField, colorField});
    }
}
