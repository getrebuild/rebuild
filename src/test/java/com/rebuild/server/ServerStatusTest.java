/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author devezhao
 * @since 2020/06/23
 */
public class ServerStatusTest extends TestSupport {

    @Test
    public void getLastStatus() {
        System.out.println(ServerStatus.getLastStatus());
    }

    @Test
    public void getHeapMemoryUsed() {
        System.out.println(ServerStatus.getHeapMemoryUsed()[1]);
    }

    @Test
    public void getSystemLoad() {
        System.out.println(ServerStatus.getSystemLoad());
    }
}