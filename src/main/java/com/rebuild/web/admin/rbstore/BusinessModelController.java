/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.rebuild.api.RespBody;
import com.rebuild.core.rbstore.RBStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author devezhao
 * @since 2020/9/30
 */
@RestController
@RequestMapping("/admin/rbstore/business-model")
public class BusinessModelController {

    @GetMapping("index")
    public JSONAware fetchIndex() {
        JSON data = RBStore.fetchBusinessModel("index.json");
        return data == null ? RespBody.error() : data;
    }
}
