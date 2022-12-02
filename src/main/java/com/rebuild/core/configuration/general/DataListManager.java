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
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 数据列表
 *
 * @author Zixin (RB)
 * @see com.rebuild.core.support.general.DataListBuilder
 * @since 08/30/2018
 */
@Slf4j
public class DataListManager extends BaseLayoutManager {

    public static final DataListManager instance = new DataListManager();

    private DataListManager() {
    }

    /**
     * @param entity
     * @param user
     * @return
     * @see #formatListFields(String, ID, boolean, ConfigBean)
     */
    public JSON getListFields(String entity, ID user) {
        return getListFields(entity, user, Boolean.TRUE);
    }

    /**
     * @param entity
     * @param user
     * @param filter
     * @return
     * @see #formatListFields(String, ID, boolean, ConfigBean)
     */
    public JSON getListFields(String entity, ID user, boolean filter) {
        JSONObject config = (JSONObject) formatListFields(entity, user, filter, getLayoutOfDatalist(user, entity));
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
     * @param filter 过滤无读取权限的字段
     * @param config
     * @return
     */
    public JSON formatListFields(String entity, ID user, boolean filter, ConfigBean config) {
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
                Map<String, Object> formatted = null;
                if (fieldPath.length == 1) {
                    formatted = formatField(lastField);
                } else {

                    // 如果没有引用实体的读权限，则直接过滤掉字段

                    Field parentField = entityMeta.getField(fieldPath[0]);
                    if (!filter) {
                        formatted = formatField(lastField, parentField);
                    } else if (Application.getPrivilegesManager().allowRead(user, lastField.getOwnEntity().getEntityCode())) {
                        formatted = formatField(lastField, parentField);
                    }
                }

                if (formatted != null) {
                    Object width = item.get("width");
                    if (width != null) formatted.put("width", width);
                    Object label2 = item.get("label2");
                    if (label2 != null) formatted.put("label2", label2);

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
        return formatField(field, null);
    }

    /**
     * @param field
     * @param parent
     * @return
     */
    public Map<String, Object> formatField(Field field, Field parent) {
        String parentField = parent == null ? "" : (parent.getName() + ".");
        String parentLabel = parent == null ? "" : (EasyMetaFactory.getLabel(parent) + ".");
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
        ConfigBean e = getLayout(user, entity, TYPE_WCHARTS);
        if (e == null) {
            return null;
        }

        // 补充图表信息
        JSONArray charts = (JSONArray) e.getJSON("config");
        ChartManager.instance.richingCharts(charts, null);
        return e.set("config", charts)
                .set("shareTo", null);
    }

    /**
     * 列表-统计字段
     *
     * @param user
     * @param entity
     * @return
     */
    public ConfigBean getListStats(ID user, String entity) {
        ConfigBean e = getLayout(user, entity, TYPE_LISTSTATS);
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
        ConfigBean e = getLayout(user, entity, TYPE_LISTFILTERPANE);
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
                if (entityMeta.containsField(field) || AdvFilterParser.VF_ACU.equals(field)) {
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
                paneFields.add(s);
            }
        }

        return paneFields;
    }

    /**
     * TODO 自定义字段 MODE2
     *
     * @param entity
     * @return
     */
    public JSON getFieldsLayoutMode2(Entity entity) {
        JSONObject emptyConfig = (JSONObject) formatListFields(entity.getName(), null, true, null);
        JSONArray fields = emptyConfig.getJSONArray("fields");

        if (entity.containsField(EntityHelper.ApprovalState)) {
            fields.add(formatField(entity.getField(EntityHelper.ApprovalState)));
        }

        return emptyConfig;
    }
}
