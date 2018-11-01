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
 * 记录操作观察者
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public abstract class OperateObserver implements Observer {

	protected final Log LOG = LogFactory.getLog(getClass());
	
	@Override
	public void update(final Observable o, final Object arg) {
		final OperateContext ctx = (OperateContext) arg;
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
	
	/**
	 * @param ctx
	 */
	protected void update(OperateContext ctx) {
		if (ctx.getAction() == BizzPermission.CREATE) {
			onCreate(ctx);
		} else if (ctx.getAction() == BizzPermission.UPDATE) {
			onUpdate(ctx);
		} else if (ctx.getAction() == BizzPermission.DELETE) {
			onDelete(ctx);
		} else if (ctx.getAction() == BizzPermission.ASSIGN) {
			onAssign(ctx);
		} else if (ctx.getAction() == BizzPermission.SHARE) {
			onSahre(ctx);
		}
	}

	/**
	 * @return
	 */
	protected boolean isAsync() {
		return true;
	}
	
	public void onCreate(final OperateContext context) {
		LOG.info("Only logging context - " + context);
	}

	public void onUpdate(final OperateContext context) {
		LOG.info("Only logging context - " + context);
	}

	public void onDelete(final OperateContext context) {
		LOG.info("Only logging context - " + context);
	}

	public void onAssign(final OperateContext context) {
		LOG.info("Only logging context - " + context);
	}

	public void onSahre(final OperateContext context) {
		LOG.info("Only logging context - " + context);
	}
}
