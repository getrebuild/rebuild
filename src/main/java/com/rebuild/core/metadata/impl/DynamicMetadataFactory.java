/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.XmlHelper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

import static com.rebuild.core.metadata.MetadataHelper.SPLITER;
import static com.rebuild.core.metadata.MetadataHelper.SPLITER_RE;

/**
 * @author Zixin (RB)
 * @since 08/04/2018
 */
@Slf4j
public class DynamicMetadataFactory extends ConfigurationMetadataFactory {
    private static final long serialVersionUID = -5709281079615412347L;

    public DynamicMetadataFactory(String configLocation, Dialect dialect) {
        super(configLocation, dialect);
    }

    public void refresh() {
        refresh(false);
    }

    @Override
    public synchronized void refresh(boolean initState) {
        super.refresh(initState);

        if (!initState && !DynamicMetadataContextHolder.isSkipLanguageRefresh(false)) {
            Application.getLanguage().refresh();
        }
    }

    @Override
    protected Document readConfiguration(boolean initState) {
        Document config = super.readConfiguration(initState);
        if (!initState) {
            appendConfig4Db(config);
        }
        return config;
    }

    /**
     * 从数据库读取配置
     *
     * @param config
     */
    private void appendConfig4Db(Document config) {
        final Element rootElement = config.getRootElement();

        Object[][] customEntities = Application.createQueryNoFilter(
                "select typeCode,entityName,physicalName,entityLabel,entityId,comments,icon,nameField,masterEntity,extConfig from MetaEntity")
                .array();
        for (Object[] c : customEntities) {
            String name = (String) c[1];
            Element entity = rootElement.addElement("entity");
            entity.addAttribute("type-code", c[0].toString())
                    .addAttribute("name", name)
                    .addAttribute("physical-name", (String) c[2])
                    .addAttribute("description", (String) c[3])
                    .addAttribute("parent", "false")
                    .addAttribute("name-field", StringUtils.defaultIfBlank((String) c[7], EntityHelper.CreatedOn))
                    .addAttribute("main", (String) c[8])
                    .addAttribute("creatable", "true")
                    .addAttribute("updatable", "true")
                    .addAttribute("queryable", "true")
                    .addAttribute("deletable", "true");

            // 实体扩展配置
            JSONObject extraAttrs;
            if (StringUtils.isBlank((String) c[9])) {
                extraAttrs = new JSONObject();
            } else {
                extraAttrs = JSON.parseObject((String) c[9]);
            }

            extraAttrs.put("metaId", c[4]);
            extraAttrs.put("comments", c[5]);
            extraAttrs.put("icon", c[6]);
            entity.addAttribute("extra-attrs", extraAttrs.toJSONString());
        }

        Set<String> cascadingFieldsChild = new HashSet<>();

        Object[][] customFields = Application.createQueryNoFilter(
                "select belongEntity,fieldName,physicalName,fieldLabel,displayType,nullable,creatable,updatable,"
                        + "maxLength,defaultValue,refEntity,cascade,fieldId,comments,extConfig,repeatable,queryable from MetaField")
                .array();
        for (Object[] c : customFields) {
            final String entityName = (String) c[0];
            final String fieldName = (String) c[1];
            Element entityElement = (Element) rootElement.selectSingleNode("entity[@name='" + entityName + "']");
            if (entityElement == null) {
                log.warn("No entity `{}` found for field `{}`", entityName, fieldName);
                continue;
            }

            Element field = entityElement.addElement("field");
            field.addAttribute("name", fieldName)
                    .addAttribute("physical-name", (String) c[2])
                    .addAttribute("description", (String) c[3])
                    .addAttribute("max-length", String.valueOf(c[8]))
                    .addAttribute("default-value", (String) c[9])
                    .addAttribute("nullable", String.valueOf(c[5]))
                    .addAttribute("creatable", String.valueOf(c[6]))
                    .addAttribute("updatable", String.valueOf(c[7]))
                    .addAttribute("repeatable", String.valueOf(c[15]))
                    .addAttribute("queryable", String.valueOf(c[16]));

            DisplayType dt = DisplayType.valueOf((String) c[4]);
            field.addAttribute("type", dt.getFieldType().getName());

            // 针对不同字段的特殊处理

            if (fieldName.equals(EntityHelper.AutoId)) {
                field.addAttribute("auto-value", "true");
            }

            if (dt == DisplayType.DECIMAL) {
                field.addAttribute("decimal-scale", "8");
            }

            if (dt == DisplayType.ANYREFERENCE
                    || dt == DisplayType.N2NREFERENCE
                    || dt == DisplayType.REFERENCE
                    || dt == DisplayType.PICKLIST
                    || dt == DisplayType.CLASSIFICATION) {
                field.addAttribute("ref-entity", (String) c[10])
                        .addAttribute("cascade", (String) c[11]);
            }

            if (dt == DisplayType.ID) {
                field.addAttribute("queryable", "false");
            }
            // bugfix
            else if (dt == DisplayType.BARCODE) {
                field.addAttribute("queryable", "true");
            }

            // 字段扩展配置
            JSONObject extraAttrs;
            if (JSONUtils.wellFormat((String) c[14])) {
                extraAttrs = JSON.parseObject((String) c[14]);
            } else {
                extraAttrs = new JSONObject();
            }

            extraAttrs.put("metaId", c[12]);
            extraAttrs.put("comments", c[13]);
            extraAttrs.put("displayType", dt.name());

            String cascadingField = extraAttrs.getString(EasyFieldConfigProps.REFERENCE_CASCADINGFIELD);
            if (StringUtils.isNotBlank(cascadingField) && dt == DisplayType.REFERENCE) {
                extraAttrs.put("_cascadingFieldParent", cascadingField);
                String[] fs = cascadingField.split(SPLITER_RE);
                cascadingFieldsChild.add(entityName + SPLITER + fs[0] + SPLITER + fieldName + SPLITER + fs[1]);
            }

            field.addAttribute("extra-attrs", extraAttrs.toJSONString());
        }

        // 处理父级级联的父子级关系
        for (String child : cascadingFieldsChild) {
            String[] fs = child.split(SPLITER_RE);
            Element fieldElement = (Element) rootElement.selectSingleNode(
                    String.format("entity[@name='%s']/field[@name='%s']", fs[0], fs[1]));
            if (fieldElement == null) {
                log.warn("No field found: {}.{}", fs[0], fs[1]);
                continue;
            }

            JSONObject extraAttrs = JSON.parseObject(fieldElement.valueOf("@extra-attrs"));
            extraAttrs.put("_cascadingFieldChild", fs[2] + SPLITER + fs[3]);
            fieldElement.addAttribute("extra-attrs", extraAttrs.toJSONString());
        }

        if (log.isDebugEnabled()) XmlHelper.dump(rootElement);
    }

    @Override
    protected Entity getEntityNoLock(String name) {
        try {
            return super.getEntityNoLock(name);
        } catch (MissingMetaExcetion noexist) {
            if (DynamicMetadataContextHolder.isSkipRefentityCheck(false)) {
                return new GhostEntity(name);
            } else {
                throw noexist;
            }
        }
    }
}
