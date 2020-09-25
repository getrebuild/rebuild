/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.NavBuilder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.utils.JSONUtils;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/8/26
 */
public class LanguageTest extends TestSupport {

    @Test
    public void getLang() {
        System.out.println("Home > " + Language.getLang("Home"));
        System.out.println("AddSome > " + Language.getLang("AddSome", "Home"));
        System.out.println("HasXNotice > " + Language.formatLang("HasXNotice", 99));

        Entity entity = MetadataHelper.getEntity(EntityHelper.User);
        System.out.println("e.User > " + Language.getLang(entity));
        System.out.println("f.createdOn > " + Language.getLang(entity.getField(EntityHelper.CreatedOn)));
        System.out.println("f.User.fullName > " + Language.getLang(entity.getField("fullName")));
        System.out.println("s.ApprovalState.PROCESSING > " + Language.getLang(ApprovalState.PROCESSING));
    }

    @Test
    public void getMdLang() {
        System.out.println("Support MD > " + Language.getLang("SystemFailureMemo"));
    }

    @Test
    public void replaceLangKey() {
        JSON resource = NavBuilder.instance.getNavPortal(UserService.ADMIN_USER);
        resource = Application.getLanguage().getDefaultBundle().replaceLangKey(resource);
        System.out.println(JSONUtils.prettyPrint(resource));
    }
}