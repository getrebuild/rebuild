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

package com.rebuild.web.admin.robot;

import com.rebuild.server.helper.SMSender;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/admin/robot/trigger/")
public class SendNotificationControll extends BaseControll {

    @RequestMapping("sendnotification-atypes")
    public void availableTypes(HttpServletResponse response) throws IOException {
        Map<String, Boolean> ta = new HashMap<>();
        ta.put("serviceMail", SMSender.availableMail());
        ta.put("serviceSms", SMSender.availableSMS());
        writeSuccess(response, ta);
    }
}
