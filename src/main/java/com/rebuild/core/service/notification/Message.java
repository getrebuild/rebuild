/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.MarkdownUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * 通知消息。
 * 注意：若指定了 relatedRecord 相关记录删除时消息也将被删除
 *
 * @author devezhao
 * @since 10/17/2018
 * @see MessageBuilder
 */
@Data
@Setter(AccessLevel.NONE)
public class Message {

    // 未分类
    public static final int TYPE_DEFAULT = 0;
    // 分配消息
    public static final int TYPE_ASSIGN = 10;
    // 共享消息
    public static final int TYPE_SAHRE = 11;
    // 审批消息
    public static final int TYPE_APPROVAL = 20;
    // 动态
    public static final int TYPE_FEEDS = 30;
    // 项目-任务
    public static final int TYPE_PROJECT = 40;

    private ID fromUser;
    private ID toUser;
    private String message;
    private int type;
    private ID relatedRecord;

    @Setter
    private String specTitle;

    /**
     * @param fromUser
     * @param toUser
     * @param message
     * @param type
     * @param relatedRecord
     */
    public Message(ID fromUser, ID toUser, String message, int type, ID relatedRecord) {
        this.fromUser = fromUser == null ? UserService.SYSTEM_USER : fromUser;
        this.toUser = toUser;
        this.message = message;
        this.type = type;
        this.relatedRecord = relatedRecord;
    }

    /**
     * 获取通知标题
     * @return
     */
    public String getTitle4Type() {
        if (StringUtils.isNotBlank(this.specTitle)) return this.specTitle;
        if (this.type == TYPE_ASSIGN) return Language.L("记录分配通知");
        else if (this.type == TYPE_SAHRE) return Language.L("记录共享通知");
        else if (this.type == TYPE_APPROVAL) return Language.L("记录审批通知");
        else if (this.type == TYPE_FEEDS) return Language.L("动态通知");
        else if (this.type == TYPE_PROJECT) return Language.L("项目任务通知");
        else return Language.L("你有一条新通知");
    }

    /**
     * 无格式内容
     * @return
     */
    public String getPlainMessage() {
        String plain = MessageBuilder.formatMessage(message, false);
        plain = MarkdownUtils.cleanMarks(plain);
        return plain;
    }
}
