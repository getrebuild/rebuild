/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.rbv.openapi.general.EntityCreate;

/**
 * @author RB
 * @since 2026/6/11
 */
public class UseEntityCreate extends EntityCreate {

    /**
     * @param entity
     * @param data
     * @return
     */
    public static JSONObject cleanPostData(Entity entity, JSONObject data) {
        return new UseEntityCreate().cleanPostData(entity, data, true);
    }
}
