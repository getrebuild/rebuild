/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.rebuild.utils.JSONUtils;

import java.io.PrintWriter;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class StreamEcho {

    /**
     * @param text
     * @param writer
     */
    public static void text(String text, PrintWriter writer) {
        echo(text, writer, "text");
    }

    /**
     * @param error
     * @param writer
     */
    public static void html(String error, PrintWriter writer) {
        echo(error, writer, "html");
    }

    /**
     * @param error
     * @param writer
     */
    public static void error(String error, PrintWriter writer) {
        html(String.format("<span style='color:red'>%s</span>", error), writer);
    }

    /**
     * @param content
     * @param writer
     * @param type
     */
    public static void echo(String content, PrintWriter writer, String type) {
        if (type == null) type = "text";
        String echo = String.format("data: %s\n\n", JSONUtils.toJSONObject(type, content).toJSONString());
        writer.write(echo);
        writer.flush();
    }

}
