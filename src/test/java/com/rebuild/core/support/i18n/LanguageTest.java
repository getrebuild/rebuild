/*
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

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * @author devezhao
 * @since 2020/8/26
 */
public class LanguageTest extends TestSupport {

    @Test
    public void getLang() {
        System.out.println($L("首页"));
        System.out.println($L("%s首页", "谁的"));
        System.out.println($L("%d条数据", 100));

        Entity entity = MetadataHelper.getEntity(EntityHelper.User);
        System.out.println($L(entity));
        System.out.println($L(entity.getField(EntityHelper.CreatedOn)));

        System.out.println($L(ApprovalState.PROCESSING));
        System.out.println($L(DisplayType.ANYREFERENCE));
    }

    @Test
    public void getMdLang() {
        System.out.println($L("**加粗**"));
        System.out.println($L("换行 [] 第二行"));
        System.out.println($L("这是一个 [链接](https://getrebuild.com/)"));
    }

    @Test
    public void available() {
        System.out.println(Application.getLanguage().available(""));
        System.out.println(Application.getLanguage().available("zh"));
        System.out.println(Application.getLanguage().available("zh-cn"));
        System.out.println(Application.getLanguage().available("zh-hk"));
        System.out.println(Application.getLanguage().available("zh-tw"));
    }
}