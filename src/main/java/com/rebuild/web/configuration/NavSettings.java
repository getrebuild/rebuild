/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.NavManager;
import com.rebuild.core.configuration.general.BaseLayoutManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.RoleService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

/**
 * 导航菜单设置
 *
 * @author Zixin (RB)
 * @since 09/19/2018
 */
@RestController
@RequestMapping("/app/settings/")
public class NavSettings extends BaseController implements ShareTo {

    @PostMapping("nav-settings")
    public RespBody sets(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomNav),
                Language.L("无操作权限"));

        ID cfgid = getIdParameter(request, "id");
        // 普通用户只能有一个
        if (cfgid != null && !UserHelper.isSelf(user, cfgid)) {
            ID useNav = NavManager.instance.detectUseConfig(user, null, NavManager.TYPE_NAV);
            if (useNav != null && UserHelper.isSelf(user, useNav)) {
                cfgid = useNav;
            } else {
                cfgid = null;
            }
        }

        JSON config = ServletUtils.getRequestJson(request);

        Record record;
        if (cfgid == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", "N");
            record.setString("applyType", BaseLayoutManager.TYPE_NAV);
            record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
        } else {
            record = EntityHelper.forUpdate(cfgid, user);
        }
        record.setString("config", config.toJSONString());
        putCommonsFields(request, record);
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        return RespBody.ok();
    }

    @GetMapping("nav-settings")
    public RespBody gets(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String cfgid = request.getParameter("id");

        // 管理员才可以新建（多个）
        if ("NEW".equalsIgnoreCase(cfgid)) {
            return RespBody.ok();
        } else if (ID.isId(cfgid)) {
            return RespBody.ok(NavManager.instance.getNavLayoutById(ID.valueOf(cfgid)));
        } else {
            return RespBody.ok(NavManager.instance.getNavLayout(user));
        }
    }

    @GetMapping("nav-settings/alist")
    public RespBody getsList(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        String sql = "select configId,configName,shareTo,createdBy from LayoutConfig where ";
        if (UserHelper.isAdmin(user)) {
            sql += String.format("applyType = '%s' and createdBy.roleId = '%s' order by configName",
                    NavManager.TYPE_NAV, RoleService.ADMIN_ROLE);
        } else {
            // 普通用户可用的
            ID[] uses = NavManager.instance.getUsesNavId(user);
            sql += "configId in ('" + StringUtils.join(uses, "', '") + "')";
        }

        Object[][] alist = Application.createQueryNoFilter(sql).array();
        return RespBody.ok(alist);
    }

    @GetMapping("nav-settings/topnav")
    public RespBody getsTopNav() {
        String s = KVStorage.getCustomValue("TopNav32");
        return RespBody.ok(s == null ? null : JSON.parseArray(s));
    }

    @PostMapping("nav-settings/topnav")
    public RespBody setsTopNav(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        if (!UserHelper.isAdmin(user)) return RespBody.error();

        JSONArray sets = (JSONArray) ServletUtils.getRequestJson(request);
        // v34 修改名称

        for (Iterator<Object> iter = sets.iterator(); iter.hasNext(); ) {
            JSONArray item = (JSONArray) iter.next();
            String newName = item.getString(2);
            if (StringUtils.isNotBlank(newName)) {
                Record record = EntityHelper.forUpdate(ID.valueOf(item.getString(0)), user);
                record.setString("configName", newName);
                Application.getBean(LayoutConfigService.class).update(record);
            }

            // 不显示
            if (!item.getBooleanValue(3)) {
                iter.remove();
            }
        }

        KVStorage.setCustomValue("TopNav32", sets.toJSONString());
        return RespBody.ok();
    }

    @PostMapping("nav-settings/nav-copyto")
    public RespBody navCopyTo(@RequestBody JSONObject post, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        if (!UserHelper.isAdmin(user)) return RespBody.error();

        final ID from  = ID.valueOf(post.getString("from"));
        final JSON config = NavManager.instance.getLayoutById(from).getJSON("config");

        for (Object s : post.getJSONArray("copyTo")) {
            ID to = ID.isId(s) ? ID.valueOf(s.toString()) : null;
            if (to == null || from.equals(to)) continue;

            Record record = EntityHelper.forUpdate(to, user);
            record.setString("config", config.toJSONString());
            Application.getBean(LayoutConfigService.class).createOrUpdate(record);
        }

        return RespBody.ok();
    }
}
