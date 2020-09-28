/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.alibaba.fastjson.JSON;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/02
 */
@Controller
public class QiniuUploadController extends BaseController {

    // 获取上传参数
    @RequestMapping("/filex/qiniu/upload-keys")
    public void getUploadKeys(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = getParameterNotNull(request, "file");

        String fileKey = QiniuCloud.formatFileKey(fileName);
        String token = QiniuCloud.instance().getUploadToken(fileKey);

        JSON ret = JSONUtils.toJSONObject(
                new String[]{"key", "token"}, new String[]{fileKey, token});
        writeSuccess(response, ret);
    }

}
