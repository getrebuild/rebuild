/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 基础服务类
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseService implements ServiceSpec {
	
	protected final Log LOG = LogFactory.getLog(getClass());
	
	final private PersistManagerFactory aPMFactory;

	protected BaseService(PersistManagerFactory aPMFactory) {
		super();
		this.aPMFactory = aPMFactory;
	}
	
	@Override
	public Record createOrUpdate(Record record) {
		return record.getPrimary() == null ? create(record) : update(record);
	}
	
	@Override
	public Record create(Record record) {
		return aPMFactory.createPersistManager().save(record);
	}

	@Override
	public Record update(Record record) {
		return aPMFactory.createPersistManager().update(record);
	}

	@Override
	public int delete(ID recordId) {
		int affected = aPMFactory.createPersistManager().delete(recordId);
		Application.getRecordOwningCache().cleanOwningUser(recordId);
		return affected;
	}
	
	/**
	 * @return
	 */
	public PersistManagerFactory getPMFactory() {
		return aPMFactory;
	}
	
	@Override
	public String toString() {
		if (getEntityCode() > 0) {
			return "service." + aPMFactory.getMetadataFactory().getEntity(getEntityCode()).getName() + "@" + Integer.toHexString(hashCode());
		} else {
			return super.toString();
		}
	}
}
