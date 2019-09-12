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

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ThreadPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Observable;
import java.util.Observer;

/**
 * 记录操作观察者。子类复写需要关注的操作即可
 * 
 * @author devezhao
 * @since 10/31/2018
 * 
 * @see ObservableService
 */
public abstract class OperatingObserver implements Observer {

	protected final Log LOG = LogFactory.getLog(getClass());
	
	protected OperatingObserver() {
		super();
	}
	
	@Override
	public void update(final Observable o, final Object arg) {
		final OperatingContext ctx = (OperatingContext) arg;
		if (isAsync()) {
			ThreadPool.exec(() -> {
				try {
					updateByAction(ctx);
				} catch (Exception ex) {
					LOG.error("OperateContext : " + ctx, ex);
				}
			});
		} else {
			updateByAction(ctx);
		}
	}
	
	/**
	 * @param ctx
	 */
	protected void updateByAction(OperatingContext ctx) {
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
		} else if (ctx.getAction() == EntityService.UNSHARE) {
			onUnshare(ctx);
		} else if (ctx.getAction() == ObservableService.DELETE_BEFORE) {
			onDeleteBefore(ctx);
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
	
	// -- 根据需要复写以下方法
	
	/**
	 * 新建时
	 * 
	 * @param context
	 */
	protected void onCreate(final OperatingContext context) {
	}

	/**
	 * 更新时
	 * 
	 * @param context
	 */
	protected void onUpdate(final OperatingContext context) {
	}

	/**
	 * 删除时
	 * 
	 * @param context
	 */
	protected void onDelete(final OperatingContext context) {
	}
	
	/**
	 * 删除前处理
	 * 
	 * @param context
	 */
	protected void onDeleteBefore(final OperatingContext context) {
	}
	
	/**
	 * 分派时
	 * 
	 * @param context
	 */
	protected void onAssign(final OperatingContext context) {
	}

	/**
	 * 共享时
	 * 
	 * @param context
	 */
	protected void onShare(final OperatingContext context) {
	}
	
	/**
	 * 取消共享时
	 * 
	 * @param context
	 */
	protected void onUnshare(final OperatingContext context) {
	}
}
