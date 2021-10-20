package com.rebuild.api.user;

import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class PageTokenVerifyTest extends TestSupport {

    @Test
    void execute() {
        String token = PageTokenVerify.generate(UserService.ADMIN_USER);
        ApiContext apiContext = new ApiContext(Collections.singletonMap("token", token), null);

        JSON ret = new PageTokenVerify().execute(apiContext);
        System.out.println(ret);
    }

    @Test
    void executeErr() {
        ApiContext apiContext = new ApiContext(Collections.singletonMap("token", "fake"), null);

        Assertions.assertThrows(ApiInvokeException.class, () -> new PageTokenVerify().execute(apiContext));
    }
}