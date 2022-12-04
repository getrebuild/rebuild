/*!
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin (RB)
 * @since 12/04/2022
 */
@RestController
@RequestMapping("/common/guide/")
public class RebuildGuideController extends BaseController {

    @GetMapping("syscfg")
    public RespBody featSyscfg() {
        List<JSON> items = new ArrayList<>();
        items.add(buildItem(Language.L("系统配置"), "admin/systems"));
        items.add(buildItem(Language.L("管理用户"), "admin/bizuser/users"));
        items.add(buildItem(Language.L("管理部门"), "admin/bizuser/departments"));
        items.add(buildItem(Language.L("管理角色"), "admin/bizuser/privileges"));
        return RespBody.ok(items);
    }

    private JSONObject buildItem(String item, String url) {
        JSONObject i = JSONUtils.toJSONObject(
                new String[] { "item", "url", "confirm" },
                new Object[] { item, url, false });

        final String key = "Guide-" + url;
        Object s = KVStorage.getCustomValue(key);
        if (s != null) i.put("confirm", ObjectUtils.toBool(s));
        return i;
    }

    @PostMapping("confirm")
    public RespBody confirmItem(HttpServletRequest request) {
        String url = getParameterNotNull(request, "url");

        final String key = "Guide-" + url;
        KVStorage.setCustomValue(key, CalendarUtils.now());
        return RespBody.ok();
    }
}
