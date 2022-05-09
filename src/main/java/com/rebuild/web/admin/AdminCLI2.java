/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.DataFileBackup;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.utils.AES;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/3/16
 */
public class AdminCLI2 {

    private static final String C_HELP = "help";
    private static final String C_CACHE = "cache";
    private static final String C_SYSCFG = "syscfg";
    private static final String C_BACKUP = "backup";
    private static final String C_AES = "aes";

    final private String[] commands;

    /**
     * @param args
     */
    protected AdminCLI2(String args) {
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
            case C_HELP:
            case "?" : {
                result = " Usage : " +
                        " \ncache [clean]" +
                        " \nsyscfg NAME [VALUE]" +
                        " \nbackup [database|datafile]" +
                        " \naes [decrypt] [VALUE]";
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
            case C_BACKUP: {
                result = this.execBackup();
                break;
            }
            case C_AES: {
                result = this.execAes();
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
     * @see ConfigurationItem
     */
    protected String execSyscfg() {
        if (commands.length < 2) return "Bad arguments";

        String name = commands[1];
        try {
            ConfigurationItem item = ConfigurationItem.valueOf(name);
            // Get
            if (commands.length <= 2) {
                return RebuildConfiguration.get(item);
            }

            String value = commands[2];
            RebuildConfiguration.set(item, value);
            return "OK";

        } catch (IllegalArgumentException ex) {
            return "Bad arguments [1] : " + name;
        }
    }

    /**
     * @return
     */
    protected String execBackup() {
        String type = commands.length > 1 ? commands[1] : null;

        List<String> result = new ArrayList<>(2);
        try {
            if (type == null || "database".equals(type)) {
                File backup = new DatabaseBackup().backup();
                result.add("Backup database : " + backup);
            }
            if (type == null || "datafile".equals(type)) {
                File backup = new DataFileBackup().backup();
                result.add("Backup datafile : " + backup);
            }

            return result.isEmpty() ? "Nothing backup" : StringUtils.join(result, "\n");

        } catch (Exception ex) {
            return "Exec failed : " + ex.getLocalizedMessage();
        }
    }

    /**
     * @return
     */
    protected String execAes() {
        if (commands.length < 2) return "Bad arguments";

        String value = commands.length > 2 ? commands[2] : commands[1];
        if ("decrypt".equalsIgnoreCase(commands[1])) {
            return AES.decryptQuietly(value);
        } else {
            return AES.encrypt(value);
        }
    }
}
