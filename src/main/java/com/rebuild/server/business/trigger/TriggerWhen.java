/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.trigger;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import com.rebuild.server.service.EntityService;

/**
 * 动作类型
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 * @see BizzPermission
 */
public enum TriggerWhen {
	
	/**
	 * 创建时
	 */
	CREATE(BizzPermission.CREATE.getMask()),
	/**
	 * 删除时
	 */
	DELETE(BizzPermission.DELETE.getMask()),
	/**
	 * 更新时
	 */
	UPDATE(BizzPermission.UPDATE.getMask()),
	/**
	 * 分派时
	 */
	ASSIGN(BizzPermission.ASSIGN.getMask()),
	/**
	 * 共享时
	 */
	SHARE(BizzPermission.SHARE.getMask()),
	/**
	 * 取消共享时
	 */
	UNSHARE(EntityService.UNSHARE.getMask())
	
	;
	
	private final int maskValue;
	TriggerWhen(int maskValue) {
		this.maskValue = maskValue;
	}
	
	public int getMaskValue() {
		return maskValue;
	}
}
