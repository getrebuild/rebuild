/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.i18n.I18nUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * @author devezhao
 * @since 2022/1/15
 */
public class ConfigCommons {

    /**
     * 查询配置列表
     *
     * @param sql
     * @param belongEntity
     * @param q
     */
    public static Object[][] queryListOfConfig(String sql, String belongEntity, String q) {
        if (StringUtils.isNotBlank(belongEntity) && !"$ALL$".equalsIgnoreCase(belongEntity)) {
            sql = sql.replace("(1=1)", "belongEntity = '" + StringEscapeUtils.escapeSql(belongEntity) + "'");
        }
        if (StringUtils.isNotBlank(q)) {
            sql = sql.replace("(2=2)", "name like '%" + StringEscapeUtils.escapeSql(q) + "%'");
        }

        Object[][] array = Application.createQuery(sql).setLimit(500).array();
        for (Object[] o : array) {
            o[2] = EasyMetaFactory.getLabel(MetadataHelper.getEntity((String) o[2]));
            o[5] = I18nUtils.formatDate((Date) o[5]);
        }
        return array;
    }
}
