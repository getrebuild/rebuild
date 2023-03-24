/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.files;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.files.BatchDownload;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.commons.FileDownloader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
            if (!FilesHelper.isFileManageable(user, fileId)) {
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
            if (!FilesHelper.isFileManageable(user, fileId)) {
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

    // TODO 更严格的文件访问权限检查
    @RequestMapping("check-readable")
    public RespBody checkReadable(@IdParam ID recordOrFileId, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final int entityCode = recordOrFileId.getEntityCode();

        boolean readable;
        // 文件
        if (entityCode == EntityHelper.Attachment) {
            readable = FilesHelper.isFileAccessable(user, recordOrFileId);
        } else {
            // 附件
            if (entityCode == EntityHelper.Feeds || entityCode == EntityHelper.FeedsComment) {
                readable = FeedsHelper.checkReadable(recordOrFileId, user);
            } else if (entityCode == EntityHelper.ProjectTask || entityCode == EntityHelper.ProjectTaskComment) {
                readable = ProjectHelper.checkReadable(recordOrFileId, user);
            } else {
                readable = Application.getPrivilegesManager().allowRead(user, recordOrFileId);
            }
        }

        return RespBody.ok(readable);
    }

    @PostMapping("batch-download")
    public void batchDownload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String files = req.getParameter("files");

        List<String> filePaths = new ArrayList<>();
        Collections.addAll(filePaths, files.split(","));

        BatchDownload bd = new BatchDownload(filePaths);
        TaskExecutors.run(bd);

        File zipName = bd.getDestZip();
        if (zipName != null && zipName.exists()) {
            FileDownloader.downloadTempFile(resp, zipName, null);
        } else {
            resp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), Language.L("无法下载文件"));
        }
    }
}
