/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.support.task.TaskExecutors;

/**
 * 业务实体用
 *
 * @author devezhao
 * @since 12/28/2018
 */
public interface EntityService extends ServiceSpec {

    /**
     * 取消共享，跟随共享权限
     *
     * @see BizzPermission
     */
    Permission UNSHARE = new BizzPermission("UNSHARE", 1 << 6, true);

    /**
     * 删除
     *
     * @param record
     * @param cascades 需要级联删除的实体
     * @return
     */
    int delete(ID record, String[] cascades);

    /**
     * 分派
     *
     * @param record
     * @param to
     * @param cascades 需要级联分派的实体
     * @return
     */
    int assign(ID record, ID to, String[] cascades);

    /**
     * 共享
     *
     * @param record
     * @param to
     * @param cascades 需要级联分派的实体
     * @return
     */
    int share(ID record, ID to, String[] cascades);

    /**
     * 取消共享
     *
     * @param record   主记录
     * @param accessId 共享的 AccessID
     * @return
     */
    int unshare(ID record, ID accessId);

    /**
     * 批量大操作
     *
     * @param context
     * @return
     */
    int bulk(BulkContext context);

    /**
     * 批量大操作（异步）
     *
     * @param context
     * @return 任务 ID
     * @see TaskExecutors
     */
    String bulkAsync(BulkContext context);
}
