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

package com.rebuild.server.service.base.obs;

import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;

/**
 * TODO 记录审计日志
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public class AuditLoggingObserver extends OperatingObserver {
	
	@Override
	public void onCreate(OperatingContext context) {
		LOG.warn(context);
	}
	
	@Override
	public void onUpdate(OperatingContext context) {
		LOG.warn(context);
	}
	
	@Override
	public void onDelete(OperatingContext context) {
		LOG.warn(context);
	}
	
	@Override
	public void onAssign(OperatingContext context) {
		LOG.warn(context);
	}
	
	@Override
	public void onShare(OperatingContext context) {
		LOG.warn(context);
	}
	
	@Override
	public void onUnshare(OperatingContext context) {
		LOG.warn(context);
	}
}