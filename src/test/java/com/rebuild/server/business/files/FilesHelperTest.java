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

package com.rebuild.server.business.files;

import cn.devezhao.persist4j.Record;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2019/11/15
 */
public class FilesHelperTest extends TestSupportWithUser {

    @Test
    public void getFolders() {
        Record folder = EntityHelper.forNew(EntityHelper.AttachmentFolder, getSessionUser());
        folder.setString("name", "123456");
        folder = Application.getService(EntityHelper.AttachmentFolder).create(folder);

        System.out.println(FilesHelper.getFolders(getSessionUser()));
        Application.getService(EntityHelper.AttachmentFolder).delete(folder.getPrimary());
    }

    @Test
    public void getPrivateFolders() {
        Record folder = EntityHelper.forNew(EntityHelper.AttachmentFolder, getSessionUser());
        folder.setString("name", "abcdef");
        folder.setString("scope", FilesHelper.SCOPE_SELF);
        folder = Application.getService(EntityHelper.AttachmentFolder).create(folder);

        System.out.println(FilesHelper.getPrivateFolders(getSessionUser()));
        Application.getService(EntityHelper.AttachmentFolder).delete(folder.getPrimary());
    }
}