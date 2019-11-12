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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.utils.JSONUtils;

/**
 * TODO
 *
 * @author ZHAO
 * @since 2019/11/12
 */
public class FilesHelper {

    /**
     * @param user
     * @return
     */
    public static JSONArray getFolders(ID user) {
        return getFolders(user, null);
    }

    /**
     * @param user
     * @return
     */
    public static JSONArray getFolders(ID user, ID parent) {
        String sql = "select folderId,name from AttachmentFolder where parent ";
        if (parent == null) {
            sql += "is null";
        } else {
            sql += "= '" + parent + "'";
        }
        Object[][] array = Application.createQueryNoFilter(sql).array();

        JSONArray folders = new JSONArray();
        for (Object[] o : array) {
            JSONObject folder = JSONUtils.toJSONObject(new String[] { "id", "text" }, o);
            JSONArray children = getFolders(user, (ID) o[0]);
            if (children != null && !children.isEmpty()) {
                folder.put("children", children);
            }
            folders.add(folder);
        }
        return folders;
    }
}
