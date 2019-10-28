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

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.base.ApprovalStepService;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2019/10/23
 */
public class ApprovalHelper {

    /**
     * 获取提交人
     *
     * @param record
     * @return
     */
    public static ID getSubmitter(ID record) {
        Object[] approvalId = Application.getQueryFactory().uniqueNoFilter(record, EntityHelper.ApprovalId);
        Assert.notNull(approvalId, "Couldn't found approval of record : " + record);
        return getSubmitter(record, (ID) approvalId[0]);
    }

    /**
     * 获取提交人
     *
     * @param record
     * @param approval
     * @return
     */
    public static ID getSubmitter(ID record, ID approval) {
        return Application.getBean(ApprovalStepService.class).findSubmitter(record, approval);
    }

    /**
     * @param record
     * @return returns [ApprovalId, ApprovalName, ApprovalState]
     */
    public static Object[] getApprovalStatus(ID record) {
        return Application.getQueryFactory().uniqueNoFilter(record,
                EntityHelper.ApprovalId, EntityHelper.ApprovalId + ".name", EntityHelper.ApprovalState);
    }
}
