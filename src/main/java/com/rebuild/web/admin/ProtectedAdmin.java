/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.License;
import com.rebuild.utils.AppUtils;
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
            String vv = CommandArgs.getString(CommandArgs._ProtectedAdmin);
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

    // 功能
    enum PaEntry {
        // 通用配置
        SYS("/systems"),
        // 服务集成*
        SSI("/integration/"),
        // API
        API("/apis-manager"),
        // 实体管理* (含分类)
        ENT("/entities;/entity/;/metadata/"),
        // 审批流程
        APR("/robot/approval"),
        // 记录转换
        TRA("/robot/transform"),
        // 触发器
        TRI("/robot/trigger"),
        // 业务进度
        SOP("/robot/sop"),
        // 报表设计
        REP("/data/report-template"),
        // 数据导入
        IMP("/data/data-imports"),
        // 外部表单
        EXF("/extform"),
        // 项目
        PRO("/project"),
        // FrontJS
        FJS("/frontjs-code"),
        // 智能扫码
        SCA("/easy-scan"),
        // 用户*
        USR("/bizuser/users;bizuser/departments"),
        // 角色
        ROL("/bizuser/role-privileges"),
        // 团队
        TEM("/bizuser/teams"),
        // 登录日志
        LLG("/audit/login-logs"),
        // 变更历史
        REV("/audit/revision-history"),
        // 回收站
        RCY("/audit/recycle-bin"),

        ;

        final private String[] paths;
        PaEntry(String paths) {
            this.paths = paths.split(";");
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
