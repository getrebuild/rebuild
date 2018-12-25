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

import com.rebuild.server.service.base.GeneralEntityService;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ThreadPool;

/**
 * 记录操作观察者。子类复写需要关注的操作即可
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
						LOG.error("OperateContext : " + ctx, ex);
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
			onShare(ctx);
		} else if (ctx.getAction() == GeneralEntityService.UNSHARE) {
			onUnShare(ctx);
		} 
	}

	/**
	 * 是否异步执行
	 * 
	 * @return
	 */
	protected boolean isAsync() {
		return false;
	}
	
	public void onCreate(final OperateContext context) {
		if (LOG.isDebugEnabled()) {
			LOG.info("onCreate - " + context);
		}
	}

	public void onUpdate(final OperateContext context) {
		if (LOG.isDebugEnabled()) {
			LOG.info("onUpdate - " + context);
		}
	}

	public void onDelete(final OperateContext context) {
		if (LOG.isDebugEnabled()) {
			LOG.info("onDelete - " + context);
		}
	}

	public void onAssign(final OperateContext context) {
		if (LOG.isDebugEnabled()) {
			LOG.info("onAssign - " + context);
		}
	}

	public void onShare(final OperateContext context) {
		if (LOG.isDebugEnabled()) {
			LOG.info("onSahre - " + context);
		}
	}
	
	public void onUnShare(final OperateContext context) {
		if (LOG.isDebugEnabled()) {
			LOG.info("onUnShare - " + context);
		}
	}
}
