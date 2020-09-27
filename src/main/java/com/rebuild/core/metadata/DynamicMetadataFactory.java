/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.XmlHelper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.impl.DisplayType;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class DynamicMetadataFactory extends ConfigurationMetadataFactory {
    private static final long serialVersionUID = -5709281079615412347L;

    private static final Logger LOG = LoggerFactory.getLogger(DynamicMetadataFactory.class);

    public DynamicMetadataFactory(String configLocation, Dialect dialect) {
        super(configLocation, dialect);
    }

    @Override
    public synchronized void refresh(boolean initState) {
        super.refresh(initState);
        if (!initState && Application.isReady()) Application.getLanguage().refresh(false);
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
                    .addAttribute("name-field", (String) c[7])
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

        Object[][] customFields = Application.createQueryNoFilter(
                "select belongEntity,fieldName,physicalName,fieldLabel,displayType,nullable,creatable,updatable,"
                        + "maxLength,defaultValue,refEntity,cascade,fieldId,comments,extConfig,repeatable,queryable from MetaField")
                .array();
        for (Object[] c : customFields) {
            String entityName = (String) c[0];
            String fieldName = (String) c[1];
            Element entityElement = (Element) rootElement.selectSingleNode("entity[@name='" + entityName + "']");
            if (entityElement == null) {
                LOG.warn("No entity found : " + entityName + "." + fieldName);
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
            if (dt == DisplayType.ANYREFERENCE || dt == DisplayType.REFERENCE
                    || dt == DisplayType.PICKLIST || dt == DisplayType.CLASSIFICATION) {
                field.addAttribute("ref-entity", (String) c[10])
                        .addAttribute("cascade", (String) c[11]);
            }
            if (dt == DisplayType.BARCODE || dt == DisplayType.ID
                    || MetadataHelper.isSystemField(fieldName)) {
                field.addAttribute("queryable", "false");
            }

            // 字段扩展配置
            JSONObject extraAttrs;
            if (StringUtils.isBlank((String) c[14])) {
                extraAttrs = new JSONObject();
            } else {
                extraAttrs = JSON.parseObject((String) c[14]);
            }
            extraAttrs.put("metaId", c[12]);
            extraAttrs.put("comments", c[13]);
            extraAttrs.put("displayType", dt.name());
            field.addAttribute("extra-attrs", extraAttrs.toJSONString());
        }

        if (LOG.isDebugEnabled()) {
            XmlHelper.dump(rootElement);
        }
    }
}
