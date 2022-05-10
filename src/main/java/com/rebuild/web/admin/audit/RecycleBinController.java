/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.audit;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.general.recyclebin.RecycleRestore;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 回收站
 *
 * @author ZHAO
 * @since 2019-08-20
 */
@Slf4j
@Controller
@RequestMapping("/admin/audit/")
public class RecycleBinController extends BaseController {

    @GetMapping("recycle-bin")
    public ModelAndView page() {
        return createModelAndView("/admin/audit/recycle-bin");
    }

    @RequestMapping("recycle-bin/restore")
    public void dataList(HttpServletRequest request, HttpServletResponse response) {
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
                log.error("Restore record failed : " + id, ex);
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
