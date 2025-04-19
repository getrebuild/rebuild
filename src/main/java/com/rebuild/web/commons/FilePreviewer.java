/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.OnlyOffice;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import static com.rebuild.core.support.ConfigurationItem.OnlyofficeServer;

/**
 * 文档预览
 *
 * @author devezhao
 * @see FileDownloader
 * @since 04/07/2025
 */
@Slf4j
@Controller
public class FilePreviewer extends BaseController {

    @GetMapping("/commons/file-preview")
    public ModelAndView ooPreview(HttpServletRequest request) {
        String src = getParameterNotNull(request, "src");
        Object[] ps = OnlyOffice.buildPreviewParams(src);

        ModelAndView mv = createModelAndView("/common/oo-preview");
        mv.getModel().put(OnlyofficeServer.name(), OnlyOffice.getOoServer());
        mv.getModel().put("_DocumentConfig", ps[0]);
        mv.getModel().put("_Token", ps[1]);

        String[] user = new String[]{"REBUILD", "REBUILD"};
        ID userid = AppUtils.getRequestUser(request);
        if (userid != null) {
            user = new String[]{userid.toString(), UserHelper.getName(userid)};
        }
        mv.getModel().put("_User",
                JSONUtils.toJSONObject(new String[]{"id", "name"}, user));

        return mv;
    }
}
