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

import com.rebuild.server.helper.state.StateHelper;
import com.rebuild.server.helper.state.StateSpec;

/**
 * 审批状态
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/25
 */
public enum ApprovalState implements StateSpec {
	
	DRAFT(1, "草稿"),
	PROCESSING(2, "审批中"),
	APPROVED(10, "通过"),
	REJECTED(11, "驳回"),
	CANCELED(12, "撤销"),
	
	;
	
	private int state;
	private String name;
	
	/**
	 * @param state
	 * @param name
	 */
	ApprovalState(int state, String name) {
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

	@Override
	public boolean isDefault() {
		return this == DRAFT;
	}

	/**
     * @param state
     * @return
     */
	public static StateSpec valueOf(int state) {
		return StateHelper.valueOf(ApprovalState.class, state);
	}
}
