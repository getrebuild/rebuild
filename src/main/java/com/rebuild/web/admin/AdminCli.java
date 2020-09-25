/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.Installer;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/3/16
 */
public class AdminCli {

    private static final String C_HELP = "help";
    private static final String C_CACHE = "cache";
    private static final String C_SYSCFG = "syscfg";

    final private String[] commands;

    /**
     * @param args
     */
    protected AdminCli(String args) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(args);
        while (m.find()) {
            String a = m.group(1);
            if (a.startsWith("\"") && a.endsWith("\"")) {
                a = a.substring(1, a.length() - 1);
            }
            list.add(a);
        }
        this.commands = list.toArray(new String[0]);
    }

    /**
     * @return
     */
    public String exec() {
        if (this.commands.length == 0) return "Bad command";

        String result = null;
        switch (commands[0]) {
            case C_HELP: {
                result = this.execHelp();
                break;
            }
            case C_CACHE: {
                result = this.execCache();
                break;
            }
            case C_SYSCFG: {
                result = this.execSyscfg();
                break;
            }
            default: {
                // NOOP
            }
        }

        return StringUtils.defaultIfBlank(result, "Unknown command : " + commands[0]);
    }

    /**
     * @return
     * @see #C_HELP
     */
    protected String execHelp() {
        return " Usage : \ncache ACTION \nsyscfg NAME VALUE";
    }

    /**
     * @return
     * @see #C_CACHE
     */
    protected String execCache() {
        if (commands.length < 2) return "Bad arguments";

        String result = "OK";

        String name = commands[1];
        if ("clean".equals(name)) {
            Installer.clearAllCache();
        } else {
            result = "Bad arguments";
        }

        return result;
    }

    /**
     * @return
     * @see #C_SYSCFG
     * @see ConfigurationItem
     */
    protected String execSyscfg() {
        if (commands.length < 3) return "Bad arguments";

        String name = commands[1];
        String value = commands[2];
        try {
            ConfigurationItem item = ConfigurationItem.valueOf(name);
            RebuildConfiguration.set(item, value);
            return "OK";

        } catch (IllegalArgumentException ex) {
            return "Bad arguments [1] : " + name;
        }
    }
}
