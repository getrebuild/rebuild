/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.ObjectUtils;
import org.apache.commons.lang.BooleanUtils;

/**
 * 命令行参数
 *
 * @author RB
 * @since 2023/2/4
 * @see ConfigurationItem
 * @see cn.devezhao.bizz.privileges.impl.BizzDepthEntry
 */
public class CommandArgs {

    public static final String rbdev = "rbdev";
    public static final String rbpass = "rbpass";

    public static final String _ForceTour = "_ForceTour";
    public static final String _HeavyStopWatcher = "_HeavyStopWatcher";
    public static final String _BizzReadDepth = "_BizzReadDepth";  // L,D,G

    /**
     * @param name
     * @return default false
     */
    public static boolean getBoolean(String name) {
        return BooleanUtils.toBoolean(System.getProperty(name));
    }

    /**
     * @param name
     * @return default -1
     */
    public static int getInteger(String name) {
        return ObjectUtils.toInt(System.getProperty(name), -1);
    }

    /**
     * @param name
     * @return
     */
    public static String getString(String name) {
        return System.getProperty(name);
    }
}
