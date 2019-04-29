/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.rbstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.metadata.entityhub.Entity2Schema;
import com.rebuild.server.metadata.entityhub.Field2Schema;
import com.rebuild.server.metadata.entityhub.ModifiyMetadataException;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.support.Table;

/**
 * 元数据模型导入
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 * 
 * @see MetaSchemaGenerator
 */
public class MetaschemaImporter extends HeavyTask<String> {
	
	private static final Log LOG = LogFactory.getLog(MetaschemaImporter.class);
	
	final private ID user;
	
	final private String fileUrl;
	private JSONObject remoteData;
	
	/**
	 * @param user
	 * @param fileUrl
	 */
	public MetaschemaImporter(ID user, String fileUrl) {
		this.user = user;
		this.fileUrl = fileUrl;
		this.remoteData = null;
	}
	
	/**
	 * for TestCase
	 * 
	 * @param user
	 * @param data
	 */
	protected MetaschemaImporter(ID user, JSONObject data) {
		this.user = user;
		this.fileUrl = null;
		this.remoteData = data;
	}
	
	/**
	 * 验证导入
	 * 
	 * @return 错误消息，返回 null 表示验证通过
	 */
	public String verfiy() {
		this.readyRemoteData();
		
		String hasError = verfiyEntity(remoteData);
		if (hasError != null) {
			return hasError;
		}
		
		JSONObject slave = remoteData.getJSONObject("slave");
		if (slave != null) {
			hasError = verfiyEntity(slave);
			if (hasError != null) {
				return hasError;
			}
		}
		
		return null;
	}
	
	private String verfiyEntity(JSONObject entity) {
		String entityName = entity.getString("entity");
		if (MetadataHelper.containsEntity(entityName)) {
			return "实体名称重复: " + entityName;
		}
		
		for (Object o : entity.getJSONArray("fields")) {
			JSONObject field = (JSONObject) o;
			if (DisplayType.REFERENCE.name().equalsIgnoreCase(field.getString("displayType"))) {
				String refEntity = field.getString("refEntity");
				if (!MetadataHelper.containsEntity(refEntity)) {
					return "缺少必要的引用实体: " + entityName;
				}
			}
		}
		return null;
	}
	
	private void readyRemoteData() {
		if (this.remoteData == null) {
			this.remoteData = (JSONObject) RBStore.fetchMetaschema(fileUrl);
		}
	}
	
	@Override
	public String exec() throws Exception {
		this.readyRemoteData();
		setTotal(100);
		setThreadUser(this.user);
		
		String entityName = performEntity(remoteData, null);
		Entity createdEntity = MetadataHelper.getEntity(entityName);
		setComplete(50);
		
		JSONObject slave = remoteData.getJSONObject("slave");
		if (slave != null) {
			try {
				performEntity(remoteData, createdEntity.getName());
				setComplete(100);
			} catch (ModifiyMetadataException ex) {
				// 出现异常，删除主实体
				new Entity2Schema(this.user).drop(createdEntity, true);
				
				throw ex;
			}
		}
		
		return entityName;
	}
	
	/**
	 * @param schemaEntity
	 * @param masterEntityName
	 * @return
	 * @throws ModifiyMetadataException
	 */
	private String performEntity(JSONObject schemaEntity, String masterEntityName) throws ModifiyMetadataException {
		String entityName = schemaEntity.getString("entity");
		String entityLabel = schemaEntity.getString("entityLabel");
		
		Entity2Schema entity2Schema = new Entity2Schema(this.user);
		entity2Schema.createEntity(
				entityName, entityLabel, schemaEntity.getString("comments"), masterEntityName, false);
		Entity entity = MetadataHelper.getEntity(entityName);
		this.setComplete((int) (this.getComplete() * 1.5));
		
		// to DB
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		StringBuilder ddl = new StringBuilder("alter table `" + entity.getPhysicalName() + "`");
		
		JSONArray fields = schemaEntity.getJSONArray("fields");
		for (Object field : fields) {
			try {
				Field unsafe = performField((JSONObject) field, entity);
				
				ddl.append("\n  add column ");
				table.generateFieldDDL(unsafe, ddl);
				ddl.append(",");
				
			} catch (Exception ex) {
				entity2Schema.drop(entity, true);
				
				if (ex instanceof ModifiyMetadataException) {
					throw ex;
				} else {
					throw new ModifiyMetadataException(ex);
				}
			}
		}
		
		ddl.deleteCharAt(ddl.length() - 1);
		try {
			Application.getSQLExecutor().executeBatch(new String[] { ddl.toString() });
		} catch (Exception ex) {
			LOG.error("DDL ERROR : \n" + ddl, ex);
			entity2Schema.drop(entity, true);
			
			throw new ModifiyMetadataException(ex);
		}
		
		String nameField = schemaEntity.getString("nameField");
		if (nameField != null) {
			EasyMeta easyMeta = EasyMeta.valueOf(entity);
			Record updateNameField = EntityHelper.forUpdate(easyMeta.getMetaId(), this.user, false);
			updateNameField.setString("nameField", nameField);
			Application.getCommonService().update(updateNameField);
		}
		
		Application.getMetadataFactory().refresh(false);
		return entityName;
	}
	
	private Field performField(JSONObject schemaField, Entity belong) {
		String fieldName = schemaField.getString("field");
		String fieldLabel = schemaField.getString("fieldLabel");
		String displayType = schemaField.getString("displayType");
		JSON extConfig = schemaField.getJSONObject("extConfig");
		
		Field unsafeField = new Field2Schema(this.user).createUnsafeField(
				belong, fieldName, fieldLabel, DisplayType.valueOf(displayType),
				schemaField.getBooleanValue("nullable"),
				true,
				schemaField.getBooleanValue("updatable"),
				schemaField.getString("comments"),
				schemaField.getString("refEntity"),
				null, true, extConfig, 
				schemaField.getString("defaultValue"));
		return unsafeField;
	}
}
