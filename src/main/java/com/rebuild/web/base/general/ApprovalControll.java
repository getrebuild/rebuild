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

package com.rebuild.web.base.general;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/05
 */
@Controller
@RequestMapping("/app/entity/approval/")
public class ApprovalControll extends BaseControll {
	
	@RequestMapping("state")
	public void fetchApprovalState(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
		
		Object[] state = Application.getQueryFactory().unique(recordId,
				EntityHelper.ApprovalId, EntityHelper.ApprovalState, EntityHelper.ApprovalStepId);
		if (state == null) {
			writeFailure(response, "无效记录");
			return;
		}
		
		Map<String, Object> data = new HashMap<>();

		int stateInt = ObjectUtils.toInt(state[1], ApprovalState.DRAFT.getState());
		data.put("state", stateInt);
		if (state[0] != null) {
			data.put("approvalId", state[0]);
		}
		
		// 获取所有节点
		if (stateInt != ApprovalState.DRAFT.getState()) {
			Object[] steps = Application.createQuery(
					"select approver,state,prevStepId from RobotApprovalStep where recordId = ?")
					.setParameter(1, recordId)
					.array();
		}
	}
}
