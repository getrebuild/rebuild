/*!
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
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
        items.add(buildItem(Language.L("通用配置"), "admin/systems", 0));
        items.add(buildItem(Language.L("邮件与短信"), "admin/integration/submail", 0));
        items.add(buildItem(Language.L("云存储"), "admin/integration/storage", 0));
        return RespBody.ok(items);
    }

    @GetMapping("usermrg")
    public RespBody featUsermrg() {
        List<JSON> items = new ArrayList<>();
        items.add(buildItem(Language.L("管理用户"), "admin/bizuser/users", Application.getUserStore().getAllUsers().length - 1));
        items.add(buildItem(Language.L("管理部门"), "admin/bizuser/departments", Application.getUserStore().getAllDepartments().length));
        items.add(buildItem(Language.L("管理角色"), "admin/bizuser/role-privileges", Application.getUserStore().getAllRoles().length));
        items.add(buildItem(Language.L("管理团队"), "admin/bizuser/teams", Application.getUserStore().getAllTeams().length));
        return RespBody.ok(items);
    }

    @GetMapping("entitymrg")
    public RespBody featEntityMrg() {
        List<JSON> items = new ArrayList<>();
        items.add(buildItem(Language.L("业务实体"), "admin/entities", MetadataSorter.sortEntities().length - 4));
        items.add(buildItem(Language.L("审批流程"), "admin/robot/approvals", count(EntityHelper.RobotApprovalConfig)));
        items.add(buildItem(Language.L("触发器"), "admin/robot/triggers", count(EntityHelper.RobotTriggerConfig)));
        items.add(buildItem(Language.L("数据导入"), "admin/data/data-imports", -1));
        items.add(buildItem(Language.L("报表模板"), "admin/data/report-templates", count(EntityHelper.DataReportConfig)));
        return RespBody.ok(items);
    }

    @GetMapping("others")
    public RespBody featOthers() {
        List<JSON> items = new ArrayList<>();
        items.add(buildItem(Language.L("企业微信集成"), "admin/integration/wxwork", 0));
        items.add(buildItem(Language.L("或钉钉集成"), "admin/integration/dingtalk", 0));
        items.add(buildItem(Language.L("Open Api"), "admin/apis-manager", count(EntityHelper.RebuildApi)));
        items.add(buildItem(Language.L("项目管理"), "admin/projects", count(EntityHelper.ProjectConfig)));
        return RespBody.ok(items);
    }

    private JSONObject buildItem(String item, String url, int num) {
        JSONObject o = JSONUtils.toJSONObject(
                new String[] { "item", "url", "confirm", "num" },
                new Object[] { item, url, false, num });

        final String key = "Guide-" + url;
        Object confirm = KVStorage.getCustomValue(key);
        if (confirm != null) o.put("confirm", true);
        return o;
    }

    private int count(int entityCode) {
        Entity entity = MetadataHelper.getEntity(entityCode);
        String sql = String.format("select count(%s) from %s", entity.getPrimaryField().getName(), entity.getName());
        Object[] c = Application.createQueryNoFilter(sql).unique();
        return ObjectUtils.toInt(c == null ? 0 : c[0]);
    }

    @PostMapping("confirm")
    public RespBody confirmItem(HttpServletRequest request) {
        String url = getParameterNotNull(request, "url");

        final String key = "Guide-" + url;
        KVStorage.setCustomValue(key, CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
        return RespBody.ok();
    }

    @PostMapping("show-naver")
    public RespBody showNaver(HttpServletRequest request) {
        boolean s = getBoolParameter(request, "s");
        KVStorage.setCustomValue("GuideShowNaver", s);
        return RespBody.ok();
    }
}
