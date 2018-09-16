/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.metadata;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.XmlHelper;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.service.entitymanage.DisplayType;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class DynamicMetadataFactory extends ConfigurationMetadataFactory {
	private static final long serialVersionUID = -5709281079615412347L;
	
	private static final Log LOG = LogFactory.getLog(DynamicMetadataFactory.class);
	
	// <Name, [ID, COMMENTS, ICON]>
	private static final Map<String, Object[]> ENTITY_EXTMETA = new HashMap<>();
	// <Name, [ID, COMMENTS]>
	private static final Map<String, Object[]> FIELD_EXTMETA = new HashMap<>();
	
	public DynamicMetadataFactory(String configLocation, Dialect dialect) {
		super(configLocation, dialect);
	}
	
	@Override
	protected Document readConfiguration(boolean initState) {
		Document config = super.readConfiguration(initState);
		if (initState == false) {
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
		
		Object[][] customentity = Application.createNoFilterQuery(
				"select typeCode,entityName,physicalName,entityLabel,entityId,comments,icon,nameField from MetaEntity order by createdOn")
				.array();
		for (Object[] custom : customentity) {
			int typeCode = (int) custom[0];
			String name = (String) custom[1];
			String physicalName = (String) custom[2];
			String description = (String) custom[3];
			
			Element entity = rootElement.addElement("entity");
			entity.addAttribute("type-code", typeCode + "")
					.addAttribute("name", name)
					.addAttribute("physical-name", physicalName)
					.addAttribute("description", description)
					.addAttribute("parent", "false")
					.addAttribute("name-field", (String) custom[7]);
			ENTITY_EXTMETA.put(name, new Object[] { custom[4], custom[5], custom[6] });
		}
		
		Object[][] customfield = Application.createNoFilterQuery(
				"select belongEntity,fieldName,physicalName,fieldLabel,displayType,nullable,creatable,updatable,precision,maxLength,defaultValue,refEntity,cascade,fieldId,comments,extConfig"
				+ " from MetaField order by createdOn")
				.array();
		for (Object[] custom : customfield) {
			String entityName = (String) custom[0];
			String fieldName = (String) custom[1];
			Element entityElement = (Element) rootElement.selectSingleNode("entity[@name='" + entityName + "']");
			if (entityElement == null) {
				LOG.warn("无效字段 [ " + fieldName + " ] 无有效依附实体");
				continue;
			}
			
			Element field = entityElement.addElement("field");
			field.addAttribute("name", fieldName)
					.addAttribute("physical-name", (String) custom[2])
					.addAttribute("description", (String) custom[3])
					.addAttribute("nullable", custom[5].toString())
					.addAttribute("creatable", custom[6].toString())
					.addAttribute("updatable", custom[7].toString())
					.addAttribute("decimal-scale", custom[8].toString())
					.addAttribute("max-length", custom[9].toString())
					.addAttribute("default-value", (String) custom[10])
					.addAttribute("ref-entity", (String) custom[11])
					.addAttribute("cascade", (String) custom[12]);
			
			DisplayType dt = DisplayType.valueOf((String) custom[4]);
			field.addAttribute("type", dt.getFieldType().getName());
			
			FIELD_EXTMETA.put(entityName + "__" + fieldName, new Object[] { custom[13], custom[14], dt, custom[15] });
		}
		
		if (LOG.isDebugEnabled()) {
			XmlHelper.dump(rootElement);
		}
	}
	
	/**
	 * @param entity
	 * @return
	 */
	protected Object[] getEntityExtmeta(String entity) {
		return ENTITY_EXTMETA.get(entity);
	}
	
	/**
	 * @param entity
	 * @param field
	 * @return
	 */
	protected Object[] getFieldExtmeta(String entity, String field) {
		String key = entity + "__" + field;
		return FIELD_EXTMETA.get(key);
	}
}
