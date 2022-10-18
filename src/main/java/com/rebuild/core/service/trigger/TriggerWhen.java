/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import com.rebuild.core.service.general.EntityService;

/**
 * 触发动作定义
 *
 * 动作传导：
 * 1. 主记录删除/审批/撤销审批会传导至明细
 * 2. 主记录更新（仅分组聚合）会传导至明细
 *
 * @author devezhao zhaofang123@gmail.com
 * @see BizzPermission
 * @since 2019/05/23
 */
public enum TriggerWhen {

    /**
     * 创建时
     */
    CREATE(BizzPermission.CREATE.getMask()),
    /**
     * 删除时
     */
    DELETE(BizzPermission.DELETE.getMask()),
    /**
     * 更新时
     */
    UPDATE(BizzPermission.UPDATE.getMask()),
    /**
     * 分派时
     */
    ASSIGN(BizzPermission.ASSIGN.getMask()),
    /**
     * 共享时
     */
    SHARE(BizzPermission.SHARE.getMask()),
    /**
     * 取消共享时
     */
    UNSHARE(EntityService.UNSHARE.getMask()),
    /**
     * 审批通过
     */
    APPROVED(128),
    /**
     * 审批撤销
     */
    REVOKED(256),
    /**
     * 定时
     */
    TIMER(512),

    /**
     * 审批提交
     */
    SUBMIT(1024),
    /**
     * 审批驳回/撤回
     */
    REJECTED(2048),

    ;

    private final int maskValue;

    TriggerWhen(int maskValue) {
        this.maskValue = maskValue;
    }

    public int getMaskValue() {
        return maskValue;
    }
}
