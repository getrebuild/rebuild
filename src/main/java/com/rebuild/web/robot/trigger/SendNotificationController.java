/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import com.alibaba.fastjson.JSON;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class SendNotificationController extends BaseController {

    @GetMapping("sendnotification-atypes")
    public JSON availableTypes() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("serviceMail", SMSender.availableMail());
        map.put("serviceSms", SMSender.availableSMS());
        return (JSON) JSON.toJSON(map);
    }
}
