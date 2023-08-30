/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量修改
 *
 * @author ZHAO
 * @since 2019/12/1
 */
@RestController
@RequestMapping("/app/{entity}/batch-update/")
public class BatchUpdateController extends BaseController {

    @PostMapping("submit")
    public RespBody submit(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate),
                Language.L("无操作权限"));

        JSONObject requestData = (JSONObject) ServletUtils.getRequestJson(request);
        requestData.put("_dataRange", getIntParameter(request, "dr", BatchOperatorQuery.DR_PAGED));
        requestData.put("entity", entity);
        BulkContext bulkContext = new BulkContext(user, BizzPermission.UPDATE, requestData);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        String taskid = Application.getEntityService(entityMeta.getEntityCode()).bulkAsync(bulkContext);

        return RespBody.ok(taskid);
    }

    @GetMapping("fields")
    public List<JSONObject> getFields(@PathVariable String entity) {
        Entity entityMeta = MetadataHelper.getEntity(entity);

        List<JSONObject> updatableFields = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (!field.isUpdatable()) continue;

            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (!easyField.isUpdatable()) continue;

            DisplayType dt = easyField.getDisplayType();
            // 不支持的字段
            if (dt == DisplayType.FILE
                    || dt == DisplayType.IMAGE
                    || dt == DisplayType.AVATAR
                    || dt == DisplayType.BARCODE
                    || dt == DisplayType.SERIES
                    || dt == DisplayType.ANYREFERENCE
                    || dt == DisplayType.LOCATION
                    || dt == DisplayType.SIGN) {
                continue;
            }

            updatableFields.add(MetaFormatter.buildRichField(easyField));
        }
        return updatableFields;
    }
}
