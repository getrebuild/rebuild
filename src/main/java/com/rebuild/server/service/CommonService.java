/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
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
public class CommonService extends BaseService {

	/**
	 * @param aPMFactory
	 */
	public CommonService(PersistManagerFactory aPMFactory) {
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
	 * @param deleted
	 */
	public void delete(ID[] deleted) {
		delete(deleted, true);
	}

	/**
	 * 批量删除
	 *
	 * @param deleted
	 * @param strictMode
	 */
	public void delete(ID[] deleted, boolean strictMode) {
		Assert.notNull(deleted, "[deleted] cannot be null");
		for (ID id : deleted) {
			delete(id, strictMode);
		}
	}

	/**
	 * 批量新建/更新
	 *
	 * @param records
	 */
	public void createOrUpdate(Record[] records, ID deleted[]) {
		createOrUpdate(records, deleted, true);
	}

	/**
	 * 批量新建/更新
	 *
	 * @param records
	 * @param deleted
	 * @param strictMode
	 */
	public void createOrUpdate(Record[] records, ID deleted[], boolean strictMode) {
		createOrUpdate(records, strictMode);
		delete(deleted, strictMode);
	}

	/**
	 * 业务实体禁止调用此类
	 *
	 * @param idOrRecord
	 * @throws PrivilegesException
	 */
	protected void tryIfWithPrivileges(Object idOrRecord) throws PrivilegesException {
		Entity entity = null;
		if (idOrRecord instanceof ID) {
			entity = MetadataHelper.getEntity(((ID) idOrRecord).getEntityCode());
		} else {
			entity = ((Record) idOrRecord).getEntity();
		}

		// 使用主实体
		if (MetadataHelper.isSlaveEntity(entity.getEntityCode())) {
			entity = entity.getMasterEntity();
		}

		if (EntityHelper.hasPrivilegesField(entity)) {
			throw new PrivilegesException("Has privileges of Entity cannot use this class(methods) : " + entity.getName());
		}
	}
}
