/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    private static final LRUMap<String, Integer> FILESIZES = new LRUMap<>(2000);

    /**
     * 暂存文件大小，以便在创建文件记录时使用
     *
     * @param filePath
     * @param fileSize in bytes
     * @see #createAttachment(String, ID)
     */
    public static void storeFileSize(String filePath, int fileSize) {
        FILESIZES.put(filePath, fileSize);
    }

    /**
     * Create Record of Attachment
     *
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
            attach.setInt("fileSize", FILESIZES.remove(filePath));
        } else {
            Object[] db = Application.createQueryNoFilter("select fileSize from Attachment where filePath = ?")
                    .setParameter(1, filePath)
                    .unique();
            if (db != null) {
                attach.setInt("fileSize", (Integer) db[0]);
            }
        }

        return attach;
    }

    /**
     * 获取可用目录，可指定父级目录
     *
     * @param user
     * @param parent
     * @return
     */
    public static JSONArray getFolders(ID user, ID parent) {
        String sql = String.format(
                "select folderId,name,scope,createdBy,parent from AttachmentFolder" +
                " where (scope = '%s' or createdBy = '%s') and ", SCOPE_ALL, user);
        if (parent == null) sql += "parent is null";
        else sql += String.format("parent = '%s'", parent);

        sql += " order by name";
        Object[][] array = Application.createQueryNoFilter(sql).array();

        JSONArray folders = new JSONArray();
        for (Object[] o : array) {
            o[2] = SCOPE_SELF.equalsIgnoreCase((String) o[2]);
            o[3] = user.equals(o[3]);
            JSONObject folder = JSONUtils.toJSONObject(
                    new String[] { "id", "text", "private", "self", "parent" }, o);
            
            JSONArray children = getFolders(user, (ID) o[0]);
            if (!children.isEmpty()) {
                folder.put("children", children);
            }
            folders.add(folder);
        }
        return folders;
    }

    /**
     * 获取所有私有目录 ID
     *
     * @param excludesUser 排除指定用户
     * @return
     */
    public static Set<ID> getPrivateFolders(ID excludesUser) {
        Object[][] array = Application.createQueryNoFilter(
                "select folderId,createdBy from AttachmentFolder where scope = ?")
                .setParameter(1, SCOPE_SELF)
                .array();
        if (array.length == 0) return Collections.emptySet();

        Set<ID> set = new HashSet<>();
        for (Object[] o : array) {
            if (excludesUser == null || !excludesUser.equals(o[1])) {
                set.add((ID) o[0]);
                set.addAll(getChildFolders((ID) o[0]));
            }
        }
        return set;
    }

    /**
     * 获取子目录
     *
     * @param parent
     * @return
     */
    public static Set<ID> getChildFolders(ID parent) {
        Object[][] array = Application.createQueryNoFilter(
                "select folderId,createdBy from AttachmentFolder where parent = ?")
                .setParameter(1, parent)
                .array();
        if (array.length == 0) return Collections.emptySet();

        Set<ID> set = new HashSet<>();
        for (Object[] o : array) {
            set.add((ID) o[0]);
            set.addAll(getChildFolders((ID) o[0]));
        }
        return set;
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

        if (SCOPE_SELF.equals(o[0])) return true;
        else if (o[1] != null) return isPrivate((ID) o[1]);
        else return false;
    }

    /**
     * 是否允许操作文件（管理员与创建人允许）
     *
     * @param user
     * @param fileId
     * @return
     */
    public static boolean isManageable(ID user, ID fileId) {
        return UserHelper.isAdmin(user) || UserHelper.isSelf(user, fileId);
    }
}
