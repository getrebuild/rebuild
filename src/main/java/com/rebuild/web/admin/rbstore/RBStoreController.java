/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.rbstore.RBStore;
import com.rebuild.core.rbstore.SkillImporter;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao-mbp
 * @since 2019/04/28
 */
@Slf4j
@RestController
public class RBStoreController extends BaseController {

    @GetMapping({"/admin/rbstore/load-index", "/setup/load-index"})
    public JSONAware loadDataIndex(HttpServletRequest request) {
        String type = getParameterNotNull(request, "type");
        if (CommonsUtils.isExternalUrl(type)) return RespBody.error();

        JSON index = null;
        try {
            index = RBStore.fetchRemoteJson(type + "/index.json");
        } catch (Exception ignored) {
        }
        return index == null ? RespBody.error("CANNOT FETCH DATA FROM RB-STORE") : index;
    }

    @GetMapping({"/admin/rbstore/load-metaschemas", "/setup/init-models"})
    public JSON loadMetaschemas() {
        JSONObject index = (JSONObject) RBStore.fetchMetaschema(null);
        JSONArray schemas = index.getJSONArray("schemas");
        for (Object o : schemas) {
            JSONObject item = (JSONObject) o;
            String key = item.getString("key");
            if (Application.isStateReady() && MetadataHelper.containsEntity(key)) {
                item.put("exists", true);
            }
        }
        return index;
    }

    @PostMapping("/admin/rbstore/import-skills")
    public RespBody importSkills(HttpServletRequest request) {
        String[] names = getParameterNotNull(request, "names").split(",");

        SkillImporter importer = new SkillImporter();
        importer.setSkillNames(names);
        importer.setUser(getRequestUser(request));

        try {
            TaskExecutors.run(importer);
            return importer.getSucceeded() > 0
                    ? RespBody.ok() : RespBody.error(importer.getErrorMessage());

        } catch (Exception ex) {
            log.error("Cannot import skills : {}", getParameter(request, "names"), ex);
            return RespBody.error(ex.getLocalizedMessage());
        }
    }
}
