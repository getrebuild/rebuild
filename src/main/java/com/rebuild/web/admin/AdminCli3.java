/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.core.Application;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.core.support.setup.DatafileBackup;
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
public class AdminCli3 {

    private static final String C_HELP = "help";
    private static final String C_CACHE = "cache";
    private static final String C_SYSCFG = "syscfg";
    private static final String C_BACKUP = "backup";
    private static final String C_AES = "aes";

    private static final String SUCCESS = "OK";

    final private String[] commands;

    /**
     * @param args
     */
    protected AdminCli3(String args) {
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
        if (this.commands.length == 0) return "WRAN: Bad command";

        String result = null;
        switch (commands[0]) {
            case C_HELP:
            case "?" : {
                result = " Usage : " +
                        " \ncache [clean|get] [KEY]" +
                        " \nsyscfg NAME [VALUE]" +
                        " \nsyscfg clean-qiniu|clean-sms|clean-email|clean-wxwork|clean-dingtalk" +
                        " \nbackup [database|datafile]" +
                        " \naes [decrypt] VALUE";
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

        return StringUtils.defaultIfBlank(result, "WRAN: Unknown command : `" + commands[0] + "`");
    }

    /**
     * 缓存
     *
     * @return
     */
    protected String execCache() {
        if (commands.length < 2) return "WRAN: Bad arguments";

        String result = SUCCESS;

        String name = commands[1];
        if ("clean".equals(name)) {
            Installer.clearAllCache();
        } else if ("get".equals(name)) {
            if (commands.length < 3) return "WRAN: Bad arguments";
            String key = commands[2];
            Object value = Application.getCommonsCache().getx(key);
            if (value == null) result = "/NULL/";
            else result = value.toString();
        } else {
            result = "WRAN: Bad arguments";
        }

        return result;
    }

    /**
     * 系统配置项
     *
     * @return
     * @see ConfigurationItem
     */
    protected String execSyscfg() {
        if (commands.length < 2) return "WRAN: Bad arguments";

        String name = commands[1];
        try {
            if ("clean-qiniu".equals(name)) {
                removeItems("Storage");
                return SUCCESS;
            } else if ("clean-sms".equals(name)) {
                removeItems("Sms");
                return SUCCESS;
            } else if ("clean-email".equals(name)) {
                removeItems("Mail");
                return SUCCESS;
            } else if ("clean-wxwork".equals(name)) {
                removeItems("Wxwork");
                return SUCCESS;
            } else if ("clean-dingtalk".equals(name)) {
                removeItems("Dingtalk");
                return SUCCESS;
            }

            ConfigurationItem item = ConfigurationItem.valueOf(name);
            // Getter
            if (commands.length == 2) {
                return RebuildConfiguration.get(item);
            }
            // Setter
            else {
                String value = commands[2];
                RebuildConfiguration.set(item, value);
                return "OK";
            }

        } catch (IllegalArgumentException ex) {
            return "WRAN: Bad arguments [1] : " + name;
        }
    }

    private void removeItems(String itemPrefix) {
        for (ConfigurationItem i : ConfigurationItem.values()) {
            if (i.name().startsWith(itemPrefix)) RebuildConfiguration.set(i, RebuildConfiguration.SETNULL);
        }
    }

    /**
     * 备份
     *
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
                File backup = new DatafileBackup().backup();
                result.add("Backup datafile : " + backup);
            }

            return result.isEmpty() ? "WRAN: Nothing to backup" : StringUtils.join(result, "\n");

        } catch (Exception ex) {
            return "WRAN: Exec failed `" + ex.getLocalizedMessage() + "`";
        }
    }

    /**
     * 加密/解密
     *
     * @return
     */
    protected String execAes() {
        if (commands.length < 2) return "WRAN: Bad arguments";

        String value = commands.length > 2 ? commands[2] : commands[1];
        if ("decrypt".equalsIgnoreCase(commands[1])) {
            return AES.decryptQuietly(value);
        } else {
            return AES.encrypt(value);
        }
    }
}
