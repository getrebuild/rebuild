/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import com.rebuild.TestSupport;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/10/29
 */
public class ChangeOwningDeptTaskTest extends TestSupport {

    @Test
    public void exec() {
        new ChangeOwningDeptTask(SIMPLE_USER, DepartmentService.ROOT_DEPT).exec();
    }
}