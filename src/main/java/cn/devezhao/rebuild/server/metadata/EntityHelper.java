package cn.devezhao.rebuild.server.metadata;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.record.RecordCreator;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.service.user.UserService;

/**
 * @author Zhao Fangfang
 * @version $Id: EntityHelper.java 3407 2017-05-05 10:09:40Z devezhao $
 * @since 1.0, 2013-6-26
 */
public class EntityHelper {

	
	/**
	 * 获取实体
	 * 
	 * @param entityName
	 * @return
	 */
	public static Entity getEntity(String entityName) {
		return Application.getPersistManagerFactory().getMetadataFactory().getEntity(entityName);
	}

	/**
	 * 获取实体
	 * 
	 * @param entityCode
	 * @return
	 */
	public static Entity getEntity(int entityCode) {
		return Application.getPersistManagerFactory().getMetadataFactory().getEntity(entityCode);
	}
	
	/**
	 * @param data
	 * @param user
	 * @return
	 */
	public static Record parse(Element data, ID user) {
		String entityName = data.attributeValue("name");
		if (StringUtils.isBlank(entityName) || !data.getName().equals("entity")) {
			throw new IllegalArgumentException("无效实体数据格式\n----\n" + data.asXML());
		}
		
		RecordCreator creator = new ExtRecordCreator(getEntity(entityName), data, user);
		Record record = creator.create();
		bindCommonsFieldsValue(record, record.getPrimary() == null);
		return record;
	}

	/**
	 * @param recordId
	 * @param user
	 * @return
	 */
	public static Record forUpdate(ID recordId, ID user) {
		Entity entity = getEntity(recordId.getEntityCode());
		Record r = new StandardRecord(entity, user);
		r.setID(entity.getPrimaryField().getName(), recordId);
		bindCommonsFieldsValue(r, false);
		return r;
	}
	
	/**
	 * @param entityCode
	 * @param user
	 * @return
	 */
	public static Record forNew(int entityCode, ID user) {
		Entity entity = EntityHelper.getEntity(entityCode);
		Record r = new StandardRecord(entity, user);
		bindCommonsFieldsValue(r, true);
		return r;
	}
	
	/**
	 * 绑定公用/权限字段值
	 * 
	 * @param r
	 * @param isNew
	 */
	protected static void bindCommonsFieldsValue(Record r, boolean isNew) {
		final Date now = CalendarUtils.now();
		final Entity entity = r.getEntity();
		
		if (entity.containsField(modifiedOn)) {
			r.setDate(modifiedOn, now);
		}
		if (entity.containsField(modifiedBy)) {
			r.setID(modifiedBy, r.getEditor());
		}
		
		if (isNew) {
			if (entity.containsField(createdOn)) {
				r.setDate(createdOn, now);
			}
			if (entity.containsField(createdBy)) {
				r.setID(createdBy, r.getEditor());
			}
			if (entity.containsField(owningUser)) {
				r.setID(owningUser, Application.getBean(UserService.class).getDeptOfUser(r.getEditor()));
			}
		}
	}

	
	// 公共字段
	
	public static final String createdOn = "createdOn";
	public static final String createdBy = "createdBy";
	public static final String modifiedOn = "modifiedOn";
	public static final String modifiedBy = "modifiedBy";
	public static final String owningUser = "owningUser";
	public static final String owningDept = "owningDept";
	
	// 实体代码

	public static final int User = 001;
	public static final int Department = 002;
	public static final int Role = 003;
	public static final int RolePrivileges = 004;
	public static final int RoleMember = 005;
	
	public static final int MetaEntity = 010;
	public static final int MetaField = 011;
	
}
