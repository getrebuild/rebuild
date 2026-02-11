/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.support.i18n.Language;

/**
 * 配置管理器
 *
 * @author devezhao
 * @since 01/04/2019
 */
public interface ConfigManager {

    /**
     * 已删除项标记
     */
    String DELETED_ITEM = "$$RB_DELETED$$";

    /**
     * 清理缓存
     *
     * @param cacheKey
     */
    void clean(Object cacheKey);

    /**
     * 获取配置所属实体
     *
     * @param cfgid
     * @param throwIfMiss
     * @return
     */
    default String getBelongEntity(ID cfgid, boolean throwIfMiss) {
        String ckey = "ConfigBelongEntity-" + cfgid;
        String belongEntity = Application.getCommonsCache().get(ckey);
        if (belongEntity == null) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(cfgid, "belongEntity");
            if (o == null) {
                if (throwIfMiss) {
                    String e = Language.L("无效配置，可能已被删除") + " (" + cfgid.toLiteral().toUpperCase() + ")";
                    throw new ConfigurationException(e);
                }
                return null;
            }

            belongEntity = (String) o[0];
            Application.getCommonsCache().put(ckey, belongEntity);
        }
        return belongEntity;
    }
}
