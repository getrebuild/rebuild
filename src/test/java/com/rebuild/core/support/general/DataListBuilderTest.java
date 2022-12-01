/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author Zixin (RB)
 * @since Jan 6, 2019
 */
public class DataListBuilderTest extends TestSupport {

    private static JSONObject queryExpr;

    static {
        queryExpr = JSON.parseObject("{ entity:'User' }");
        JSON fields = JSON.parseArray("[ 'userId', 'loginName', 'createdOn', 'createdBy', 'createdBy.fullName', 'modifiedBy.fullName' ]");
        queryExpr.put("fields", fields);
        JSON filter = JSON.parseObject("{ entity:'User', type:'QUICK', values:{ 1:'admin' } }");
        queryExpr.put("filter", filter);
        queryExpr.put("sort", "createdOn:desc");
        queryExpr.put("pageNo", 1);
        queryExpr.put("pageSize", 100);
    }

    @Test
    public void testQueryParser() {
        QueryParser queryParser = new QueryParser(queryExpr);
        System.out.println(queryParser.toSql());
        System.out.println(queryParser.toCountSql());
    }

    @Test
    public void testQuery() {
        DataListBuilder dlc = new DataListBuilderImpl(queryExpr, UserService.ADMIN_USER);
        System.out.println(dlc.getJSONResult());
    }

    @Test
    public void testJoinFields() {
        QueryParser queryParser = new QueryParser(queryExpr);
        System.out.println(queryParser.getQueryJoinFields());
    }

    @Test
    public void testColumnLayout() {
        JSON layout = DataListManager.instance.getListFields(Account, SIMPLE_USER);
        System.out.println(layout);
    }
}
