/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 2026/10/19
 */
public class ConfigNotFoundException extends ConfigurationException{
    private static final long serialVersionUID = -3042946221889770934L;

    public ConfigNotFoundException(String msg) {
        super(msg);
    }

    public ConfigNotFoundException(ID configId) {
        super("Config not found:" + configId);
    }
}
