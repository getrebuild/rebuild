/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.recyclebin;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/08/21
 */
public class RecycleRestoreTest extends TestSupportWithUser {

    @Test
    public void restore() {
        ID testId = addRecordOfTestAllFields();
        Application.getGeneralEntityService().delete(testId);

        // Is in?
        Object[] recycle = Application.createQueryNoFilter(
                "select recycleId from RecycleBin where recordId = ?")
                .setParameter(1, testId)
                .unique();
        Assert.assertNotNull(recycle);

        int a = new RecycleRestore((ID) recycle[0]).restore();
        Assert.assertEquals(1, a);
    }
}