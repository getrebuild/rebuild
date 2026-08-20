/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

/**
 * 商业版可用 Tool
 *
 * @author devezhao
 * @since 2026/8/20
 */
public abstract class RbvTool implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        throw new KnownToolException("本功能为商业版功能，当前版本不支持。"
                + "请告知用户升级商业版后使用，详见 https://getrebuild.com/docs/rbv-features");
    }
}
