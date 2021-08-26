package com.rebuild.core;

import org.junit.jupiter.api.Test;

class ServerStatusTest {

    @Test
    void oshi() {
        double[] vmMemory = ServerStatus.getJvmMemoryUsed();
        System.out.println(vmMemory[0] + ", " + vmMemory[1]);

        System.out.println(ServerStatus.getSystemLoad());
        System.out.println(ServerStatus.getLocalIp());
    }
}