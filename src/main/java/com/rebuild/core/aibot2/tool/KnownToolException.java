/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

/**
 * 工具执行中的已知业务异常（如参数校验失败、实体不存在等，区别于非预期的系统异常）
 *
 * @author devezhao
 * @since 2026/8/16
 * @see com.rebuild.web.KnownExceptionConverter
 */
public class KnownToolException extends ToolException {
    private static final long serialVersionUID = 6277346236693464472L;

    public KnownToolException(String message) {
        super(message);
    }

    public KnownToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
