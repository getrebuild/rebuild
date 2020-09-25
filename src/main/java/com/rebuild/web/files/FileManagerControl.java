/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.files;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
public class FileManagerControl extends BaseController {

    @RequestMapping("post-files")
    public void postFiles(HttpServletRequest request, HttpServletResponse response) {
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
        Application.getCommonsService().createOrUpdate(fileRecords.toArray(new Record[0]), false);

        writeSuccess(response);
    }

    @RequestMapping("delete-files")
    public void deleteFiles(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        String[] files = getParameter(request, "ids", "").split(",");

        Set<ID> willDeletes = new HashSet<>();
        for (String file : files) {
            if (!ID.isId(file)) continue;

            ID fileId = ID.valueOf(file);
            if (!allowOperating(user, fileId)) {
                writeFailure(response, getLang(request, "NoFilePermissionSome", "Delete"));
                return;
            }

            willDeletes.add(fileId);
        }
        Application.getCommonsService().delete(willDeletes.toArray(new ID[0]));

        writeSuccess(response);
    }

    @RequestMapping("move-files")
    public void moveFiles(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        ID inFolder = getIdParameter(request, "folder");
        String[] files = getParameter(request, "ids", "").split(",");

        List<Record> fileRecords = new ArrayList<>();
        for (String file : files) {
            if (!ID.isId(file)) {
                continue;
            }
            ID fileId = ID.valueOf(file);
            if (!allowOperating(user, fileId)) {
                writeFailure(response, getLang(request, "NoFilePermissionSome", "Modify"));
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
        Application.getCommonsService().createOrUpdate(fileRecords.toArray(new Record[0]), false);
        writeSuccess(response);
    }

    @RequestMapping("check-readable")
    public void checkReadable(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID record = getIdParameterNotNull(request, "id");

        int entityCode = record.getEntityCode();
        boolean readable;
        if (entityCode == EntityHelper.Feeds || entityCode == EntityHelper.FeedsComment) {
            readable = FeedsHelper.checkReadable(record, user);
        } else if (entityCode == EntityHelper.ProjectTask || entityCode == EntityHelper.ProjectTaskComment) {
            readable = ProjectHelper.checkReadable(record, user);
        } else {
            readable = Application.getPrivilegesManager().allowRead(user, record);
        }

        writeSuccess(response, readable);
    }

    /**
     * 是否允许操作指定文件（管理员与创建人允许）
     *
     * @param user
     * @param file
     * @return
     */
    private boolean allowOperating(ID user, ID file) {
        if (UserHelper.isAdmin(user)) return true;

        Object[] o = Application.createQueryNoFilter(
                "select createdBy from Attachment where attachmentId = ?")
                .setParameter(1, file)
                .unique();
        return o != null && o[0].equals(user);
    }
}
