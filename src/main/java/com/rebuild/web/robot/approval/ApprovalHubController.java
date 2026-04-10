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
import com.rebuild.core.support.License;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import static com.rebuild.core.service.approval.ApprovalState.DRAFT;

/**
 * @author Zixin
 * @since 2026/04/05
 */
@Slf4j
@RestController
@RequestMapping("/approval/")
public class ApprovalHubController extends BaseController {

    // `userApprove = '%s' and state = 1 and approvalStepId.isWaiting = 'F'`
    public static final ID FILTER_BADGE = ID.valueOf("014-0490000000000000");

    @GetMapping({"home", "hub"})
    public ModelAndView pageIndex(HttpServletResponse response) throws IOException {
        if (License.isCommercial()) {
            return createModelAndView("/approval/home");
        }

        response.sendError(404,
                Language.L("免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));
        return null;
    }

    @RequestMapping("data-list")
    public RespBody dataList(HttpServletRequest request) {
        ID user = getRequestUser(request);
        int type = getIntParameter(request, "type", 1);

        String sql = "select hubId,createdOn,createdBy,state,approvalStepId,approvalStepId.recordId,approvalStepId.approvalId,hubBatch" +
                " from RobotApprovalHub where ";
        sql += buildFilterSql(type, user);
        // 排序
        if ("older".equals(getParameter(request, "sort"))) sql += " order by createdOn asc";
        else sql += " order by createdOn desc";

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 100);

        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();
        JSONArray res = new JSONArray();
        for (Object[] o : array) {
            res.add(buildItem(o, user));
        }

        return RespBody.ok(res);
    }

    private JSONObject buildItem(Object[] o, ID user) {
        int state = (Integer) o[3];
        ID recordId = (ID) o[5];
        ID approvalId = (ID) o[6];
        String hubBatch = (String) o[7];

        JSONObject item = JSONUtils.toJSONObject(
                new String[]{"id", "createdOn", "createdBy", "state", "approvalId"},
                new Object[]{o[0], I18nUtils.formatDate((Date) o[1]), o[2], state, approvalId});
        // 创建人
        item.put("createdBy", new Object[]{o[2], UserHelper.getName((ID) o[2])});

        // 记录信息
        EasyEntity recordMeta = EasyMetaFactory.valueOf(MetadataHelper.getEntity(recordId.getEntityCode()));
        item.put("recordMeta", new Object[]{
                recordId, FieldValueHelper.getLabelNotry(recordId),
                recordMeta.getName(), recordMeta.getLabel()
        });

        if (state == DRAFT.getState()) {
            Object[] a = Application.createQueryNoFilter(
                    "select state from RobotApprovalHub where hubBatch = ? and userApprove = ?")
                    .setParameter(1, hubBatch)
                    .setParameter(2, user)
                    .unique();
            if (a != null && (Integer) a[0] == DRAFT.getState()) {
                item.put("imApprover", true);
            }
        }

        return item;
    }

    /**
     * @param type
     * @param user
     * @return
     */
    public static String buildFilterSql(int type, ID user) {
        if (type == 1) {
            return String.format("userApprove = '%s' and state = 1 and approvalStepId.isWaiting = 'F'", user);
        } else if (type == 2) {
            return String.format("userApprove = '%s' and state in (10,11)", user);
        } else if (type == 3) {
            return String.format("userSubmit = '%s'", user);
        } else if (type == 4) {
            return String.format("userCc = '%s'", user);
        }
        return "(1=2)";
    }
}
