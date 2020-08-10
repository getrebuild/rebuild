/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
	CANCELED(12, "撤回"),  // 或无效的
	REVOKED(13, "撤销"),

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
