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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
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
    public static JSONArray getAccessableFolders(ID user, ID parent) {
        // NOTE 缓存加速
        String sql = "select folderId,name,scope,createdBy,parent,scope from AttachmentFolder where ";
        if (parent == null) sql += "parent is null";
        else sql += String.format("parent = '%s'", parent);

        sql += " order by name";
        Object[][] array = Application.createQueryNoFilter(sql).array();

        JSONArray folders = new JSONArray();
        for (Object[] o : array) {
            boolean access = SCOPE_ALL.equals(o[2]) || user.equals(o[3]);
            final String scopeSpecUsers = o[2] != null && o[2].toString().length() >= 20 ? o[2].toString() : null;
            // v3.3 ID
            if (!access && scopeSpecUsers != null) {
                Collection<String> c = new HashSet<>();
                CollectionUtils.addAll(c, scopeSpecUsers.split(","));
                Set<ID> inUsers = UserHelper.parseUsers(c, null);
                access = inUsers.contains(user);
            }

            // 如果不可访问，子级目录即使公开也不可访问
            if (!access) continue;

            o[2] = SCOPE_SELF.equals(o[2]);
            o[3] = user.equals(o[3]);
            o[5] = scopeSpecUsers;
            JSONObject folder = JSONUtils.toJSONObject(
                    new String[] { "id", "text", "private", "self", "parent", "specUsers" }, o);

            JSONArray children = getAccessableFolders(user, (ID) o[0]);
            if (!children.isEmpty()) {
                folder.put("children", children);
            }
            folders.add(folder);
        }
        return folders;
    }

    /**
     * 获取可用目录
     *
     * @param user 指定用户
     * @return
     */
    public static Set<ID> getAccessableFolders(ID user) {
        JSONArray accessable = getAccessableFolders(user, null);
        Set<ID> set = new HashSet<>();
        intoAccessableFolders(accessable, set);
        return set;
    }

    private static void intoAccessableFolders(JSONArray folders, Set<ID> into) {
        for (Object o : folders) {
            JSONObject folder = (JSONObject) o;
            into.add(ID.valueOf(folder.getString("id")));

            JSONArray c = folder.getJSONArray("children");
            if (c != null) intoAccessableFolders(c, into);
        }
    }

    /**
     * 获取所有子级目录（注意无权限控制）
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
     * 是否允许操作文件（管理员与创建人允许）
     *
     * @param user
     * @param fileId
     * @return
     */
    public static boolean isFileManageable(ID user, ID fileId) {
        return UserHelper.isAdmin(user) || UserHelper.isSelf(user, fileId);
    }

    /**
     * 是否允许访问文件
     *
     * @param user
     * @param fileId
     * @return
     */
    public static boolean isFileAccessable(ID user, ID fileId) {
        Object[] o = Application.getQueryFactory().uniqueNoFilter(fileId, "folderId");
        if (o == null) return true;
        return getAccessableFolders(user).contains((ID) o[0]);
    }
}
