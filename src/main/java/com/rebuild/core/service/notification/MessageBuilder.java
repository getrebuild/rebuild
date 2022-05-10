/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.MarkdownUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息构建
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/12
 */
public class MessageBuilder {

    /**
     * @param toUser
     * @param message
     * @return
     */
    public static Message createMessage(ID toUser, String message) {
        return new Message(null, toUser, message, null, Message.TYPE_DEFAULT);
    }

    /**
     * @param toUser
     * @param message
     * @param type
     * @return
     */
    public static Message createMessage(ID toUser, String message, int type) {
        return new Message(null, toUser, message, null, type);
    }

    /**
     * @param toUser
     * @param message
     * @param relatedRecord
     * @return
     */
    public static Message createApproval(ID toUser, String message, ID relatedRecord) {
        return new Message(null, toUser, message, relatedRecord, Message.TYPE_APPROVAL);
    }

    /**
     * @param toUser
     * @param message
     * @param type
     * @param relatedRecord
     * @return
     */
    public static Message createMessage(ID toUser, String message, int type, ID relatedRecord) {
        return new Message(null, toUser, message, relatedRecord, type);
    }

    // --

    /**
     * Matchs @ID
     */
    public static final Pattern AT_PATTERN = Pattern.compile("(@[0-9a-zA-Z\\-]{20})");

    /**
     * 格式化消息
     *
     * @param message
     * @return
     */
    public static String formatMessage(String message) {
        return formatMessage(message, true);
    }

    /**
     * 格式化消息，支持转换 MD 语法
     *
     * @param message
     * @param md2html
     * @return
     * @see MarkdownUtils#render(String)
     */
    public static String formatMessage(String message, boolean md2html) {
        // 匹配 `@ID`
        Matcher atMatcher = AT_PATTERN.matcher(message);
        while (atMatcher.find()) {
            String at = atMatcher.group();
            String atLabel = parseAtId(at.substring(1));
            if (atLabel != null && !atLabel.equals(at)) {
                message = message.replace(at, atLabel);
            }
        }

        if (md2html) {
            message = MarkdownUtils.render(message);
        }
        return message;
    }

    /**
     * @param atid
     * @return
     */
    protected static String parseAtId(String atid) {
        if (!ID.isId(atid)) {
            return atid;
        }

        final ID id = ID.valueOf(atid);
        if (id.getEntityCode() == EntityHelper.User) {
            if (Application.getUserStore().existsUser(id)) {
                return Application.getUserStore().getUser(id).getFullName();
            } else {
                return "[无效用户]";
            }
        }

        String recordLabel = FieldValueHelper.getLabelNotry(id);
        String recordUrl = AppUtils.getContextPath("/app/list-and-view?id=" + id);
        return String.format("[%s](%s)", recordLabel, recordUrl);
    }
}
