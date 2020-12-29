/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

/**
 * @author devezhao
 * @since 2020/9/7
 */
public class WebConstants {

    // Global

    /**
     * 运行环境
     */
    public static final String ENV = "env";

    /**
     * 商业版
     */
    public static final String COMMERCIAL = "commercial";

    /**
     * 基础 URL
     */
    public static final String BASE_URL = "baseUrl";

    /**
     * 系统名称
     */
    public static final String APP_NAME = "appName";

    /**
     * 云存储 URL（为空则说明未配置云存储，会使用本地存储）
     */
    public static final String STORAGE_URL = "storageUrl";

    /**
     * 是否可分享文件
     */
    public static final String FILE_SHARABLE = "fileSharable";

    /**
     * 是否开启页面水印
     */
    public static final String MARK_WATERMARK = "markWatermark";

    // Per-Request

    /**
     * 语言包 Key
     */
    public static final String LOCALE = "locale";

    /**
     * CSRF-Token
     */
    public static final String CSRF_TOKEN = "csrfToken";

    /**
     * Auth-Token
     */
    public static final String AUTH_TOKEN = "authToken";

    /**
     * 主题
     */
    public static final String USE_THEME = "useTheme";

    // Object

    /**
     * 语言包
     *
     * @see com.rebuild.core.support.i18n.LanguageBundle
     */
    public static final String $BUNDLE = "bundle";

    /**
     * 用户
     *
     * @see com.rebuild.core.privileges.bizz.User
     */
    public static final String $USER = "user";
}
