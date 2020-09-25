/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.notification;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * 系统通知
 *
 * @author devezhao
 * @see com.rebuild.core.service.notification.Message
 * @since 11/01/2018
 */
@Controller
public class NotificationControl extends BaseController {

    @GetMapping("/notifications")
    public ModelAndView pageIndex() {
        return createModelAndView("/notification/messages");
    }

    @GetMapping("/notifications/todo")
    public ModelAndView pageTodo() {
        return createModelAndView("/notification/todo");
    }

    @GetMapping("/notification/check-state")
    public void checkMessage(HttpServletRequest request, HttpServletResponse response) {
        int unread = Application.getNotifications().getUnreadMessage(getRequestUser(request));
        writeSuccess(response, JSONUtils.toJSONObject("unread", unread));
    }

    @RequestMapping("/notification/make-read")
    public void toggleUnread(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        String ids = getParameter(request, "id");

        if ("ALL".equalsIgnoreCase(ids)) {
            Object[][] unreads = Application.createQueryNoFilter(
                    "select messageId from Notification where toUser = ?")
                    .setParameter(1, user)
                    .array();
            ids = "";
            for (Object[] o : unreads) {
                ids += o[0] + ",";
            }
        }

        for (String id : ids.split(",")) {
            if (!ID.isId(id)) continue;

            Record record = EntityHelper.forUpdate(ID.valueOf(id), user);
            record.setBoolean("unread", false);
            Application.getNotifications().update(record);
        }
        writeSuccess(response);
    }

    @GetMapping("/notification/messages")
    public void listMessage(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        int pn = getIntParameter(request, "pageNo", 1);
        int ps = getIntParameter(request, "pageSize", 40);
        int type = getIntParameter(request, "type", 0);
        boolean preview = getBoolParameter(request, "preview");

        String sql = "select fromUser,message,createdOn,unread,messageId,relatedRecord,type from Notification" +
                " where toUser = ? and (1=1) order by createdOn desc";
        if (type == 1) {
            sql = sql.replace("(1=1)", "unread = 'T'");
        } else if (type == 2) {
            sql = sql.replace("(1=1)", "unread = 'F'");
        } else if (type >= 10) {
            sql = sql.replace("(1=1)", String.format("(type >= %d and type < %d)", type, type + 10));
        }

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, user)
                .setLimit(ps, pn * ps - ps)
                .array();
        for (int i = 0; i < array.length; i++) {
            Object[] m = array[i];
            m[0] = new Object[]{m[0], UserHelper.getName((ID) m[0])};
            m[1] = MessageBuilder.formatMessage((String) m[1], !preview, true);
            m[2] = I18nUtils.formatDate((Date) m[2]);
            array[i] = m;
        }
        writeSuccess(response, array);
    }

    @GetMapping("/notification/approvals")
    public void listApprovals(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        int pn = getIntParameter(request, "pageNo", 1);
        int ps = getIntParameter(request, "pageSize", 40);

        Object[][] array = Application.createQueryNoFilter(
                "select fromUser,message,createdOn,relatedRecord,messageId" +
                        " from Notification where toUser = ? and type = 20 and relatedRecord is not null order by createdOn desc")
                .setParameter(1, user)
                .setLimit(ps, pn * ps - ps)
                .array();

        for (int i = 0; i < array.length; i++) {
            Object[] m = array[i];
            m[0] = new Object[]{m[0], UserHelper.getName((ID) m[0])};
            m[1] = MessageBuilder.formatMessage((String) m[1]);
            m[2] = I18nUtils.formatDate((Date) m[2]);

            // 审批状态
            ID approvalStep = (ID) m[3];
            Object[] stepState = Application.createQueryNoFilter(
                    "select isCanceled,state from RobotApprovalStep where stepId = ?")
                    .setParameter(1, approvalStep)
                    .unique();
            if (stepState == null) {
                m[3] = new Object[]{0};
            } else {
                boolean canceled = (Boolean) stepState[0];
                ApprovalState state = (ApprovalState) ApprovalState.valueOf((Integer) stepState[1]);
                if (state == ApprovalState.DRAFT) {
                    m[3] = canceled ? new Object[]{2, "已处理"} : new Object[]{1, "待处理"};
                } else if (state == ApprovalState.APPROVED) {
                    m[3] = new Object[]{10, "已同意"};
                } else if (state == ApprovalState.REJECTED) {
                    m[3] = new Object[]{11, "已驳回"};
                }
            }

            array[i] = m;
        }
        writeSuccess(response, array);
    }
}
