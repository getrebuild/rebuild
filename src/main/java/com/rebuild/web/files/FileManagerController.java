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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/11/12
 */
@RestController
@RequestMapping("/files/")
public class FileManagerController extends BaseController {

    @RequestMapping("post-files")
    public RespBody postFiles(HttpServletRequest request) {
        final ID user = getRequestUser(request);
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

        return RespBody.ok();
    }

    @RequestMapping("delete-files")
    public RespBody deleteFiles(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String[] files = getParameter(request, "ids", "").split(",");

        Set<ID> willDeletes = new HashSet<>();
        for (String file : files) {
            if (!ID.isId(file)) continue;

            ID fileId = ID.valueOf(file);
            if (!FilesHelper.isManageable(user, fileId)) {
                return RespBody.errorl("无权删除他人文件");
            }

            willDeletes.add(fileId);
        }

        Application.getCommonsService().delete(willDeletes.toArray(new ID[0]));
        return RespBody.ok();
    }

    @RequestMapping("move-files")
    public RespBody moveFiles(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ID inFolder = getIdParameter(request, "folder");
        String[] files = getParameter(request, "ids", "").split(",");

        List<Record> fileRecords = new ArrayList<>();
        for (String file : files) {
            if (!ID.isId(file)) continue;

            ID fileId = ID.valueOf(file);
            if (!FilesHelper.isManageable(user, fileId)) {
                return RespBody.errorl("无权修改他人文件");
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
        return RespBody.ok();
    }

    @RequestMapping("check-readable")
    public RespBody checkReadable(@IdParam ID recordId, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        int entityCode = recordId.getEntityCode();
        boolean readable;
        if (entityCode == EntityHelper.Feeds || entityCode == EntityHelper.FeedsComment) {
            readable = FeedsHelper.checkReadable(recordId, user);
        } else if (entityCode == EntityHelper.ProjectTask || entityCode == EntityHelper.ProjectTaskComment) {
            readable = ProjectHelper.checkReadable(recordId, user);
        } else {
            readable = Application.getPrivilegesManager().allowRead(user, recordId);
        }

        return RespBody.ok(readable);
    }
}
