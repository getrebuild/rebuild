/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO
 *
 * @author ZHAO
 * @since 2020/11/25
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class HookUrlController extends BaseController {

    @PostMapping("hookurl-test")
    public RespBody availableTypes(@RequestBody JSON data) {
        return null;
    }
}
