/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import com.rebuild.core.RebuildException;

/**
 * Exception when configuration unset or incorrect
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/14
 */
public class ConfigurationException extends RebuildException {
    private static final long serialVersionUID = -329453920432770209L;

    public ConfigurationException(String msg) {
        super(msg);
    }
}
