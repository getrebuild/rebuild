/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.service.DataSpecificationException;

/**
 * 删除
 * 
 * @author devezhao
 * @since 10/16/2018
 */
public class BulkDelete extends BulkOperator {

	protected BulkDelete(BulkContext context, GeneralEntityService ges) {
		super(context, ges);
	}

	@Override
	protected Integer exec() {
		final ID[] records = prepareRecords();
		this.setTotal(records.length);
		
		for (ID id : records) {
			if (Application.getPrivilegesManager().allowDelete(context.getOpUser(), id)) {
				try {
					ges.delete(id, context.getCascades());
					this.addSucceeded();
				} catch (DataSpecificationException ex) {
					LOG.warn("Couldn't delete : " + id + " Ex : " + ex);
				}
			} else {
				LOG.warn("No have privileges to DELETE : " + context.getOpUser() + " > " + id);
			}
			this.addCompleted();
		}

		return getSucceeded();
	}
}
