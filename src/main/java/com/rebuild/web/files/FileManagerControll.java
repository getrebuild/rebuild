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

package com.rebuild.web.files;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.business.files.FilesHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/11/12
 */
@Controller
@RequestMapping("/files/")
public class FileManagerControll extends BaseControll {

    @RequestMapping("post-files")
    public void postFiles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ID user = getRequestUser(request);
        ID inFolder = getIdParameter(request, "folder");
        JSONArray files = (JSONArray) ServletUtils.getRequestJson(request);

        List<Record> fileRecords = new ArrayList<>();
        for (Object o : files) {
            Record r = FilesHelper.createAttachment((String) o, user);
            if (inFolder != null) {
                r.setID("inFolder", inFolder);
            }
            fileRecords.add(r);
        }
        Application.getCommonService().createOrUpdate(fileRecords.toArray(new Record[0]), false);

        writeSuccess(response);
    }

    @RequestMapping("delete-files")
    public void deleteFiles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ID user = getRequestUser(request);
        String[] files = getParameter(request, "ids", "").split(",");

        Set<ID> willDeletes = new HashSet<>();
        for (String file : files) {
            if (!ID.isId(file)) {
                continue;
            }
            ID fileId = ID.valueOf(file);
            if (!checkAllow(user, fileId)) {
                writeFailure(response, "无权删除他人文件");
                return;
            }

            willDeletes.add(fileId);
        }
        Application.getCommonService().delete(willDeletes.toArray(new ID[0]));
        writeSuccess(response);
    }

    @RequestMapping("move-files")
    public void moveFiles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ID user = getRequestUser(request);
        ID inFolder = getIdParameter(request, "folder");
        String[] files = getParameter(request, "ids", "").split(",");

        List<Record> fileRecords = new ArrayList<>();
        for (String file : files) {
            if (!ID.isId(file)) {
                continue;
            }
            ID fileId = ID.valueOf(file);
            if (!checkAllow(user, fileId)) {
                writeFailure(response, "无权更改他人文件");
                return;
            }

            Record r = EntityHelper.forUpdate(fileId, user);
            if (inFolder == null) {
                r.setNull("inFolder");
            } else {
                r.setID("inFolder", inFolder);
            }
            fileRecords.add(r);
        }
        Application.getCommonService().createOrUpdate(fileRecords.toArray(new Record[0]), false);
        writeSuccess(response);
    }

    @RequestMapping("check-readable")
    public void checkReadable(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ID user = getRequestUser(request);
        ID record = getIdParameterNotNull(request, "id");

        boolean OK = false;
        if (record.getEntityCode() == EntityHelper.Feeds || record.getEntityCode() == EntityHelper.FeedsComment) {
            OK = FeedsHelper.checkReadable(record, user);
        } else {
            OK = Application.getSecurityManager().allowRead(user, record);
        }
        writeSuccess(response, OK);
    }

    // 是否允许操作指定文件（管理员总是允许）
    private boolean checkAllow(ID user, ID file) {
        if (UserHelper.isAdmin(user)) {
            return true;
        }

        Object[] o = Application.createQueryNoFilter(
                "select createdBy from Attachment where attachmentId = ?")
                .setParameter(1, file)
                .unique();
        return o != null && o[0].equals(user);
    }
}
