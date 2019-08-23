/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin.audit;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.business.recyclebin.RecycleRestore;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO 回收站
 *
 * @author ZHAO
 * @since 2019-08-20
 */
@Controller
@RequestMapping("/admin/audit/")
public class RecycleBinControll extends BasePageControll {

    @RequestMapping("recycle-bin")
    public ModelAndView pageLogging(HttpServletRequest request) throws IOException {
        return createModelAndView("/admin/audit/recycle-bin.jsp");
    }

    @RequestMapping("recycle-bin/restore")
    public void dataList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean cascade = getBoolParameter(request, "cascade");
        String ids = getParameterNotNull(request, "ids");

        String lastError = null;
        int restored = 0;
        for (String id : ids.split(",")) {
            if (!ID.isId(id)) {
                continue;
            }

            try {
                int a = new RecycleRestore(ID.valueOf(id)).restore(cascade);
                restored += a;
            } catch (Exception ex) {
                // 出现错误就跳出
                LOG.error("Restore record failed : " + id, ex);
                lastError = ex.getLocalizedMessage();
                break;
            }
        }

        if (lastError != null && restored == 0) {
            writeFailure(response, lastError);
        } else {
            writeSuccess(response, JSONUtils.toJSONObject("restored", restored));
        }
    }
}
