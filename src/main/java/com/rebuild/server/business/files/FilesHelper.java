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

package com.rebuild.server.business.files;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 文件&附件帮助类
 *
 * @author ZHAO
 * @since 2019/11/12
 */
public class FilesHelper {

    // 公共
    public static final String SCOPE_ALL = "ALL";
    // 私有
    public static final String SCOPE_SELF = "SELF";

    private static final LRUMap FILESIZES = new LRUMap(2000);

    /**
     * 暂存文件大小，以便在创建文件记录时使用
     *
     * @param filePath
     * @param fileSize in bytes
     *
     * @see #createAttachment(String, ID)
     */
    public static void storeFileSize(String filePath, int fileSize) {
        FILESIZES.put(filePath, fileSize);
    }

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
     * 获取目录
     *
     * @param user
     * @return
     */
    public static JSONArray getFolders(ID user) {
        return getFolders(user, null);
    }

    /**
     * 获取目录，可指定父级目录
     *
     * @param user
     * @param parent
     * @return
     */
    public static JSONArray getFolders(ID user, ID parent) {
        String sql = "select folderId,name,scope,createdBy,parent from AttachmentFolder" +
                " where (scope = 'ALL' or (createdBy = ? and scope = 'SELF')) and ";
        if (parent == null) {
            sql += "parent is null";
        } else {
            sql += String.format("parent = '%s'", parent);
        }
        Object[][] array = Application.createQueryNoFilter(sql).setParameter(1, user).array();

        JSONArray folders = new JSONArray();
        for (Object[] o : array) {
            o[2] = SCOPE_SELF.equalsIgnoreCase((String) o[2]);
            o[3] = user.equals(o[3]);
            JSONObject folder = JSONUtils.toJSONObject(
                    new String[] { "id", "text", "private", "self", "parent" }, o);

            JSONArray children = getFolders(user, (ID) o[0]);
            if (children != null && !children.isEmpty()) {
                folder.put("children", children);
            }
            folders.add(folder);
        }
        return folders;
    }

    /**
     * 是否私有目录。如果父级是私有，那么子目录也是私有
     *
     * @param folder
     * @return
     */
    public static boolean isPrivate(ID folder) {
        Object[] o = Application.createQueryNoFilter(
                "select scope,parent from AttachmentFolder where folderId = ?")
                .setParameter(1, folder)
                .unique();
        if (SCOPE_SELF.equals(o[0])) {
            return true;
        } else if (o[1] != null) {
            return isPrivate((ID) o[1]);
        } else {
            return false;
        }
    }
}
