/*!
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
import com.rebuild.core.support.i18n.Language;

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
        // NOTE 异步无法使用 NotificationOnce 功能
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
        content = MessageFormat.format(content,
                from, context.getAffected().length, EasyMetaFactory.valueOf(related.getEntityCode()).getLabel());
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
        content = MessageFormat.format(content,
                from, context.getAffected().length, EasyMetaFactory.valueOf(related.getEntityCode()).getLabel());
        Application.getNotifications().send(
                MessageBuilder.createMessage(to, content, Message.TYPE_SAHRE));
    }

    /**
     * @param affected
     * @param related
     * @param shareType
     * @return
     */
    private String buildMessage(ID[] affected, ID related, boolean shareType) {
        String message = Language.L("@{0} 共享了 {1} 条{2}记录给你");
        if (affected.length > 1) {
            for (ID id : affected) {
                if (id.getEntityCode().intValue() != related.getEntityCode().intValue()) {
                    message = Language.L("@{0} 共享了{2}及其关联记录共 {1} 条记录给你", related);
                    break;
                }
            }

            message += Language.L("，包括 @%s 等", related);
        } else {
            message += " @" + related;
        }

        if (!shareType) {
            message = message.replace(Language.L(" 共享"), Language.L(" 分派"));
        }
        return message;
    }
}
