/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.integration;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

/**
 * 七牛云存储
 *
 * @author Zixin (RB)
 * @since 05/19/2018
 */
@Slf4j
public class QiniuCloud {

    /**
     * 默认配置
     */
    public static final Configuration CONFIGURATION = Configuration.create(Region.autoRegion());

    private static final QiniuCloud INSTANCE = new QiniuCloud();

    public static QiniuCloud instance() {
        return INSTANCE;
    }

    // --

    private final UploadManager UPLOAD_MANAGER = new UploadManager(CONFIGURATION);

    private Auth auth;
    private String bucketName;

    private QiniuCloud() {
        initAuth();
    }

    /**
     * 是否可用
     *
     * @return
     */
    public boolean available() {
        return this.auth != null;
    }

    /**
     */
    public void initAuth() {
        String[] account = RebuildConfiguration.getStorageAccount();
        if (account != null) {
            this.auth = Auth.create(account[0], account[1]);
            this.bucketName = account[2];
        } else {
            log.info("No QiniuCloud configuration! Using local storage.");
        }
    }

    /**
     * @return
     */
    public Auth getAuth() {
        Assert.notNull(auth, "Qiniu account not config");
        return auth;
    }

    /**
     * 文件上传
     *
     * @param file
     * @param fileName
     * @param fops
     * @return
     * @throws IOException
     */
    public String upload(File file, String fileName, String fops) throws IOException {
        String fileKey;
        // v4.1
        if (fileName != null && fileName.startsWith("rb/")) fileKey = fileName;
        else fileKey = formatFileKey(fileName == null ? file.getName() : fileName);

        Response resp = UPLOAD_MANAGER.put(file, fileKey, getUploadToken(fileKey, fops));
        if (resp.isOK()) {
            return fileKey;
        } else {
            log.error("Cannot upload file : {}. Resp: {}", file.getName(), resp);
            return null;
        }
    }

    /**
     * 文件上传
     *
     * @param file
     * @param fileName
     * @return
     * @throws IOException
     */
    public String upload(File file, String fileName) throws IOException {
        return upload(file, fileName, null);
    }

    /**
     * 文件上传
     *
     * @param file
     * @return
     * @throws IOException
     */
    public String upload(File file) throws IOException {
        return upload(file, null);
    }

    /**
     * 从 URL 上传
     *
     * @param url
     * @return
     * @throws Exception
     */
    public String upload(URL url) throws Exception {
        File tmp = OkHttpUtils.readBinary(url.toString());
        if (tmp == null) {
            throw new RebuildException("Cannot read file from URL : " + url);
        }

        try {
            return upload(tmp);
        } finally {
            FileUtils.deleteQuietly(tmp);
        }
    }

    /**
     * 生成访问 URL（有效期 30m）
     *
     * @param filePath
     * @return
     */
    public String makeUrl(String filePath) {
        return makeUrl(filePath, 30 * 60);
    }

    /**
     * 生成访问 URL
     *
     * @param filePath
     * @param seconds
     * @return
     */
    public String makeUrl(String filePath, int seconds) {
        String baseUrl = RebuildConfiguration.getStorageUrl() + filePath;
        // default use HTTPS
        if (baseUrl.startsWith("//")) {
            baseUrl = "https:" + baseUrl;
        }

        long deadline = System.currentTimeMillis() / 1000 + seconds;
        if (seconds > 60) {
            Calendar c = CalendarUtils.getInstance();
            c.add(Calendar.SECOND, seconds);
            c.set(Calendar.SECOND, 59);  // full seconds
            deadline = c.getTimeInMillis() / 1000;
        }

        // `e` "token out of date"
        return getAuth().privateDownloadUrlWithDeadline(baseUrl, deadline);
    }

    /**
     * 移动文件
     *
     * @param newKey
     * @param oldKey
     * @return
     */
    public boolean move(String newKey, String oldKey) {
        BucketManager bucketManager = new BucketManager(getAuth(), CONFIGURATION);
        Response resp;
        try {
            resp = bucketManager.move(this.bucketName, oldKey, this.bucketName, newKey, true);
            if (resp.isOK()) return true;

            throw new RebuildException("Failed to move file : " + newKey + " < " + oldKey + " : " + resp.bodyString());
        } catch (QiniuException e) {
            throw new RebuildException("Failed to move file : " + newKey + " < " + oldKey, e);
        }
    }

    /**
     * 拷贝文件
     *
     * @param fromKey
     * @param toKey
     * @return
     */
    public boolean copy(String fromKey, String toKey) {
        BucketManager bucketManager = new BucketManager(getAuth(), CONFIGURATION);
        Response resp;
        try {
            resp = bucketManager.copy(this.bucketName, fromKey, this.bucketName, toKey, true);
            if (resp.isOK()) return true;

            throw new RebuildException("Failed to copy file : " + toKey + " < " + fromKey + " : " + resp.bodyString());
        } catch (QiniuException e) {
            throw new RebuildException("Failed to copy file : " + toKey + " < " + fromKey, e);
        }
    }

    /**
     * 删除文件
     *
     * @param key
     * @return
     */
    public boolean delete(String key) {
        BucketManager bucketManager = new BucketManager(getAuth(), CONFIGURATION);
        Response resp;
        try {
            resp = bucketManager.delete(this.bucketName, key);
            if (resp.isOK()) return true;

            log.warn("Cannot delete file : {} < {} : {}", this.bucketName, key, resp.bodyString());
            return false;
        } catch (QiniuException e) {
            throw new RebuildException("Failed to delete file : " + this.bucketName + " < " + key, e);
        }
    }

    /**
     * 文件信息
     *
     * @param filePath
     * @return
     */
    public FileInfo stat(String filePath) {
        BucketManager bucketManager = new BucketManager(getAuth(), CONFIGURATION);
        try {
            return bucketManager.stat(this.bucketName, filePath);
        } catch (QiniuException e) {
            log.error("Cannot stat file : {}", filePath);
        }
        return null;
    }

    /**
     * 获取空间大小
     *
     * @return bytes
     */
    @SuppressWarnings("deprecation")
    public long stats() {
        // https://developer.qiniu.com/kodo/3906/statistic-interface
        String time = CalendarUtils.getPlainDateFormat().format(CalendarUtils.now());
        String url = String.format(
                "%s/v6/space?bucket=%s&begin=%s000000&end=%s235959&g=day",
                CONFIGURATION.apiHost(), bucketName, time, time);
        StringMap headers = getAuth().authorization(url);

        try {
            Client client = new Client(CONFIGURATION);
            Response resp = client.get(url, headers);
            if (resp.isOK()) {
                JSONObject map = JSON.parseObject(resp.bodyString());
                return map.getJSONArray("datas").getLong(0);
            }

        } catch (QiniuException e) {
            log.warn(null, e);
        }
        return -1;
    }

    /**
     * 获取上传 Token
     *
     * @param fileKey
     * @param fops 异步预处理
     * @return
     * @see #formatFileKey(String)
     */
    public String getUploadToken(String fileKey, String fops) {
        // 上传策略参见 https://developer.qiniu.com/kodo/manual/1206/put-policy
        int maxSize = RebuildConfiguration.getInt(ConfigurationItem.PortalUploadMaxSize);
        StringMap policy = new StringMap().put("fsizeLimit", FileUtils.ONE_MB * maxSize);
        if (fops != null) policy.put("persistentOps", fops).put("persistentNotifyUrl", "https://webhook.site/e2784dd3-cf2c-49ce-8d53-05666e7f5bd0");

        return getAuth().uploadToken(bucketName, fileKey, 3600L, policy, true);
    }

    /**
     * 获取上传 Token
     *
     * @param fileKey
     * @return
     */
    public String getUploadToken(String fileKey) {
        return getUploadToken(fileKey, null);
    }

    /**
     * 下载文件
     *
     * @param filePath
     * @param dest
     * @throws IOException
     */
    public void download(String filePath, File dest) throws IOException {
        String url = makeUrl(filePath);
        OkHttpUtils.readBinary(url, dest, null);
    }

    // --

    /**
     * @param fileName
     * @return
     * @see #parseFileName(String)
     */
    public static String formatFileKey(String fileName) {
        return formatFileKey(fileName, true, null);
    }

    /**
     * @param fileName
     * @param keepName
     * @param updir
     * @return
     * @see #parseFileName(String)
     */
    public static String formatFileKey(String fileName, boolean keepName, String updir) {
        if (keepName) {
            while (fileName.contains("__")) {
                fileName = fileName.replace("__", "_");
            }
            // 去除特殊符号
            fileName = fileName.replace("  ", " ").replace(" ", "-");
            fileName = fileName.replaceAll("[?&#+%/\\s]", "");

            // 文件名长度控制
            if (fileName.length() > 51) {
                fileName = fileName.substring(0, 25) + ".." + fileName.substring(fileName.length() - 25);
            }

        } else {
            String fileExt = FileUtil.getSuffix(fileName);
            fileName = CommonsUtils.randomHex(true).substring(0, 20);
            if (StringUtils.isNotBlank(fileExt)) fileName += "." + fileExt;
        }

        String dt = CalendarUtils.getDateFormat("yyyyMMddHHmmssSSS").format(CalendarUtils.now());
        String subdir = dt.substring(0, 8);
        String filePrefix = dt.substring(8);
        // remove unsafe flags
        if (StringUtils.isNotBlank(updir)) subdir = updir.replaceAll("[%./\\\\\\s]", "");

        return String.format("rb/%s/%s__%s", subdir, filePrefix, fileName);
    }

    /**
     * 解析上传文件名称
     *
     * @param filePath
     * @return
     * @see #formatFileKey(String)
     */
    public static String parseFileName(String filePath) {
        String[] filePathSplit = filePath.split("/");
        String fileName = filePathSplit[filePathSplit.length - 1];
        if (fileName.contains("__")) {
            fileName = fileName.substring(fileName.indexOf("__") + 2);
        }
        if (fileName.contains("?")) {
            fileName = fileName.split("\\?")[0];
        }
        return fileName;
    }

    /**
     * URL 编码（中文或特殊字符）
     *
     * @param url
     * @return
     */
    public static String encodeUrl(String url) {
        if (StringUtils.isBlank(url)) return url;

        String[] urlSplit = url.split("/");
        for (int i = 0; i < urlSplit.length; i++) {
            String e = CodecUtils.urlEncode(urlSplit[i]);
            if (e.contains("+")) {
                e = e.replace("+", "%20");
            }
            urlSplit[i] = e;
        }
        return StringUtils.join(urlSplit, "/");
    }

    /**
     * 存储空间大小（1小时缓存）
     *
     * @return
     */
    public static long getStorageSize() {
        Long size = (Long) Application.getCommonsCache().getx("_StorageSize");
        if (size != null) return size;

        if (QiniuCloud.instance().available()) {
            size = QiniuCloud.instance().stats();
        } else {
            File data = RebuildConfiguration.getFileOfData("rb");
            if (data.exists()) {
                size = FileUtils.sizeOfDirectory(data);
            }
        }

        if (size == null) size = 0L;

        Application.getCommonsCache().putx("_StorageSize", size, CommonsCache.TS_HOUR);
        return size;
    }

    /**
     * 下载文件
     *
     * @param filepath
     * @return
     * @throws IOException
     * @throws RebuildException If cannot read/download
     */
    public static File downloadFile(String filepath) throws IOException, RebuildException {
        File file = null;
        if (CommonsUtils.isExternalUrl(filepath)) {
            String name = filepath.split("\\?")[0];
            name = name.substring(name.lastIndexOf("/") + 1);
            file = RebuildConfiguration.getFileOfTemp("dn" + System.nanoTime() + "." + name);
            OkHttpUtils.readBinary(filepath, file, null);

        } else if (QiniuCloud.instance().available()) {
            String name = parseFileName(filepath);
            file = RebuildConfiguration.getFileOfTemp("dn" + System.nanoTime() + "." + name);
            instance().download(filepath, file);

        } else if (filepath.startsWith("rb/") || filepath.startsWith("/rb/")) {
            file = RebuildConfiguration.getFileOfData(filepath);
        }

        if (file == null || !file.exists()) throw new RebuildException("Cannot read file : " + filepath);
        return file;
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String uploadFile(File file) throws IOException {
        return uploadFile(file, null);
    }

    /**
     * 上传文件
     *
     * @param file
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String uploadFile(File file, String fileName) throws IOException {
        if (fileName == null) fileName = file.getName();

        String fileKey;
        if (QiniuCloud.instance().available()) {
            fileKey = QiniuCloud.instance().upload(file, fileName);
        } else {
            fileKey = QiniuCloud.formatFileKey(fileName);
            File move2data = RebuildConfiguration.getFileOfData(fileKey);
            FileUtils.moveFile(file, move2data);
        }
        return fileKey;
    }

    /**
     * 删除文件
     *
     * @param filePaths
     * @return
     */
    public static int deleteFiles(String filePaths) {
        if (StringUtils.isBlank(filePaths)) return 0;

        if (!JSONUtils.wellFormat(filePaths)) {
            if (filePaths.startsWith("rb/")) {
                filePaths = "[\"" + filePaths + "\"]";
            } else {
                return 0;
            }
        }

        int del = 0;
        JSONArray fileKeys = JSON.parseArray(filePaths);
        for (Object fileKey : fileKeys) {
            if (QiniuCloud.instance().available()) {
                del += QiniuCloud.instance().delete(fileKey.toString()) ? 1 : 0;
            } else {
                File file = RebuildConfiguration.getFileOfData(fileKey.toString());
                if (file.exists() && !file.isDirectory()) {
                    del += FileUtils.deleteQuietly(file) ? 1 : 0;
                }
            }
        }
        return del;
    }
}
