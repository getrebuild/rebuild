/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

/**
 * ToolHelper.isSameEntity 必须做数值比较：getEntityCode() 返回 Integer，
 * 引用比较时超出 -128~127 缓存的实体编码（如用户自定义实体 990+）会误判为不同实体
 *
 * @author RB
 * @since 2026/10/10
 */
class ToolHelperEntityTest {

    @Test
    void isSameEntityMustCompareNumerically() {
        // 用户自定义实体编码 > 127，超出 Integer 缓存
        ID salesOrder = ID.valueOf("995-01a123448ac40041");
        Assertions.assertTrue(ToolHelper.isSameEntity(salesOrder, entity(995)), "同编码实体应判定相等");
        Assertions.assertFalse(ToolHelper.isSameEntity(salesOrder, entity(993)), "不同编码实体应判定不等");

        // 系统实体编码 <= 127（Integer 缓存内）
        ID adminUser = ID.valueOf("001-0000000000000001");
        Assertions.assertTrue(ToolHelper.isSameEntity(adminUser, entity(1)));

        // checkRecordEntity 与该比较同源，大编码实体不应误报
        Assertions.assertDoesNotThrow(() -> ToolHelper.checkRecordEntity(salesOrder, entity(995)));
    }

    /**
     * 仅需 getEntityCode 的实体替身（避免依赖数据库）
     *
     * @param entityCode
     * @return
     */
    private static Entity entity(int entityCode) {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class[]{Entity.class},
                (proxy, method, args) -> {
                    if ("getEntityCode".equals(method.getName())) return Integer.valueOf(entityCode);
                    return null;
                });
    }
}
