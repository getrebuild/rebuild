/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;

/**
 * 程序调用提问参数（ChatManager#ask）
 *
 * @author RB
 * @since 4.6
 */
@Getter
@Accessors(chain = true)
public class AskRequest {

    private final String userContent;

    @Setter
    private String prompt;
    @Setter
    private List<File> files;
    @Setter
    private String source;
    @Setter
    private ID chatid;
    @Setter
    private AibotAgent agent;
    @Setter
    private ID user;

    /**
     * @param userContent 提问内容
     */
    public AskRequest(String userContent) {
        this.userContent = userContent;
    }
}
