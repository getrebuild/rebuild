/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.files;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.files.BatchDownload;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.commons.FileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/11/12
 */
@Slf4j
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
            if (inFolder != null) r.setID("inFolder", inFolder);
            fileRecords.add(r);
        }

        ID[] deletes = getIdArrayParameter(request, "deletes");
        createOrUpdateAndDelete43(fileRecords.toArray(new Record[0]), deletes, user);
        return RespBody.ok();
    }

    @RequestMapping("delete-files")
    public RespBody deleteFiles(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String[] files = getParameter(request, "ids", "").split(",");

        Set<ID> willDeleteIds = new HashSet<>();
        for (String file : files) {
            if (!ID.isId(file)) continue;

            ID fileId = ID.valueOf(file);
            if (!FilesHelper.isFileManageable(user, fileId)) {
                return RespBody.errorl("无权删除他人文件");
            }
            willDeleteIds.add(fileId);
        }

        TransactionStatus tx = TransactionManual.newTransaction();
        ServiceSpec ss = Application.getService(EntityHelper.Attachment);
        try {
            for (ID fileId : willDeleteIds) ss.delete(fileId);

            TransactionManual.commit(tx);
        } catch (Exception ex) {
            log.error("[delete-files]", ex);
            TransactionManual.rollback(tx);
        }
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
            Record r = EntityHelper.forUpdate(fileId, user);
            if (inFolder == null) r.setNull("inFolder");
            else r.setID("inFolder", inFolder);
            fileRecords.add(r);
        }

        ID[] deletes = getIdArrayParameter(request, "deletes");
        createOrUpdateAndDelete43(fileRecords.toArray(new Record[0]), deletes, user);
        return RespBody.ok();
    }

    // 更新并删除文件（记录）
    private void createOrUpdateAndDelete43(Record[] createOrUpdate, ID[] deletes, ID user) {
        if (ArrayUtils.isNotEmpty(deletes)) {
            TransactionStatus tx = TransactionManual.newTransaction();
            ServiceSpec ss = Application.getService(EntityHelper.Attachment);
            try {
                // 覆盖/删除
                for (ID fileid : deletes) {
                    if (FilesHelper.isFileManageable(user, fileid)) {
                        ss.delete(fileid);
                    } else {
                        log.warn("No privileges to overwrite/delete file : {}", fileid);
                    }
                }
                // 新增
                Application.getCommonsService().createOrUpdate(createOrUpdate, false);

                TransactionManual.commit(tx);
            } catch (Exception ex) {
                log.error("[createOrUpdateAndDelete]", ex);
                TransactionManual.rollback(tx);
            }

        } else {
            Application.getCommonsService().createOrUpdate(createOrUpdate, false);
        }
    }

    @RequestMapping("check-files")
    public RespBody checkFiles(HttpServletRequest request) {
        ID inFolder = getIdParameter(request, "folder");
        JSONArray files = (JSONArray) ServletUtils.getRequestJson(request);

        String checkSql = "select attachmentId from Attachment where belongEntity = 0 and isDeleted = 'F' and fileName = ?";
        if (inFolder == null) checkSql += " and inFolder is null";
        else checkSql += String.format(" and inFolder = '%s'", inFolder);

        Map<String, Object> error = new HashMap<>();
        for (Object o : files) {
            // 支持文件名和 ID
            String name = (String) o;
            if (ID.isId(name)) {
                Object[] e = Application.getQueryFactory().uniqueNoFilter(ID.valueOf(name), "fileName");
                if (e != null && e[0] != null) name = e[0].toString();
            } else {
                name = QiniuCloud.cleanFileName(name);
            }

            Object[] e = Application.createQueryNoFilter(checkSql).setParameter(1, name).unique();
            if (e != null) error.put(name, e[0]);
        }
        return RespBody.ok(error);
    }

    @RequestMapping("check-readable")
    public RespBody checkReadable(@IdParam ID fileId, HttpServletRequest request) {
        String filePath = checkFileReadable(fileId, getRequestUser(request));
        return filePath == null ? RespBody.error() : RespBody.ok(filePath);
    }

    // 是否可读取文件
    private String checkFileReadable(ID fileId, ID user) {
        // v4.2 目录
        if (fileId.getEntityCode() == EntityHelper.AttachmentFolder) {
            return FilesHelper.getAccessableFolders(user).contains(fileId) ? fileId.toLiteral() : null;
        }
        // FIXME v4.2 仪表盘临时借用
        if (fileId.getEntityCode() == EntityHelper.DashboardConfig) {
            return UserHelper.isSelf(user, fileId) ? fileId.toLiteral() : null;
        }

        Object[] file = Application.getQueryFactory().uniqueNoFilter(
                fileId, "filePath,relatedRecord,belongEntity");
        if (file == null) return null;
        if (UserHelper.isAdmin(user)) return (String) file[0];

        // 文件
        if ((int) file[2] <= 0) {
            if (FilesHelper.isFileAccessable(user, fileId)) return (String) file[0];
            else return null;
        }

        // 附件
        final ID recordId = (ID) file[1];
        if (recordId == null) return null;

        int entityCode = recordId.getEntityCode();
        boolean readable;
        if (entityCode == EntityHelper.Feeds || entityCode == EntityHelper.FeedsComment) {
            readable = FeedsHelper.checkReadable(recordId, user);
        } else if (entityCode == EntityHelper.ProjectTask || entityCode == EntityHelper.ProjectTaskComment) {
            readable = ProjectHelper.checkReadable(recordId, user);
        } else {
            readable = Application.getPrivilegesManager().allowRead(user, recordId);
        }
        return readable ? (String) file[0] : null;
    }

    @PostMapping("batch-download")
    public void downloadBatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String files = req.getParameter("files");

        List<String> filesList = new ArrayList<>();
        Collections.addAll(filesList, files.split(","));

        BatchDownload bd = new BatchDownload(filesList);
        TaskExecutors.run(bd);

        File zipName = bd.getDestZip();
        if (zipName != null && zipName.exists()) {
            FileDownloader.downloadTempFile(resp, zipName);
        } else {
            resp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), Language.L("无法下载文件"));
        }
    }

    @RequestMapping("download")
    public void download(@IdParam ID fileId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String filePath = checkFileReadable(fileId, getRequestUser(req));
        if (filePath == null) {
            resp.sendError(HttpStatus.FORBIDDEN.value(), Language.L("你没有查看此文件的权限"));
        } else {
            String fileUrl = CodecUtils.urlEncode(filePath);
            fileUrl = fileUrl.replace("%2F", "/");
            fileUrl = String.format("../filex/download/%s?attname=%s",
                    fileUrl, CodecUtils.urlEncode(QiniuCloud.parseFileName(filePath)));
            resp.sendRedirect(fileUrl);
        }
    }

    @PostMapping("file-edit")
    public RespBody fileEdit(HttpServletRequest req) throws IOException {
        final ID user = getRequestUser(req);
        final ID fileId = getIdParameter(req, "id");
        if (!FilesHelper.isFileManageable(user, fileId)) {
            return RespBody.errorl("无权修改此文件");
        }

        String newName = getParameterNotNull(req, "newName");

        Object[] o = Application.getQueryFactory().uniqueNoFilter(fileId, "filePath");
        String filePath = (String) o[0];
        if (CommonsUtils.isExternalUrl(filePath)) return RespBody.errorl("无法修改外部文件");

        String oldName = QiniuCloud.parseFileName(filePath);
        if (StringUtils.equals(newName, oldName)) return RespBody.ok(filePath);

        String newFilePath = filePath.substring(0, filePath.lastIndexOf(oldName)) + newName;
        FilesHelper.moveFile(filePath, newFilePath, fileId);
        return RespBody.ok(newFilePath);
    }
}
