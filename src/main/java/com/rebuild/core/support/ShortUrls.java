/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;

/**
 * 短链/文件分享
 *
 * @author RB
 * @since 2023/5/30
 */
@Slf4j
public class ShortUrls {

    /**
     * 已有有效分享则复用原记录（同一用户同一对象仅一条，链接不变），仅更新有效期
     *
     * @param longUrl
     * @param seconds
     * @param user
     * @return
     */
    public static String make(String longUrl, int seconds, ID user) {
        Object[][] exists = Application.createQueryNoFilter(
                "select shortId,shortKey from ShortUrl" +
                        " where longUrl = ? and createdBy = ? and (expireTime > ? or expireTime is null) order by createdOn desc")
                .setParameter(1, longUrl)
                .setParameter(2, user)
                .setParameter(3, CalendarUtils.now())
                .setLimit(1)
                .array();

        if (exists.length > 0) {
            Record update = EntityHelper.forUpdate((ID) exists[0][0], user);
            if (seconds > 0) update.setDate("expireTime", CalendarUtils.add(seconds, Calendar.SECOND));
            else update.setNull("expireTime");
            Application.getCommonsService().update(update);

            return (String) exists[0][1];
        }

        Record record = EntityHelper.forNew(EntityHelper.ShortUrl, user);
        record.setString("longUrl", longUrl);
        if (seconds > 0) record.setDate("expireTime", CalendarUtils.add(seconds, Calendar.SECOND));

        String shortKey;
        while (true) {
            shortKey = CodecUtils.randomCode(10);
            Object e = Application.createQueryNoFilter(
                    "select shortId from ShortUrl where shortKey = ?")
                    .setParameter(1, shortKey)
                    .unique();
            if (e == null) break;
        }

        record.setString("shortKey", shortKey);
        Application.getCommonsService().create(record);
        return shortKey;
    }

    /**
     * @param shortKey
     * @return
     */
    public static String retrieveUrl(String shortKey) {
        Object[] o = Application.createQueryNoFilter(
                "select longUrl,expireTime,checkPasswd from ShortUrl where shortKey = ?")
                .setParameter(1, shortKey)
                .unique();
        if (o == null) return null;

        if (o[1] != null) {
            long exp = ((Date) o[1]).getTime() - CalendarUtils.now().getTime();
            if (exp < 0) {
                log.warn("ShortUrl is expired : {}", shortKey);
                return null;
            }
        }

        return (String) o[0];
    }
}
