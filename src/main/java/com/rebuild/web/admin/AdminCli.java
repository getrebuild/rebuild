/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.cache.EhcacheDriver;
import com.rebuild.server.helper.cache.JedisCacheDriver;
import org.apache.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/3/16
 */
public class AdminCli {

    protected static final String C_CACHECLEAN = "cacheclean";
    protected static final String C_SYSCFG = "syscfg";

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
        String result = null;
        switch (commands[0]) {
            case C_CACHECLEAN: {
                result = this.execCacheClean();
                break;
            }
            case C_SYSCFG: {
                result = this.execSysCfg();
                break;
            }
        }

        return StringUtils.defaultIfBlank(result, "Unknow command : " + commands[0]);
    }

    /**
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected String execCacheClean() {
        if (Application.getCommonCache().isUseRedis()) {
            try (Jedis jedis = ((JedisCacheDriver) Application.getCommonCache().getCacheTemplate()).getJedisPool().getResource()) {
                jedis.flushAll();
            }
        } else {
            ((EhcacheDriver) Application.getCommonCache().getCacheTemplate()).cache().clear();
        }
        return "OK";
    }

    /**
     * @return
     */
    protected String execSysCfg() {
        if (commands.length < 3) {
            return "Bad arguments";
        }

        String name = commands[1];
        String value = commands[2];
        try {
            ConfigurableItem item = ConfigurableItem.valueOf(name);
            SysConfiguration.set(item, value);
            return "OK";

        } catch (IllegalArgumentException ex) {
            return "Bad arguments [1] : " + name;
        }
    }
}
