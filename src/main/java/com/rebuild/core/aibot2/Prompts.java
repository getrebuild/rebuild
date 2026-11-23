/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统提示词加载。内容位于 aibot2/prompts.md，按 &lt;!-- @name --&gt; 分节，支持 {{key}} 占位符
 *
 * @author Zixin
 * @since 2026/11/22
 */
@Slf4j
public class Prompts {

    private static final String RES_FILE = "aibot2/prompts.md";

    private static final Pattern SECTION = Pattern.compile("^<!--\\s*@([\\w-]+)\\s*-->$");

    private static final Set<String> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    private static volatile Map<String, String> cache;

    /**
     * @param name 分节名称
     * @return
     */
    public static String get(String name) {
        Map<String, String> prompts = cache;
        if (prompts == null) {
            synchronized (Prompts.class) {
                prompts = cache;
                if (prompts == null) cache = prompts = load();
            }
        }
        String prompt = prompts.get(name);
        if (prompt == null) {
            if (WARNED_MISSING.add(name)) log.warn("Prompt section not found : {}", name);
            return "";
        }
        return prompt;
    }

    /**
     * 获取提示词并替换占位符
     *
     * @param name 分节名称
     * @param vars 占位符名称与值，依次成对传入
     * @return
     */
    public static String render(String name, Object... vars) {
        String text = get(name);
        if (vars != null) {
            for (int i = 0; i + 1 < vars.length; i += 2) {
                text = StringUtils.replace(text, "{{" + vars[i] + "}}", String.valueOf(vars[i + 1]));
            }
        }
        return text;
    }

    private static Map<String, String> load() {
        String res = CommonsUtils.getStringOfRes(RES_FILE);
        if (res == null) return Collections.emptyMap();

        Map<String, String> prompts = new HashMap<>();
        String name = null;
        StringBuilder content = new StringBuilder();
        for (String line : res.split("\\r?\\n")) {
            Matcher m = SECTION.matcher(line.trim());
            if (m.matches()) {
                put(prompts, name, content);
                name = m.group(1);
                content.setLength(0);
            } else if (name != null) {
                content.append(line).append('\n');
            }
        }
        put(prompts, name, content);
        return prompts;
    }

    private static void put(Map<String, String> prompts, String name, StringBuilder content) {
        if (name == null) return;

        String text = content.toString().trim();
        if (!text.isEmpty()) prompts.put(name, text);
    }
}
