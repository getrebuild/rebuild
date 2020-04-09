/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.rebuild.server.TestSupport;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/3/11
 */
public class AuthTokenManagerTest extends TestSupport {

    @Test
    public void tokenLifecycle() {
        String newToken = AuthTokenManager.generateToken(SIMPLE_USER, 60);
        Assert.assertNotNull(AuthTokenManager.verifyToken(newToken, false));
        Assert.assertNotNull(AuthTokenManager.verifyToken(newToken, false));

        // renew
        AuthTokenManager.refreshToken(newToken, 60);

        // destroy
        AuthTokenManager.verifyToken(newToken, true);
        Assert.assertNull(AuthTokenManager.verifyToken(newToken, false));
    }
}