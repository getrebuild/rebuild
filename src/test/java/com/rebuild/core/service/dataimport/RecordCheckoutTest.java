package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Field;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

class RecordCheckoutTest extends TestSupport {

    @Test
    void checkoutReferenceValue() {
        RecordCheckout checkout = new RecordCheckout(null);

        Field relatedAccount = MetadataHelper.getEntity(SalesOrder).getField("relatedAccount");
        checkout.checkoutReferenceValue(relatedAccount, new Cell("123"));

        Field owningUser = MetadataHelper.getEntity(SalesOrder).getField(EntityHelper.OwningUser);
        checkout.checkoutReferenceValue(owningUser, new Cell(UserService.ADMIN_USER));
    }

    @Test
    void checkoutMultiSelectValue() {
        RecordCheckout checkout = new RecordCheckout(null);

        Field MULTISELECT = MetadataHelper.getEntity(TestAllFields).getField("MULTISELECT");
        checkout.checkoutMultiSelectValue(MULTISELECT, new Cell("1哈哈； 3"));
    }
}