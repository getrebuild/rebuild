/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

import static com.rebuild.utils.CommonsUtils.escapeSql;

/**
 * @author devezhao
 * @since 2026/4/15
 */
@RestController
@RequestMapping("/admin/commons-config/")
public class CommonsConfigController extends BaseController {

    @GetMapping("list")
    public RespBody list(HttpServletRequest request) {
        String entity = getParameter(request, "entity");
        String type = getParameterNotNull(request, "type");
        String q = getParameter(request, "q");

        String sql = "select configId,name,config, modifiedOn from CommonsConfig where type=?";
        if (StringUtils.isNotBlank(entity)) sql += String.format(" and belongEntity = '%s'", entity);
        if (StringUtils.isNotBlank(q)) sql += String.format(" and name like '%%%s%%'", escapeSql(q));
        sql += " order by modifiedOn desc";

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, type)
                .setLimit(2000)
                .array();

        JSONArray res = new JSONArray();
        for (Object[] o : array) {
            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "name", "modifiedOn"},
                    new Object[]{o[0], o[1], I18nUtils.formatDate((Date) o[3])});

            String conf = (String) o[2];
            if (JSONUtils.wellFormat(conf)) {
                item.put("config", JSONUtils.parseObjectSafe(conf));
            }
            res.add(item);
        }
        return RespBody.ok(res);
    }
}
