/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.user;

import com.rebuild.TestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/3/11
 */
public class AuthTokenManagerTest extends TestSupport {

    @Test
    void accessToken() {
        // generate
        String accessToken = AuthTokenManager.generateToken(SIMPLE_USER, 3, null);

        // verify
        Assertions.assertNotNull(AuthTokenManager.verifyToken(accessToken, false));
        Assertions.assertNotNull(AuthTokenManager.verifyToken(accessToken, false));

        // refresh
        AuthTokenManager.refreshAccessToken(accessToken, 3);

        // destroy
        AuthTokenManager.verifyToken(accessToken, true);
        Assertions.assertNull(AuthTokenManager.verifyToken(accessToken, false));
    }

    @Test
    void onceToken() {
        // generate
        String onceToken = AuthTokenManager.generateOnceToken(null);

        // verify
        Assertions.assertNotNull(AuthTokenManager.verifyToken(onceToken, false));
        Assertions.assertNull(AuthTokenManager.verifyToken(onceToken, false));
    }
}