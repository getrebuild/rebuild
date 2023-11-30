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
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

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
     * @param longUrl
     * @return
     */
    public static String make(String longUrl) {
        return make(longUrl, 0, null);
    }

    /**
     * @param longUrl
     * @param seconds
     * @param user
     * @return
     */
    public static String make(String longUrl, int seconds, ID user) {
        Record record = EntityHelper.forNew(EntityHelper.ShortUrl,
                ObjectUtils.defaultIfNull(user, UserService.SYSTEM_USER));
        record.setString("longUrl", longUrl);
        if (seconds > 0) record.setDate("expireTime", CalendarUtils.add(seconds, Calendar.SECOND));

        final String shortKey = CodecUtils.randomCode(20);
        record.setString("shortKey", shortKey);

        Application.getCommonsService().create(record);
        return shortKey;
    }

    /**
     * @param shortKey
     * @return
     */
    public static boolean invalid(String shortKey) {
        String dsql = String.format(
                "delete from `short_url` where SHORT_KEY = '%s'", CommonsUtils.escapeSql(shortKey));
        int a = Application.getSqlExecutor().execute(dsql);
        return a > 0;
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
