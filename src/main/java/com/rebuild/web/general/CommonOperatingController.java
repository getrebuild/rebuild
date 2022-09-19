/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 非业务实体操作（如系统实体）
 *
 * @author devezhao
 * @since 2020/11/6
 * @see com.rebuild.core.Application#getService(int)
 * @see GeneralOperatingController
 */
@Slf4j
@RestController
@RequestMapping("/app/entity/")
public class CommonOperatingController extends BaseController {

    @PostMapping("common-save")
    public JSONAware save(HttpServletRequest request) {
        final JSON formJson = ServletUtils.getRequestJson(request);

        Record record;
        try {
            record = EntityHelper.parse((JSONObject) formJson, getRequestUser(request));
        } catch (DataSpecificationException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        return saveRecord(record);
    }

    @RequestMapping("common-delete")
    public JSON delete(@IdParam ID recordId) {
        return deleteRecord(recordId);
    }

    @RequestMapping("common-get")
    public RespBody get(@IdParam ID recordId, HttpServletRequest request) {
        // 为空则返回全部
        String fields = getParameter(request, "fields", "");

        Record record = Application.getQueryFactory().recordNoFilter(recordId, fields.split(","));
        return RespBody.ok(record);
    }

    /**
     * @param record
     * @return
     */
    static JSONAware saveRecord(Record record) {
        try {
            record = Application.getService(record.getEntity().getEntityCode()).createOrUpdate(record);
            return JSONUtils.toJSONObject("id", record.getPrimary());
        } catch (DataSpecificationException | AccessDeniedException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }
    }

    /**
     * @param recordId
     * @return
     */
    static JSON deleteRecord(ID recordId) {
        int del = Application.getService(recordId.getEntityCode()).delete(recordId);
        return JSONUtils.toJSONObject(
                new String[] { "deleted", "requests" },
                new Object[] { del, del });
    }
}
