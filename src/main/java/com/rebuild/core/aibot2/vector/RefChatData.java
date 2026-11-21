/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.vector;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.aibot2.ChatManager;
import org.apache.commons.lang3.StringUtils;

/**
 * 引用历史会话（AI 压缩后输出）
 *
 * @author Zixin
 * @since 2026/9/12
 */
public class RefChatData implements VectorData {

    private final ID chatid;

    public RefChatData(ID chatid) {
        this.chatid = chatid;
    }

    @Override
    public String toVector() {
        return StringUtils.defaultString(ChatManager.summarizeChat(chatid));
    }
}
