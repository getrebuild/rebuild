package cn.devezhao.rebuild.server.metadata;

import org.dom4j.Element;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.record.XmlRecordCreator;

/**
 * @author Zhao Fangfang
 * @version $Id: ExtRecordCreator.java 22 2013-06-26 12:15:01Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-26
 */
public class ExtRecordCreator extends XmlRecordCreator {

	/**
	 * @param entity
	 * @param source
	 * @param editor
	 */
	public ExtRecordCreator(Entity entity, Element source, ID editor) {
		super(entity, source, editor);
	}
	
	@Override
	protected void afterCreate(Record record, boolean isNew) {
		super.afterCreate(record, isNew);
		EntityHelper.bindCommonsFieldsValue(record, isNew);
	}
}