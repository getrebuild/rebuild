/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2025/4/15
 */
public class Config {

    public static String getServerUrl(String path) {
        String url = RebuildConfiguration.get(ConfigurationItem.AibotDSUrl);
        if (path != null) url += "/" + path;
        return url.replace("//", "/");
    }

    public static String getSecret() {
        String sk = RebuildConfiguration.get(ConfigurationItem.AibotDSSecret);
        Assert.notNull(sk, "[AibotDSSecret] is not set");
        return sk;
    }

    public static String getBasePrompt() {
        return RebuildConfiguration.get(ConfigurationItem.AibotBasePrompt);
    }
}
