/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.setup;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.user.signup.LoginController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author devezhao
 * @since 2019/11/25
 */
@RestController
@RequestMapping("/setup/")
public class InstallController extends BaseController implements InstallState {

    @GetMapping("install")
    public ModelAndView index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (Application.isReady() && !Application.devMode()) {
            response.sendError(404);
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/setup/install");
        mv.getModel().put("Version", Application.VER);

        // 切换语言
        LoginController.putLocales(mv, AppUtils.getReuqestLocale(request));

        return mv;
    }

    @PostMapping("test-connection")
    public RespBody testConnection(HttpServletRequest request) {
        JSONObject dbProps = (JSONObject) ServletUtils.getRequestJson(request);
        JSONObject props = JSONUtils.toJSONObject("databaseProps", dbProps);

        Installer checker = new Installer(props);

        try (Connection conn = checker.getConnection(null)) {
            DatabaseMetaData dmd = conn.getMetaData();
            String okMsg = formatLang(request, "ConnectionSucceed",
                    dmd.getDatabaseProductName() + " " + dmd.getDatabaseProductVersion());

            // 查询表
            try (ResultSet rs = dmd.getTables(conn.getCatalog(), conn.getSchema(), null, new String[]{"TABLE"})) {
                if (rs.next()) {
                    String hasTable = rs.getString("TABLE_NAME");
                    if (hasTable != null) {
                        if (checker.isRbDatabase()) {
                            okMsg += String.format(" (%s)",
                                    formatLang(request, "NoneEmptyDbTips", dbProps.getString("dbName")));
                        } else {
                            return RespBody.errorl("NoneEmptyDbError");
                        }
                    }
                }
            } catch (SQLException ignored) {
            }

            return RespBody.ok(okMsg);

        } catch (SQLException ex) {
            if (ex.getLocalizedMessage().contains("Unknown database")) {
                String okMsg = formatLang(request, "ConnectionSucceedEmptyDbTips",
                        dbProps.getString("dbName"));
                return RespBody.ok(okMsg);
            } else {
                return RespBody.error(formatLang(request, "ConnectionError", ex.getLocalizedMessage()));
            }
        }
    }

    @PostMapping("test-cache")
    public RespBody testCache(HttpServletRequest request) {
        JSONObject cacheProps = (JSONObject) ServletUtils.getRequestJson(request);

        JedisPool pool = new JedisPool(new JedisPoolConfig(),
                StringUtils.defaultIfBlank(cacheProps.getString("CacheHost"), "127.0.0.1"),
                ObjectUtils.toInt(cacheProps.getString("CachePort"), 6379),
                3000,
                StringUtils.defaultIfBlank(cacheProps.getString("CachePassword"), null));

        try (Jedis client = pool.getResource()) {
            String info = client.info("server");
            if (info.length() > 80) {
                info = info.substring(0, 80) + "...";
            }
            pool.destroy();

            return RespBody.ok(formatLang(request, "ConnectionSucceed", info));

        } catch (Exception ex) {
            return RespBody.error(
                    formatLang(request, "ConnectionError", ThrowableUtils.getRootCause(ex).getLocalizedMessage()));
        }
    }

    @PostMapping("install-rebuild")
    public RespBody installExec(HttpServletRequest request) {
        JSONObject installProps = (JSONObject) ServletUtils.getRequestJson(request);

        try {
            new Installer(installProps).install();
            return RespBody.ok();
        } catch (Exception ex) {
            LOG.error("An error occurred during install", ex);
            return RespBody.error(
                    getLang(request, "InstallFailed") + " : " + ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
    }
}
