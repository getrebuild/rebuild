package com.rebuild.core.service.aibot.tool;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Zixin
 * @since 2026/6/9
 */
@Slf4j
public class File2Record implements Tool {

    @Override
    public Object tool(String arguments) {
        log.info("exec : {}", arguments);
        throw new UnsupportedOperationException();
    }
}
