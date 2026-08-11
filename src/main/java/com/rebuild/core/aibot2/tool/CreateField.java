/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.general.PickListService;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.metadata.impl.Field2Schema;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 新建字段（仅管理员）。不同字段类型有不同配置项
 *
 * @author devezhao
 * @since 2026/8/10
 */
@Slf4j
public class CreateField implements Tool {

    // 允许通过 AI 创建的字段类型
    private static final Set<DisplayType> ALLOWED_TYPES = EnumSet.of(
            DisplayType.TEXT, DisplayType.NTEXT,
            DisplayType.NUMBER, DisplayType.DECIMAL,
            DisplayType.DATE, DisplayType.DATETIME, DisplayType.TIME,
            DisplayType.BOOL, DisplayType.EMAIL, DisplayType.URL, DisplayType.PHONE,
            DisplayType.LOCATION, DisplayType.TAG, DisplayType.SERIES,
            DisplayType.IMAGE, DisplayType.FILE,
            DisplayType.PICKLIST, DisplayType.MULTISELECT, DisplayType.CLASSIFICATION,
            DisplayType.REFERENCE, DisplayType.N2NREFERENCE);

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        final ID user = UserContextHolder.getUser();
        if (!UserHelper.isAdmin(user)) {
            throw new ToolException("仅管理员可新建字段");
        }

        String entityIdent = args.getString("entity");
        if (StringUtils.isBlank(entityIdent)) {
            throw new ToolException("实体 (entity) 不能为空");
        }
        Entity entity = ToolHelper.resolveEntity(entityIdent);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        String fieldLabel = args.getString("fieldLabel");
        if (StringUtils.isBlank(fieldLabel)) {
            throw new ToolException("字段名称 (fieldLabel) 不能为空");
        }

        DisplayType dt = parseDisplayType(args.getString("type"));

        // 类型相关配置
        String refEntity = null;
        JSON extConfig = null;
        JSONArray options = null;

        if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
            // 引用字段必须指定引用实体
            String refIdent = args.getString("refEntity");
            if (StringUtils.isBlank(refIdent)) {
                throw new ToolException("引用字段必须指定引用实体 (refEntity)");
            }
            Entity ref = ToolHelper.resolveEntity(refIdent);
            if (ref == null) {
                throw new ToolException("无效引用实体 : " + refIdent + ToolHelper.suggestEntity(refIdent));
            }
            refEntity = ref.getName();

        } else if (dt == DisplayType.PICKLIST || dt == DisplayType.MULTISELECT) {
            // 下拉列表/多选必须指定选项
            options = args.getJSONArray("options");
            if (options == null || options.isEmpty()) {
                throw new ToolException("下拉列表/多选字段必须指定选项 (options)，如 [\"选项1\", \"选项2\"]");
            }

        } else if (dt == DisplayType.CLASSIFICATION) {
            // 分类字段必须指定分类数据
            ID dataId = resolveClassification(args.getString("classification"));
            extConfig = JSONUtils.toJSONObject(EasyFieldConfigProps.CLASSIFICATION_USE, dataId);

        } else if (dt == DisplayType.SERIES) {
            // 自动编号规则（可选）
            String seriesFormat = args.getString("seriesFormat");
            if (StringUtils.isNotBlank(seriesFormat)) {
                extConfig = JSONUtils.toJSONObject(EasyFieldConfigProps.SERIES_FORMAT, seriesFormat);
            }
        }

        String fieldName = new Field2Schema(user).createField(
                entity, fieldLabel, null, dt, args.getString("comments"), refEntity, extConfig);

        // 下拉列表/多选：字段创建后补充选项
        if (dt == DisplayType.PICKLIST || dt == DisplayType.MULTISELECT) {
            Field newField = MetadataHelper.getEntity(entity.getEntityCode()).getField(fieldName);
            createPickListItems(newField, options);
        }

        log.info("Field created via AI : {}.{}", entity.getName(), fieldName);

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "field", "label", "type", "message"},
                new Object[]{"ok", entity.getName(), fieldName, fieldLabel, dt.name(),
                        String.format("已成功在实体 [%s] 中创建字段 [%s](%s)，可前往管理中心-实体管理配置表单布局",
                                EasyMetaFactory.getLabel(entity), fieldLabel, fieldName)});
    }

    /**
     * 解析字段类型
     *
     * @param typeStr
     * @return
     */
    private DisplayType parseDisplayType(String typeStr) {
        if (StringUtils.isBlank(typeStr)) {
            throw new ToolException("字段类型 (type) 不能为空，可用类型: " + buildAllowedTypesDesc());
        }

        DisplayType dt;
        try {
            dt = DisplayType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ToolException("无效字段类型 : " + typeStr + "，可用类型: " + buildAllowedTypesDesc());
        }

        if (!ALLOWED_TYPES.contains(dt)) {
            throw new ToolException("不支持创建该字段类型 : " + typeStr + "，可用类型: " + buildAllowedTypesDesc());
        }
        return dt;
    }

    /**
     * 可用类型描述，如 TEXT(文本)
     *
     * @return
     */
    private String buildAllowedTypesDesc() {
        List<String> list = new ArrayList<>();
        for (DisplayType dt : ALLOWED_TYPES) {
            list.add(dt.name() + "(" + dt.getDisplayName() + ")");
        }
        return StringUtils.join(list, ", ");
    }

    /**
     * 解析分类数据（支持 ID、名称）
     *
     * @param ident
     * @return
     */
    private ID resolveClassification(String ident) {
        if (StringUtils.isBlank(ident)) {
            throw new ToolException("分类字段必须指定分类数据 (classification)");
        }

        Object[][] array = Application.createQueryNoFilter(
                "select dataId,name from Classification where isDisabled = 'F'").array();

        List<Object[]> fuzzy = new ArrayList<>();
        for (Object[] row : array) {
            ID dataId = (ID) row[0];
            String name = (String) row[1];
            if (dataId.toLiteral().equals(ident) || name.equals(ident)) {
                return dataId;
            }
            if (StringUtils.containsIgnoreCase(name, ident) || StringUtils.containsIgnoreCase(ident, name)) {
                fuzzy.add(row);
            }
        }

        if (fuzzy.size() == 1) return (ID) fuzzy.get(0)[0];

        List<String> names = new ArrayList<>();
        for (Object[] row : fuzzy) names.add((String) row[1]);
        String suggest = names.isEmpty() ? "" : "，可用分类: " + StringUtils.join(names, ", ");
        throw new ToolException("未找到匹配的分类数据 : " + ident + suggest);
    }

    /**
     * 创建下拉列表/多选选项（默认第一个为默认值）
     *
     * @param field
     * @param options
     */
    private void createPickListItems(Field field, JSONArray options) {
        JSONArray showItems = new JSONArray();
        for (Object o : options) {
            String text = o instanceof JSONObject
                    ? ((JSONObject) o).getString("text") : String.valueOf(o);
            if (StringUtils.isBlank(text)) continue;

            JSONObject item = new JSONObject(true);
            item.put("text", text.trim());
            item.put("default", showItems.isEmpty());
            showItems.add(item);
        }

        if (showItems.isEmpty()) {
            throw new ToolException("下拉列表/多选字段必须指定有效选项 (options)");
        }

        JSONObject config = new JSONObject(true);
        config.put("show", showItems);
        Application.getBean(PickListService.class).updateBatch(field, config);
    }
}
