/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 消息通知
 *
 * @author devezhao
 * @since 2025/8/20
 */
@Slf4j
public class MyNotification extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000007");

    public MyNotification() {
        super(null);
        this.config = getChartConfig();
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("我的通知");
    }

    @Override
    public JSON build() {
        String sql = "select fromUser,message,createdOn,unread,messageId,relatedRecord,type,unread from Notification" +
                " where toUser = ? and (1=1) order by createdOn desc";
        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, UserContextHolder.getUser())
                .setLimit(500)
                .array();

        List<Object[]> resReaded = new ArrayList<>();
        List<Object[]> resUnread = new ArrayList<>();

        for (Object[] o : array) {
            o[0] = new Object[]{o[0], UserHelper.getName((ID) o[0])};
            o[1] = MessageBuilder.formatMessage((String) o[1]);
            o[2] = I18nUtils.formatDate((Date) o[2]);

            if (ObjectUtils.toBool(o[7])) resUnread.add(o);
            else resReaded.add(o);
        }

        return JSONUtils.toJSONObject(new String[]{"readed", "unread"}, new Object[]{resReaded, resUnread});
    }
}
