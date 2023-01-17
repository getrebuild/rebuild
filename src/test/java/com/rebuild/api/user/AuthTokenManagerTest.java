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

    @SuppressWarnings("deprecation")
    @Test
    void accessToken() {
        // Generate
        String accessToken = AuthTokenManager.generateToken(SIMPLE_USER, 3, null);

        // Verify
        Assertions.assertNotNull(AuthTokenManager.verifyToken(accessToken));
        Assertions.assertNotNull(AuthTokenManager.verifyToken(accessToken));

        // Refresh
        AuthTokenManager.refreshAccessToken(accessToken);

        // Destroy
        AuthTokenManager.verifyToken(accessToken, true, true);
        Assertions.assertNull(AuthTokenManager.verifyToken(accessToken));
    }

    @Test
    void onceToken() {
        // Generate
        String onceToken = AuthTokenManager.generateOnceToken(null);

        // Verify
        Assertions.assertNotNull(AuthTokenManager.verifyToken(onceToken));
        Assertions.assertNull(AuthTokenManager.verifyToken(onceToken));
    }
}