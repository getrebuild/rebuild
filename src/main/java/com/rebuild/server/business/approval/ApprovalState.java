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

package com.rebuild.server.business.approval;

import com.rebuild.server.metadata.entity.State;

/**
 * 审批状态
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/25
 */
public enum ApprovalState implements State {
	
	DRAFT(1, "草稿"),
	PROCESSING(2, "审批中"),
	APPROVED(10, "通过"),
	REJECTED(11, "驳回"),
	
	// 暂未用
	CANCELED(12, "撤回"),
	
	;
	
	private int state;
	private String name;
	
	/**
	 * @param state
	 * @param name
	 */
	private ApprovalState(int state, String name) {
		this.state = state;
		this.name = name;
	}
	
	@Override
	public int getState() {
		return state;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public static State valueOf(int state) {
		for (ApprovalState s : ApprovalState.values()) {
			if (s.getState() == state) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unknow state : " + state);
	}
}
