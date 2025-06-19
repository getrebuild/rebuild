/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.integration;

import com.rebuild.TestSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * @author devezhao
 * @since 2019/03/08
 */
@Disabled
public class SMSenderTest extends TestSupport {

    @Test
    public void testSendSMS() {
        if (!SMSender.availableSMS()) return;

        SMSender.sendSMS("17187472172", "SMSenderTest#testSendSMS");
    }

    @Test
    void testSendMail() throws Exception {
        if (!SMSender.availableMail()) return;

        SMSender.sendMail(
                "getrebuild@sina.com", "SMSenderTest#testSendMail", "test content");

        SMSender.sendMail(
                "getrebuild@sina.com", "SMSenderTest#testSendMail", "test content with attach",
                new File[]{ResourceUtils.getFile("classpath:logback.xml")});
    }
}
