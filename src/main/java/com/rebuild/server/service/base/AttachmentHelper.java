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

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.metadata.EntityHelper;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * TODO
 *
 * @author ZHAO
 * @since 2019/11/13
 */
public class AttachmentHelper {

    private static final LRUMap FILESIZES = new LRUMap(2000);

    /**
     * @param filePath
     * @param user
     * @return
     */
    public static Record createAttachment(String filePath, ID user) {
        Record attach = EntityHelper.forNew(EntityHelper.Attachment, user);
        attach.setString("filePath", filePath);
        String ext = FilenameUtils.getExtension(filePath);
        if (StringUtils.isNotBlank(ext)) {
            attach.setString("fileType", ext);
        }
        if (FILESIZES.containsKey(filePath)) {
            attach.setInt("fileSize", (int) FILESIZES.remove(filePath));
        }
        return attach;
    }

    /**
     * 设置文件大小，以便在创建文件记录时使用
     *
     * @param filePath
     * @param fileSize in bytes
     *
     * @see #createAttachment(String, ID)
     */
    public static void storeFileSize(String filePath, int fileSize) {
        FILESIZES.put(filePath, fileSize);
    }
}
