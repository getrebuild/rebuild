/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.api.RespBody;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

import static com.rebuild.web.commons.FileDownloader.checkUser;

/**
 * 文件上传
 *
 * @author Zixin (RB)
 * @since 11/06/2019
 */
@Slf4j
@Controller
@RequestMapping("/filex/")
public class FileUploader extends BaseController {

    @PostMapping("upload")
    public void upload(HttpServletRequest request, HttpServletResponse response) {
        RbAssert.isAllow(checkUser(request), "Unauthorized access");

        CommonsMultipartResolver resolver = new CommonsMultipartResolver(request.getServletContext());

        MultipartFile file = null;
        MultipartHttpServletRequest mp = resolver.resolveMultipart(request);
        for (MultipartFile t : mp.getFileMap().values()) {
            // 只允许一个
            file = t;
            break;
        }

        if (file == null) {
            writeFailure(response, "No file found");
            return;
        }

        String uploadName;
        try {
            boolean noname = getBoolParameter(request, "noname", Boolean.FALSE);
            uploadName = QiniuCloud.formatFileKey(file.getOriginalFilename(), !noname);

            File dest;
            // 上传临时文件
            if (getBoolParameter(request, "temp")) {
                uploadName = uploadName.split("/")[2];
                dest = RebuildConfiguration.getFileOfTemp(uploadName);
            } else {
                dest = RebuildConfiguration.getFileOfData(uploadName);
                FileUtils.forceMkdir(dest.getParentFile());
            }

            file.transferTo(dest);
            if (!dest.exists()) {
                writeFailure(response, Language.L("上传失败，请稍后重试"));
                return;
            }

        } catch (Exception ex) {
            log.error(null, ex);
            uploadName = null;
        }

        if (uploadName != null) {
            writeSuccess(response, uploadName);
        } else {
            writeFailure(response, Language.L("上传失败，请稍后重试"));
        }
    }

    /**
     * @see FilesHelper#storeFileSize(String, long)
     */
    @RequestMapping("store-filesize")
    @ResponseBody
    public RespBody storeFilesize(HttpServletRequest request) {
        long fileSize = ObjectUtils.toLong(getParameter(request, "fs"));
        if (fileSize < 1) {
            return RespBody.error();
        }

        String filePath = request.getParameter("fp");
        if (StringUtils.isNotBlank(filePath)) {
            FilesHelper.storeFileSize(filePath, fileSize);
        }
        return RespBody.ok();
    }
}
