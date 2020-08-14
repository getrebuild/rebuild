/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.RebuildException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import org.springframework.util.Assert;

/**
 * 普通的 CRUD 服务
 * <br>- 此类有事物
 * <br>- 此类不经过用户权限验证 {@link PrivilegesGuardInterceptor}
 * <br>- 有权限的实体使用此类需要指定 <tt>strictMode=false</tt>
 *
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
public class CommonsService extends BaseService {

	protected CommonsService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return 0;
	}

	@Override
	public Record create(Record record) {
		return this.create(record, true);
	}

	@Override
	public int delete(ID recordId) {
		return this.delete(recordId, true);
	}

	@Override
	public Record update(Record record) {
		return update(record, true);
	}

	/**
	 * @param record
	 * @param strictMode
	 * @return
	 */
	public Record create(Record record, boolean strictMode) {
		if (strictMode) {
			tryIfWithPrivileges(record);
		}
		return super.create(record);
	}

	/**
	 * @param record
	 * @param strictMode
	 * @return
	 */
	public Record update(Record record, boolean strictMode) {
		if (strictMode) {
			tryIfWithPrivileges(record);
		}
		return super.update(record);
	}

	/**
	 * @param recordId
	 * @param strictMode
	 * @return
	 */
	public int delete(ID recordId, boolean strictMode) {
		if (strictMode) {
			tryIfWithPrivileges(recordId);
		}
		return super.delete(recordId);
	}

	/**
	 * 批量新建/更新
	 *
	 * @param records
	 */
	public void createOrUpdate(Record[] records) {
		createOrUpdate(records, true);
	}

	/**
	 * 批量新建/更新
	 *
	 * @param records
	 * @param strictMode
	 */
	public void createOrUpdate(Record[] records, boolean strictMode) {
		Assert.notNull(records, "[records] cannot be null");
		for (Record r : records) {
			if (r.getPrimary() == null) {
				create(r, strictMode);
			} else {
				update(r, strictMode);
			}
		}
	}

	/**
	 * 批量删除
	 *
	 * @param deletes
	 */
	public void delete(ID[] deletes) {
		delete(deletes, true);
	}

	/**
	 * 批量删除
	 *
	 * @param deletes
	 * @param strictMode
	 */
	public void delete(ID[] deletes, boolean strictMode) {
		Assert.notNull(deletes, "[deleted] cannot be null");
		for (ID id : deletes) {
			delete(id, strictMode);
		}
	}

	/**
	 * 批量新建/更新、删除（同一事物）
	 *
	 * @param records
	 * @param deletes
	 */
	public void createOrUpdateAndDelete(Record[] records, ID[] deletes) {
		createOrUpdateAndDelete(records, deletes, true);
	}

	/**
	 * 批量新建/更新、删除
	 *
	 * @param records
	 * @param deletes
	 * @param strictMode
	 */
	public void createOrUpdateAndDelete(Record[] records, ID[] deletes, boolean strictMode) {
		createOrUpdate(records, strictMode);
		delete(deletes, strictMode);
	}

	/**
	 * 业务实体禁止调用此类
	 *
	 * @param idOrRecord
	 * @throws PrivilegesException
	 */
	protected void tryIfWithPrivileges(Object idOrRecord) throws PrivilegesException {
		Entity entity;
		if (idOrRecord instanceof ID) {
			entity = MetadataHelper.getEntity(((ID) idOrRecord).getEntityCode());
		} else if (idOrRecord instanceof Record) {
			entity = ((Record) idOrRecord).getEntity();
		} else {
			throw new RebuildException("Invalid argument [idOrRecord] : " + idOrRecord);
		}

		// 验证主实体
		if (MetadataHelper.isSlaveEntity(entity.getEntityCode())) {
			entity = entity.getMasterEntity();
		}

		if (MetadataHelper.hasPrivilegesField(entity)) {
			throw new PrivilegesException("Privileges entity cannot use this class (methods) : " + entity.getName());
		}
	}
}
