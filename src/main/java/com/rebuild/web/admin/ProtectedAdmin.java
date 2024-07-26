/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.CommandArgs;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 07/27/2024
 */
public class ProtectedAdmin {

    private static PaEntry[] PA;

    /**
     * @param uriOrKey
     * @param adminUser
     * @return
     */
    public static boolean allow(String uriOrKey, ID adminUser) {
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

    // TODO
    enum PaEntry {
        // 通用配置
        SYS("/systems"),
        // 服务集成*
        SSI("/integration/"),
        // 实体管理* (含分类)
        ENT("/entities;/entity/;/metadata/"),
        // 审批流程
        APR(""),
        // 记录转换
        TRA(""),
        // 触发器
        TRI(""),
        // 业务进度
        SOP(""),
        // 报表设计
        REP(""),
        // 数据导入
        IMP(""),
        // 外部表单
        EXF(""),
        // 项目
        PRO(""),
        // FrontJS
        FJS(""),
        // 用户*
        USR(""),
        // 角色
        ROL(""),
        // 团队
        TEM(""),
        // 登录日志
        LLG(""),
        // 变更历史
        REV(""),
        // 回收站
        RCY(""),

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
            // URL
            for (String p : paths) {
                if (uriOrKey.contains(p)) return true;
            }
            return false;
        }
    }
}
