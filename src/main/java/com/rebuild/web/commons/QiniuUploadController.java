/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.alibaba.fastjson.JSON;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/02
 */
@RestController
public class QiniuUploadController extends BaseController {

    // 获取上传参数
    @GetMapping("/filex/qiniu/upload-keys")
    public JSON getUploadKeys(HttpServletRequest request) {
        String fileName = getParameterNotNull(request, "file");
        boolean noname = getBoolParameter(request, "noname", Boolean.FALSE);

        String fileKey = QiniuCloud.formatFileKey(fileName, !noname);
        String token = QiniuCloud.instance().getUploadToken(fileKey);

        return JSONUtils.toJSONObject(
                new String[] { "key", "token" },
                new String[] { fileKey, token });
    }
}
