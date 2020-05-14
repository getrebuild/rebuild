/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.metadata;

import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.XmlHelper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class DynamicMetadataFactory extends ConfigurationMetadataFactory {
	private static final long serialVersionUID = -5709281079615412347L;

	private static final Log LOG = LogFactory.getLog(DynamicMetadataFactory.class);

	public DynamicMetadataFactory(String configLocation, Dialect dialect) {
		super(configLocation, dialect);
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
				"select typeCode,entityName,physicalName,entityLabel,entityId,comments,icon,nameField,masterEntity from MetaEntity")
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
					.addAttribute("master", (String) c[8])
					.addAttribute("creatable", "true")
					.addAttribute("updatable", "true")
					.addAttribute("queryable", "true")
					.addAttribute("deletable", "true");

            JSONObject extraAttrs = JSONUtils.toJSONObject(
                    new String[] { "metaId", "comments", "icon" },
                    new Object[] { c[4], c[5], c[6] });
            entity.addAttribute("extra-attrs", extraAttrs.toJSONString());
		}

		Object[][] customFields = Application.createQueryNoFilter(
				"select belongEntity,fieldName,physicalName,fieldLabel,displayType,nullable,creatable,updatable,"
						+ "maxLength,defaultValue,refEntity,cascade,fieldId,comments,extConfig,repeatable from MetaField")
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
					.addAttribute("queryable", "true");

			if (fieldName.equals(EntityHelper.AutoId)) {
				field.addAttribute("auto-value", "true");
			}

			DisplayType dt = DisplayType.valueOf((String) c[4]);
			field.addAttribute("type", dt.getFieldType().getName());

			if (dt == DisplayType.DECIMAL) {
				field.addAttribute("decimal-scale", "8");
			} else if (dt == DisplayType.ANYREFERENCE || dt == DisplayType.REFERENCE
					|| dt == DisplayType.PICKLIST || dt == DisplayType.CLASSIFICATION) {
				field.addAttribute("ref-entity", (String) c[10])
						.addAttribute("cascade", (String) c[11]);
			}

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

        XmlHelper.dump((Element) rootElement.selectSingleNode("entity[@name='testallfields999']"));
        if (LOG.isDebugEnabled()) {
            XmlHelper.dump(rootElement);
		}
	}
}
