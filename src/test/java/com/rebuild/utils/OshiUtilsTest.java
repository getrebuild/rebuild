package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

class OshiUtilsTest {

    @Test
    void getSI() {
        System.out.println(OshiUtils.getSI());
    }

    @Test
    void getOsMemoryUsed() {
        System.out.println(Arrays.toString(OshiUtils.getOsMemoryUsed()));
    }

    @Test
    void getSystemLoad() {
        System.out.println(OshiUtils.getSystemLoad());
    }

    @Test
    void getLocalIp() {
        System.out.println(OshiUtils.getLocalIp());
    }
}