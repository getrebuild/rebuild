/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.TsetEntity;
import com.rebuild.core.rbstore.RbSystemImporter;
import com.rebuild.core.service.approval.ApprovalFields2Schema;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.DatabaseBackup;
import com.rebuild.core.support.setup.DatafileBackup;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.AES;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
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
@Slf4j
public class AdminCli3 {

    private static final String C_HELP = "help";
    private static final String C_CACHE = "cache";
    private static final String C_SYSCFG = "syscfg";
    private static final String C_BACKUP = "backup";
    private static final String C_AES = "aes";
    private static final String C_CLEAN_APPROVAL = "clean-approval";
    private static final String C_ADD_TESTENTITY = "add-testentity";
    private static final String C_RBSPKG = "rbspkg";

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
                        " \nsyscfg clean-qiniu|clean-sms|clean-email|clean-wxwork|clean-dingtalk|clean-feishu" +
                        " \nbackup [database|datafile]" +
                        " \naes [decrypt] VALUE" +
                        " \nclean-approval ENTITY" +
                        " \nadd-testentity [NAME]" +
                        " \nrbspkg URL";
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
            case C_CLEAN_APPROVAL : {
                result = this.execCleanApproval();
                break;
            }
            case C_ADD_TESTENTITY : {
                result = this.execAddTestentity();
                break;
            }
            case C_RBSPKG: {
                result = this.execRbspkg();
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
            } else if ("clean-feishu".equals(name)) {
                removeItems("Feishu");
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
                if (item == ConfigurationItem.SN) {
                    String usql = String.format("update system_config set `VALUE` = '%s' where `ITEM` = 'SN'",
                            StringEscapeUtils.escapeSql(value));
                    Application.getSqlExecutor().execute(usql);
                    // reset: RB NEED RESTART
                    Application.getCommonsCache().evict(ConfigurationItem.SN.name());
                    License.siteApiNoCache("api/authority/query");
                } else {
                    RebuildConfiguration.set(item, value);
                }
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

    /**
     * 删除审批字段
     *
     * @return
     */
    private String execCleanApproval() {
        if (commands.length < 2) return "WRAN: Bad arguments";

        String entity = commands[1];
        if (!MetadataHelper.containsEntity(entity)) {
            return "WRAN: No entity exists";
        }

        boolean o = new ApprovalFields2Schema().dropFields(MetadataHelper.getEntity(entity));
        return o ? "OK" : "WRAN: Drop error";
    }

    /**
     *
     * @return
     */
    private String execAddTestentity() {
        String name = commands.length > 1 ? commands[1] : "TestAllFields999";
        String entityName = new TsetEntity().create(name);

        if (entityName.startsWith("EXISTS:")) return "WRAN: " + entityName;
        else return "OK: " + entityName;
    }

    /**
     * @return
     */
    private String execRbspkg() {
        if (commands.length < 2) return "WRAN: Bad arguments";

        String fileUrl = commands[1];
        RbSystemImporter importer = new RbSystemImporter(fileUrl);
        try {
            TaskExecutors.run(importer);
            return "OK";
        } catch (Exception ex) {
            log.error("RBSPKG", ex);
            return "ERROR: " + ex.getLocalizedMessage();
        }
    }
}
