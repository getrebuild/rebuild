/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

/**
 * 发送内部通知
 *
 * @author devezhao
 * @since 11/01/2018
 */
public class NotificationObserver extends OperatingObserver {

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
    public void onAssign(OperatingContext context) {
        final ID related = context.getAfterRecord().getPrimary();
        if (NotificationOnce.didBegin()) {
            NotificationOnce.getMergeSet().add(related);
            return;
        }

        ID from = context.getOperator();
        ID to = context.getAfterRecord().getID(EntityHelper.OwningUser);

        String content = buildMessage(context.getAffected(), related, false);
        content = MessageFormat.format(content, from, context.getAffected().length, getLabel(related));
        Application.getNotifications().send(
                MessageBuilder.createMessage(to, content, Message.TYPE_ASSIGN));
    }

    @Override
    public void onShare(OperatingContext context) {
        final ID related = context.getAfterRecord().getID("recordId");
        if (NotificationOnce.didBegin()) {
            NotificationOnce.getMergeSet().add(related);
            return;
        }

        ID from = context.getOperator();
        ID to = context.getAfterRecord().getID("shareTo");

        String content = buildMessage(context.getAffected(), related, true);
        content = MessageFormat.format(content, from, context.getAffected().length, getLabel(related));
        Application.getNotifications().send(
                MessageBuilder.createMessage(to, content, Message.TYPE_SAHRE));
    }

    private String getLabel(ID id) {
        return EasyMetaFactory.valueOf(id.getEntityCode()).getLabel();
    }

    /**
     * @param affected
     * @param related
     * @param shareType
     * @return
     */
    private String buildMessage(ID[] affected, ID related, boolean shareType) {
        String msg = "@{0} 共享了 {1} 条{2}记录给你";
        if (affected.length > 1) {
            for (ID id : affected) {
                if (id.getEntityCode().intValue() != related.getEntityCode().intValue()) {
                    msg = "@{0} 共享了{2}及其关联记录共 {1} 条记录给你";
                    break;
                }
            }
            msg += "，包括 @";

            String atrs = StringUtils.join(ArrayUtils.subarray(affected, 0, 10), " @");
            msg += atrs;
            if (affected.length > 10) {
                msg += " 等";
            }
        } else {
            msg += " @" + related;
        }

        if (!shareType) {
            msg = msg.replace(" 共享", " 分派");
        }
        return msg;
    }
}
