/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.TestSupport;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.charts.builtin.ApprovalList;
import com.rebuild.server.business.charts.builtin.FeedsSchedule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ZHAO
 * @since 2019/10/14
 */
public class BuiltinChartsTest extends TestSupport {

    @Test
    public void testApprovalList() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("state", ApprovalState.APPROVED.getState());
        JSON ret = new ApprovalList().setUser(SIMPLE_USER).setExtraParams(params).build();
        System.out.println(ret);
    }

    @Test
    public void testFeedsSchedule() throws Exception {
        JSON ret = new FeedsSchedule().setUser(SIMPLE_USER).build();
        System.out.println(ret);
    }
}
