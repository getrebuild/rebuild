/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.rebuild.TestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/3/11
 */
public class AuthTokenManagerTest extends TestSupport {

    @Test
    public void tokenLifecycle() {
        String newToken = AuthTokenManager.generateToken(SIMPLE_USER, 60);
        Assertions.assertNotNull(AuthTokenManager.verifyToken(newToken, false));
        Assertions.assertNotNull(AuthTokenManager.verifyToken(newToken, false));

        // renew
        AuthTokenManager.refreshToken(newToken, 60);

        // destroy
        AuthTokenManager.verifyToken(newToken, true);
        Assertions.assertNull(AuthTokenManager.verifyToken(newToken, false));
    }



}