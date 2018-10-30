/*
rebuild - Building your system freely.
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

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ThreadPool;

/**
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public abstract class AwareObserver implements Observer {

	protected static final Log LOG = LogFactory.getLog(AwareObserver.class);
	
	@Override
	public void update(final Observable o, final Object arg) {
		final AwareContext ctx = (AwareContext) arg;
		if (isAsync()) {
			ThreadPool.exec(new Runnable() {
				@Override
				public void run() {
					try {
						update(ctx);
					} catch (Exception ex) {
						LOG.error("AwareContext : " + ctx, ex);
					}
				}
			});
		} else {
			update(ctx);
		}
	}
	
	private void update(AwareContext ctx) {
		if (ctx.getAction() == BizzPermission.CREATE) {
			notifyCreate(ctx);
		} else if (ctx.getAction() == BizzPermission.UPDATE) {
			notifyUpdate(ctx);
		} else if (ctx.getAction() == BizzPermission.DELETE) {
			notifyDelete(ctx);
		} else if (ctx.getAction() == BizzPermission.ASSIGN) {
			notifyAssign(ctx);
		} else if (ctx.getAction() == BizzPermission.SHARE) {
			notifySahre(ctx);
		}
	}

	/**
	 * 是否异步
	 * 
	 * @return
	 */
	abstract protected boolean isAsync();
	
	public void notifyCreate(final AwareContext context) {
	}

	public void notifyUpdate(final AwareContext context) {
	}

	public void notifyDelete(final AwareContext context) {
	}

	public void notifyAssign(final AwareContext context) {
	}

	public void notifySahre(final AwareContext context) {
	}
}
