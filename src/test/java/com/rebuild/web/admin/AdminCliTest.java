/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.server.TestSupport;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author devezhao
 * @since 2020/3/16
 */
public class AdminCliTest extends TestSupport {

    @Test
    public void exec() {
        System.out.println(new AdminCli("cacheclean").exec());
        System.out.println(new AdminCli("syscfg AdminDangers false").exec());
        System.out.println(new AdminCli("abc123").exec());
    }
}