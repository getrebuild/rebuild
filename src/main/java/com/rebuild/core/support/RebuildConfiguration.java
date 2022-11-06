/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.core.RebuildException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局系统配置
 *
 * @author devezhao
 * @see ConfigurationItem
 * @see com.rebuild.core.BootEnvironmentPostProcessor
 * @since 10/14/2018
 */
@Slf4j
public class RebuildConfiguration extends KVStorage {

    /**
     * 获取数据目录下的文件（或目录）
     *
     * @param filepath
     * @return
     */
    public static File getFileOfData(String filepath) {
        if (filepath != null && filepath.contains("../")) {
            throw new SecurityException("Attack path detected : " + filepath);
        }

        String d = get(ConfigurationItem.DataDirectory);
        File datadir = null;
        if (StringUtils.isNotBlank(d)) {
            datadir = new File(d);
            if (!datadir.exists() && !datadir.mkdirs()) {
                log.error("Cannot mkdir for data directory : {}", datadir);
            }
        }

        if (datadir == null || !datadir.exists()) {
            datadir = FileUtils.getUserDirectory();
            datadir = new File(datadir, ".rebuild");
            if (!datadir.exists() && !datadir.mkdirs()) {
                log.error("Cannot mkdir for data directory : {}", datadir);
            }
        }

        if (!datadir.exists()) {
            throw new RebuildException("No data directory exists!");
        }
        
        return filepath == null ? datadir : new File(datadir, filepath);
    }

    /**
     * 获取临时文件（或目录）
     *
     * @param filepath
     * @return
     * @see #getFileOfData(String)
     */
    public static File getFileOfTemp(String filepath) {
        if (filepath != null && filepath.contains("../")) {
            throw new SecurityException("Attack path detected : " + filepath);
        }

        File temp = getFileOfData("temp");
        if (!temp.exists()) {
            if (!temp.mkdirs()) {
                throw new RebuildException("Cannot mkdir for temp directory : " + temp);
            }
        }

        return filepath == null ? temp : new File(temp, filepath);
    }

    /**
     * 云存储地址
     *
     * @return
     */
    public static String getStorageUrl() {
        String[] account = getStorageAccount();
        return account == null ? null : account[3];
    }

    /**
     * 云存储账号
     *
     * @return returns [StorageApiKey, StorageApiSecret, StorageBucket, StorageURL]
     */
    public static String[] getStorageAccount() {
        return getsNoUnset(true,
                ConfigurationItem.StorageApiKey, ConfigurationItem.StorageApiSecret,
                ConfigurationItem.StorageBucket, ConfigurationItem.StorageURL);
    }

    /**
     * 邮件账号
     *
     * @return returns [MailUser, MailPassword, MailAddr, MailName, MailCc, MailSmtpServer]
     */
    public static String[] getMailAccount() {
        String[] set = getsNoUnset(false,
                ConfigurationItem.MailUser, ConfigurationItem.MailPassword, ConfigurationItem.MailAddr, ConfigurationItem.MailName);
        if (set == null) return null;

        String cc = get(ConfigurationItem.MailCc);
        String smtpServer = get(ConfigurationItem.MailSmtpServer);

        return new String[] {
                set[0], set[1], set[2], set[3],
                StringUtils.defaultIfBlank(cc, null),
                StringUtils.defaultIfBlank(smtpServer, null)
        };
    }

    /**
     * 短信账号
     *
     * @return returns [SmsUser, SmsPassword, SmsSign]
     */
    public static String[] getSmsAccount() {
        return getsNoUnset(false,
                ConfigurationItem.SmsUser, ConfigurationItem.SmsPassword, ConfigurationItem.SmsSign);
    }

    /**
     * 获取绝对 URL
     *
     * @return
     */
    public static String getHomeUrl() {
        return getHomeUrl(null);
    }

    /**
     * 获取绝对 URL
     *
     * @param path 可带有路径，会自动拼接
     * @return
     */
    public static String getHomeUrl(String path) {
        String homeUrl = get(ConfigurationItem.HomeURL);
        if (path != null) homeUrl = joinPath(homeUrl, path);
        else if (!homeUrl.endsWith("/")) homeUrl += "/";
        return homeUrl;
    }

    /**
     * 获取绝对 URL H5
     *
     * @param path
     * @return
     * @see #getHomeUrl(String)
     */
    public static String getMobileUrl(String path) {
        String mobileUrl = BootEnvironmentPostProcessor.getProperty(ConfigurationItem.MobileUrl.name());
        if (mobileUrl != null) {
            return path == null ? mobileUrl : joinPath(mobileUrl, path);
        }

        mobileUrl = "/h5app/";
        if (path != null) mobileUrl = joinPath(mobileUrl, path);
        return getHomeUrl(mobileUrl);
    }

    static String joinPath(String path1, String path2) {
        if (path1.endsWith("/")) path1 = path1.substring(0, path1.length() - 1);
        if (path2.startsWith("/")) path2 = path2.substring(1);
        return path1 + "/" + path2;
    }

    /**
     * 获取多个，任意一个为空都返回 null
     *
     * @param noCache
     * @param items
     * @return
     */
    static String[] getsNoUnset(boolean noCache, ConfigurationItem... items) {
        List<String> list = new ArrayList<>();
        for (ConfigurationItem item : items) {
            String v = get(item, noCache);
            if (StringUtils.isBlank(v)) {
                return null;
            }
            list.add(v);
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param name
     * @return
     */
    public static String get(ConfigurationItem name) {
        return get(name, false);
    }

    /**
     * @param name
     * @param noCache
     * @return
     */
    public static String get(ConfigurationItem name, boolean noCache) {
        return getValue(name.name(), noCache, name.getDefaultValue());
    }

    /**
     * @param name
     * @return
     */
    public static int getInt(ConfigurationItem name) {
        String s = get(name);
        return s == null
                ? ObjectUtils.defaultIfNull((Integer) name.getDefaultValue(), 0)
                : NumberUtils.toInt(s);
    }

    /**
     * @param name
     * @return
     */
    public static long getLong(ConfigurationItem name) {
        String s = get(name);
        return s == null
                ? ObjectUtils.defaultIfNull((Long) name.getDefaultValue(), 0L)
                : NumberUtils.toLong(s);
    }

    /**
     * @param name
     * @return
     */
    public static boolean getBool(ConfigurationItem name) {
        String s = get(name);
        return s == null
                ? ObjectUtils.defaultIfNull((Boolean) name.getDefaultValue(), false)
                : BooleanUtils.toBoolean(s);
    }

    /**
     * @param name
     * @param value
     * @return
     */
    public static void set(ConfigurationItem name, Object value) {
        if (ConfigurationItem.DataDirectory == name || ConfigurationItem.RedisDatabase == name) {
            throw new SecurityException("Attack configuration detected : " + name + "=" + value);
        }
        setValue(name.name(), value);
    }
}
