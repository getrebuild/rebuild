/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
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
        final ID relatedId = context.getAfterRecord().getPrimary();
        if (NotificationOnce.didBegin()) {
            NotificationOnce.getMergeSet().add(relatedId);
            return;
        }

        ID from = context.getOperator();
        ID to = context.getAfterRecord().getID(EntityHelper.OwningUser);

        String content = buildMessage(context.getAffected(), related, BizzPermission.ASSIGN);
        content = MessageFormat.format(content,
                from, context.getAffected().length, EasyMetaFactory.valueOf(relatedId.getEntityCode()).getLabel());
        
        Application.getNotifications().send(
                MessageBuilder.createMessage(to, content, Message.TYPE_ASSIGN, relatedId));
    }

    @Override
    public void onShare(OperatingContext context) {
        final ID relatedId = context.getAfterRecord().getID("recordId");
        if (NotificationOnce.didBegin()) {
            NotificationOnce.getMergeSet().add(relatedId);
            return;
        }

        ID from = context.getOperator();
        ID to = context.getAfterRecord().getID("shareTo");

        String content = buildMessage(context.getAffected(), related, BizzPermission.SHARE);
        content = MessageFormat.format(content,
                from, context.getAffected().length, EasyMetaFactory.valueOf(relatedId.getEntityCode()).getLabel());

        Application.getNotifications().send(
                MessageBuilder.createMessage(to, content, Message.TYPE_SAHRE, relatedId));
    }

    /**
     * @param affected
     * @param relatedId
     * @param action
     * @return
     */
    private String buildMessage(ID[] affected, ID relatedId, Permission action) {
        String message = Language.L("@{0} 共享了 {1} 条{2}记录给你");
        if (affected.length > 1) {
            for (ID id : affected) {
                if (id.getEntityCode().intValue() != relatedId.getEntityCode().intValue()) {
                    message = Language.L("@{0} 共享了{2}及其关联记录共 {1} 条记录给你", relatedId);
                    break;
                }
            }

            message += Language.L("，包括 @%s 等", relatedId);
        } else {
            message += " @" + relatedId;
        }

        if (action == BizzPermission.ASSIGN) {
            message = message.replace(Language.L("共享"), Language.L("分派"));
        }
        return message;
    }
}
