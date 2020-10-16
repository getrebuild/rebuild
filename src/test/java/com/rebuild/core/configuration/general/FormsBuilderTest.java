/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.JSONUtils;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/03
 */
public class FormsBuilderTest extends TestSupport {

    @Test
    public void testModel() {
        JSON newModel = FormsBuilder.instance.buildForm("User", UserService.ADMIN_USER, null);
        System.out.println(newModel);

        JSON editModel = FormsBuilder.instance.buildForm("User", UserService.ADMIN_USER, UserService.SYSTEM_USER);
        System.out.println(editModel);
    }

    @Test
    public void testViewModel() {
        JSON viewModel = FormsBuilder.instance.buildView("User", UserService.ADMIN_USER, UserService.SYSTEM_USER);
        System.out.println(viewModel);
    }


    @Test
    public void testSetFormInitialValue() {
        Entity SalesOrder999 = MetadataHelper.getEntity(SalesOrder);
        Entity SalesOrderItem999 = MetadataHelper.getEntity(SalesOrderItem);

        JSONObject initial = JSONUtils.toJSONObject(FormsBuilder.DV_MAINID, ID.newId(SalesOrder999.getEntityCode()));

        JSONArray elements = new JSONArray();
        JSONObject el = new JSONObject();
        el.put("field", "SalesOrder999Id");
        elements.add(el);

        JSONObject mockModel = new JSONObject();
        mockModel.put("elements", elements);

        FormsBuilder.instance.setFormInitialValue(SalesOrderItem999, mockModel, initial);
    }
}
