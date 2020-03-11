/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.rebuild.server.TestSupport;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author devezhao
 * @since 2020/3/11
 */
public class LoginTokenManagerTest extends TestSupport {

    @Test
    public void tokenLifecycle() {
        String newToken = LoginTokenManager.generateToken(SIMPLE_USER, 60);
        Assert.assertNotNull(LoginTokenManager.verifyToken(newToken, false));
        Assert.assertNotNull(LoginTokenManager.verifyToken(newToken, false));

        // renew
        LoginTokenManager.refreshToken(newToken, 60);

        // destroy
        LoginTokenManager.verifyToken(newToken, true);
        Assert.assertNull(LoginTokenManager.verifyToken(newToken, false));
    }
}