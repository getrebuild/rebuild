/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.exception.jdbc.ConstraintViolationException;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 02/19/2023
 */
class QueryDecoratorTest extends TestSupport {

    @Test
    void test() {
        ID id1 = null;
        while (id1 == null) {
            try {
                id1 = addRecordOfTestAllFields(UserService.SYSTEM_USER);
            } catch (ConstraintViolationException ex) {
                _log.error(ex.getLocalizedMessage());
            }
        }
        final ID id2 = addRecordOfTestAllFields(UserService.SYSTEM_USER);
        final ID id3 = addRecordOfTestAllFields(UserService.SYSTEM_USER);

        Record id3Record = EntityHelper.forUpdate(id3);
        id3Record.setIDArray("N2NREFERENCE", new ID[] { id1, id2 });
        UserContextHolder.replaceUser(UserService.SYSTEM_USER);
        try {
            Application.getGeneralEntityService().update(id3Record);
        } finally {
            UserContextHolder.restoreUser();
        }

        Record r = Application.createQueryNoFilter(
                "select N2NREFERENCE,TestAllFieldsId from TestAllFields where TestAllFieldsId = ?")
                .setParameter(1, id3)
                .record();
        Assertions.assertEquals(2, r.getIDArray("N2NREFERENCE").length);
    }
}