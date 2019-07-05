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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
public class ApprovalProcessor {
	
	public static final ApprovalProcessor instance = new ApprovalProcessor();
	private ApprovalProcessor() {}

	/**
	 * 获取已执行流程列表
	 * 
	 * @param recordId
	 * @param approvalId
	 * @return returns [ [S,S], [S], [SSS], [S] ]
	 */
	public JSONArray getSteps(ID recordId, ID approvalId) {
		Object[][] array = Application.createQuery(
				"select prevStepId,approver,state,createdOn,stepId from RobotApprovalStep where recordId = ? and approvalId = ?")
				.setParameter(1, recordId)
				.setParameter(2, approvalId)
				.array();

		Map<ID, List<Object[]>> stepsByPrev = new HashMap<>();
		for (Object[] o : array) {
			ID prev = o[0] == null ? approvalId : (ID) o[0];
			List<Object[]> steps = stepsByPrev.get(prev);
			if (steps == null) {
				steps = new ArrayList<Object[]>();
				stepsByPrev.put(prev, steps);
			}
			steps.add(o);
		}
		
		JSONArray steps = new JSONArray();
		
		List<Object[]> root = stepsByPrev.remove(approvalId);
		Set<ID> prevIds = formatSteps(root, steps);
		
		while (true) {
			Set<ID> lastPrevIds = new HashSet<ID>();
			for (ID prev : prevIds) {
				List<Object[]> next = stepsByPrev.get(prev);
				if (next != null) {
					lastPrevIds.addAll(formatSteps(next, steps));
					stepsByPrev.remove(prev);
				}
			}
			
			prevIds = lastPrevIds;
			if (prevIds.isEmpty()) {
				break;
			}
		}
		
		return steps;
	}
	
	/**
	 * @param steps
	 * @param dest
	 * @return
	 */
	private Set<ID> formatSteps(List<Object[]> steps, JSONArray dest) {
		Set<ID> ids = new HashSet<>();
		JSONArray list = new JSONArray();
		for (Object[] o : steps) {
			ID approver = (ID) o[1];
			JSONObject step = JSONUtils.toJSONObject(
					new String[] { "approver", "approverName", "state", "createdOn" }, 
					new Object[] { approver, UserHelper.getName(approver), o[2], 
							CalendarUtils.getUTCDateTimeFormat().format(o[3]) });
			list.add(step);
			ids.add((ID) o[4]);
		}
		dest.add(list);
		return ids;
	}
	
}
