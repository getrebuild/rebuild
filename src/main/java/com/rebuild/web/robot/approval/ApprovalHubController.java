/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author devezhao
 * @since 2019/07/05
 */
@Slf4j
@RestController
@RequestMapping("/approval/")
public class ApprovalHubController extends BaseController {

    @GetMapping({"home", "hub"})
    public ModelAndView pageIndex() {
        return createModelAndView("/approval/home");
    }

    @RequestMapping("data-list")
    public RespBody dataList(HttpServletRequest request) {
        ID user = getRequestUser(request);
        int type = getIntParameter(request, "type", 1);

        String sql = "select hubId,createdOn,createdBy,state,approvalStepId.recordId from RobotApprovalHub where ";
        if (type == 1) {
            sql += String.format("userApprove = '%s' and state in (1)", user);
        } else if (type == 2) {
            sql += String.format("userApprove = '%s' and state in (10,11)", user);
        } else if (type == 3) {
            sql += String.format("userSubmit = '%s'", user);
        } else if (type == 4) {
            sql += String.format("userCc = '%s'", user);
        }

        // 排序
        if ("newer".equals(getParameter(request, "sort"))) sql += " order by createdOn desc";
        else sql += " order by createdOn asc";

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 100);

        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();
        JSONArray res = new JSONArray();
        for (Object[] o : array) {
            ID recordId = (ID) o[4];
            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "createdOn", "state", "recordId"},
                    new Object[]{o[0], I18nUtils.formatDate((Date) o[1]), o[3], recordId});
            item.put("createdBy", new Object[]{o[2], UserHelper.getName((ID) o[2])});
            item.put("recordName", FieldValueHelper.getLabelNotry(recordId));

            EasyEntity recordMeta = EasyMetaFactory.valueOf(MetadataHelper.getEntity(recordId.getEntityCode()));
            item.put("recordMeta", new Object[]{
                    recordMeta.getName(), recordMeta.getLabel()
            });

            res.add(item);
        }

        return RespBody.ok(res);
    }
}
