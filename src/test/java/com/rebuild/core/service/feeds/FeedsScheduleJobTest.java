/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/2/27
 */
public class FeedsScheduleJobTest extends TestSupport {

    @Test
    public void doInternal() {
        String contentMore = "{ scheduleRemind:3 }";
        Object[][] array = new Object[][]{
                new Object[]{
                        SIMPLE_USER, ID.newId(EntityHelper.Feeds), CodecUtils.randomCode(100), contentMore
                },
                new Object[]{
                        SIMPLE_USER, ID.newId(EntityHelper.Feeds), CodecUtils.randomCode(60), contentMore
                },
                new Object[]{
                        UserService.ADMIN_USER, ID.newId(EntityHelper.Feeds), CodecUtils.randomCode(60), contentMore
                }
        };
        new FeedsScheduleJob().doInternal(array);
    }
}