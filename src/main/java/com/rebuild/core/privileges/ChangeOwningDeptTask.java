/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.task.HeavyTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.text.MessageFormat;

/**
 * 用户变更部门后，该用户的业务记录中的所属部门也需要变更
 *
 * @author devezhao
 * @since 12/29/2018
 */
@Slf4j
public class ChangeOwningDeptTask extends HeavyTask<Integer> {

    final private ID user;
    final private ID deptNew;

    /**
     * @param user
     * @param deptNew
     */
    protected ChangeOwningDeptTask(ID user, ID deptNew) {
        this.user = user;
        this.deptNew = deptNew;
    }

    @Override
    protected Integer exec() {
        log.info("Start modifying the `OwningDept` ... " + this.user);
        this.setTotal(MetadataHelper.getEntities().length);

        final StopWatch sw = new StopWatch("ChangeOwningDeptTask");

        final String updeptSql = String.format(
                "update `{0}` set `{1}` = ''%s'' where `{2}` = ''%s''", deptNew.toLiteral(), user.toLiteral());
        int changed = 0;
        for (Entity e : MetadataHelper.getEntities()) {
            if (this.isInterrupt()) {
                this.setInterrupted();
                log.error("Task interrupted : " + user + " > " + deptNew);
                break;
            }
            if (!MetadataHelper.hasPrivilegesField(e)) {
                this.addCompleted();
                continue;
            }

            sw.start(e.getName());
            String sql = MessageFormat.format(updeptSql,
                    e.getPhysicalName(),
                    e.getField(EntityHelper.OwningDept).getPhysicalName(),
                    e.getField(EntityHelper.OwningUser).getPhysicalName());
            Application.getSqlExecutor().execute(sql, 600);
            this.addCompleted();
            changed++;
            sw.stop();
        }

        log.info("Modify the `OwningDept` to complete : {} > {}\n{}", this.user, changed, sw.prettyPrint());
        return changed;
    }
}
