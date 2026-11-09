/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;

import java.io.File;
import java.util.List;

/**
 * 程序调用提问参数（ChatManager#ask）
 *
 * @author RB
 * @since 4.6
 */
public class AskRequest {

    private final String userContent;
    private String prompt;
    private List<File> files;
    private String source;
    private ID chatid;
    private AibotAgent agent;
    private ID user;

    /**
     * @param userContent 提问内容
     */
    public AskRequest(String userContent) {
        this.userContent = userContent;
    }

    public AskRequest setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public AskRequest setFiles(List<File> files) {
        this.files = files;
        return this;
    }

    public AskRequest setSource(String source) {
        this.source = source;
        return this;
    }

    public AskRequest setChatid(ID chatid) {
        this.chatid = chatid;
        return this;
    }

    public AskRequest setAgent(AibotAgent agent) {
        this.agent = agent;
        return this;
    }

    public AskRequest setUser(ID user) {
        this.user = user;
        return this;
    }

    public String getUserContent() {
        return userContent;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<File> getFiles() {
        return files;
    }

    public String getSource() {
        return source;
    }

    public ID getChatid() {
        return chatid;
    }

    public AibotAgent getAgent() {
        return agent;
    }

    public ID getUser() {
        return user;
    }
}
