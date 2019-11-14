/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.SystemEntityService;

/**
 * 文件目录
 *
 * @author devezhao
 * @since 2019/11/14
 */
public class AttachmentFolderService extends SystemEntityService {

    protected AttachmentFolderService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.AttachmentFolder;
    }

    @Override
    public int delete(ID recordId) {
        Object inFolder = Application.createQueryNoFilter(
                "select inFolder from Attachment where inFolder = ?")
                .setParameter(1, recordId)
                .unique();
        if (inFolder != null) {
            throw new DataSpecificationException("目录内有文件不能删除");
        }
        Object parent = Application.createQueryNoFilter(
                "select parent from AttachmentFolder where parent = ?")
                .setParameter(1, recordId)
                .unique();
        if (parent != null) {
            throw new DataSpecificationException("目录内有子目录不能删除");
        }

        return super.delete(recordId);
    }
}
