/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.rebuild.api.RespBody;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.rbstore.BusinessModelImporter;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导入元数据模型
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
@Slf4j
@RestController
public class MetaschemaController extends BaseController {

    @RequestMapping("/admin/metadata/imports")
    public RespBody imports(HttpServletRequest request) {
        final String mainKey = getParameterNotNull(request, "key");

        BusinessModelImporter bmi = new BusinessModelImporter();

        Map<String, String> refs = bmi.findRefs(mainKey);

        List<String> entityFiles = new ArrayList<>();
        for (Map.Entry<String, String> e : refs.entrySet()) {
            if (!MetadataHelper.containsEntity(e.getKey())) {
                entityFiles.add(e.getValue());
            }
        }
        bmi.setModelFiles(entityFiles.toArray(new String[0]));

        try {
            TaskExecutors.run(bmi);

            if (bmi.getSucceeded() > 0) {
                return RespBody.ok(mainKey);
            } else {
                return RespBody.error();
            }

        } catch (Exception ex) {
            log.error("Cannot import entity : " + mainKey, ex);
            return RespBody.error(ex.getLocalizedMessage());
        }
    }
}
