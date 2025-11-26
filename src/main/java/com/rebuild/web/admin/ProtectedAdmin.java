/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * -D_ProtectedAdmin=USR;ROL
 *
 * @author Zixin
 * @since 07/27/2024
 */
public class ProtectedAdmin {

    private static PaEntry[] PA;

    /**
     * @param uriOrKey
     * @param request
     * @return
     */
    public static boolean allow(String uriOrKey, HttpServletRequest request) {
        if (Application.devMode()) PaEntry.valueOf(uriOrKey);  // check
        return allow(uriOrKey, AppUtils.getRequestUser(request));
    }

    /**
     * @param uriOrKey
     * @param adminUser
     * @return
     */
    public static boolean allow(String uriOrKey, ID adminUser) {
        if (!License.isRbvAttached()) return true;
        if (UserService.ADMIN_USER.equals(adminUser)) return true;

        if (PA == null) {
            String vv = RebuildConfiguration.get(ConfigurationItem.ProtectedAdmin);
            if (StringUtils.isBlank(vv)) PA = new PaEntry[0];
            else {
                List<PaEntry> pp = new ArrayList<>();
                for (String v : vv.split("[,;]")) {
                    pp.add(PaEntry.valueOf(v));
                }
                PA = pp.toArray(new PaEntry[0]);
            }
        }
        if (PA.length == 0) return true;

        for (PaEntry p : PA) {
            if (p.matches(uriOrKey)) return true;
        }
        return false;
    }

    /**
     * 缓存清理
     */
    public static void clean() {
        // unsafe
        PA = null;
    }

    // --

    /**
     * 功能列表
     */
    public enum PaEntry {
        SYS("/systems", "通用配置"),
        SSI("/integration/", "服务集成"),
        API("/apis-manager", "OpenAPI 秘钥"),
        AIB("/integration/aibot", "AI 助手"),
        ENT("/entities;/entity/;/metadata/", "实体管理"),
        APR("/robot/approval", "审批流程"),
        TRA("/robot/transform", "记录转换"),
        TRI("/robot/trigger", "触发器"),
        SOP("/robot/sop", "业务进度"),
        REP("/data/report-template", "报表模版"),
        EXF("/extform", "外部表单"),
        IMP("/data/data-imports", "数据导入"),
        SYN("/data/data-syncer", "数据同步"),
        PRO("/project", "项目"),
        FJS("/frontjs-code", "FrontJS"),
        I18("/i18n/editor", "多语言"),
        USR("/bizuser/users;/bizuser/departments", "部门用户"),
        ROL("/bizuser/role-privileges;/bizuser/role", "角色权限"),
        TEM("/bizuser/teams", "团队"),
        LLG("/audit/login-logs", "登录日志"),
        REV("/audit/revision-history", "变更历史"),
        RCY("/audit/recycle-bin", "回收站"),

        ;

        final private String[] paths;
        @Getter
        final private String name;
        PaEntry(String paths, String name) {
            this.paths = paths.split(";");
            this.name = name;
        }

        /**
         * @param uriOrKey
         * @return
         */
        public boolean matches(String uriOrKey) {
            if (name().equals(uriOrKey)) return true;
            if (!uriOrKey.contains("/admin/")) return false;

            // URL
            uriOrKey = "/" + uriOrKey.split("/admin/")[1];
            for (String p : paths) {
                if (uriOrKey.startsWith(p)) return true;
            }
            return false;
        }
    }
}
