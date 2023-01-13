/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.service.approval.ApprovalState;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/8/26
 */
class LanguageTest extends TestSupport {

    @Test
    void getLang() {
        System.out.println(Language.L("首页"));
        System.out.println(Language.L("%s首页", "谁的"));
        System.out.println(Language.L("%d条记录", 100));

        Entity entity = MetadataHelper.getEntity(EntityHelper.User);
        System.out.println(Language.L(entity));
        System.out.println(Language.L(entity.getField(EntityHelper.CreatedOn)));

        System.out.println(Language.L(ApprovalState.PROCESSING));
        System.out.println(Language.L(DisplayType.ANYREFERENCE));
    }

    @Test
    void getMdLang() {
        System.out.println(Language.L("**加粗**"));
        System.out.println(Language.L("换行 [] 第二行"));
        System.out.println(Language.L("这是一个 [链接](https://getrebuild.com/)"));
    }

    @Test
    void available() {
        System.out.println(Application.getLanguage().available(""));
        System.out.println(Application.getLanguage().available("zh"));
        System.out.println(Application.getLanguage().available("zh-cn"));
        System.out.println(Application.getLanguage().available("zh-hk"));
        System.out.println(Application.getLanguage().available("zh-tw"));
    }
}