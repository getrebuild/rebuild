package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Field;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void checkoutDateValue() {
        RecordCheckout checkout = new RecordCheckout(null);

        assertNotNull(checkout.checkoutDateValue(new Cell("2019-01-01 23:59:59")));
        assertNotNull(checkout.checkoutDateValue(new Cell("2019-01-01")));
        assertNotNull(checkout.checkoutDateValue(new Cell("2019年01月01日 23分59分59秒")));
        assertNotNull(checkout.checkoutDateValue(new Cell("2019年01月01日")));
        assertNotNull(checkout.checkoutDateValue(new Cell("2019/01/01 23:59:59")));
        assertNotNull(checkout.checkoutDateValue(new Cell("2019/01/01")));
        assertNotNull(checkout.checkoutDateValue(new Cell("2024/2/26")));
    }
}