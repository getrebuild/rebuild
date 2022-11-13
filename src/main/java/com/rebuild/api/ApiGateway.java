/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.*;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.RebuildApiManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.RateLimiters;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * OpenAPI 网关
 *
 * @author Zixin (RB)
 * @since 05/19/2018
 */
@Slf4j
@org.springframework.stereotype.Controller
public class ApiGateway extends Controller implements Initialization {

    // 基于 IP 限流
    private static final RequestRateLimiter RRL = RateLimiters.createRateLimiter(
            new int[] { 10, 60 },
            new int[] { 600, 6000 });

    private static final Map<String, Class<? extends BaseApi>> API_CLASSES = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws Exception {
        Set<Class<?>> apiClasses = cn.devezhao.commons.ReflectUtils.getAllSubclasses(
                BaseApi.class.getPackage().getName(), BaseApi.class);
        apiClasses.addAll(cn.devezhao.commons.ReflectUtils.getAllSubclasses(
                "com.rebuild.rbv.openapi", BaseApi.class));

        for (Class<?> c : apiClasses) {
            BaseApi api = (BaseApi) ReflectUtils.newInstance(c);
            String apiName = api.getApiName();
            if (API_CLASSES.containsKey(apiName)) {
                throw new RebuildException("Api `" + apiName + "` already exists");
            }
            API_CLASSES.put(apiName, (Class<? extends BaseApi>) c);
        }

        log.info("Added {} APIs", API_CLASSES.size());
    }

    @CrossOrigin
    @RequestMapping("/gw/api/**")
    public void api(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        final String apiName = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        final Date reuqestTime = CalendarUtils.now();
        final String remoteIp = ServletUtils.getRemoteAddr(request);
        final String requestId = CommonsUtils.randomHex();

        response.addHeader("X-RB-Server", ServerStatus.STARTUP_ONCE + "/" + Application.BUILD);
        response.setHeader("X-Request-Id", requestId);

        if (RRL.overLimitWhenIncremented("ip:" + remoteIp)) {
            JSON error = formatFailure("Request frequency exceeded", ApiInvokeException.ERR_FREQUENCY);
            log.error("{} : {}", requestId, error.toJSONString());
            ServletUtils.writeJson(response, error.toJSONString());
            return;
        }

        int errorCode;
        String errorMsg;

        ApiContext context = null;
        try {
            final BaseApi api = createApi(apiName);
            context = verfiy(request, api);

            UserContextHolder.setReqip(remoteIp);
            UserContextHolder.setUser(context.getBindUser());

            JSON result = api.execute(context);
            logRequestAsync(reuqestTime, remoteIp, requestId, apiName, context, result);

            ServletUtils.writeJson(response, result.toJSONString());
            return;

        } catch (ApiInvokeException ex) {
            errorCode = ex.getErrorCode();
            errorMsg = ex.getErrorMsg();
        } catch (DataSpecificationException ex) {
            errorCode = ApiInvokeException.ERR_DATASPEC;
            errorMsg = ex.getLocalizedMessage();
        } catch (Throwable ex) {
            errorCode = Controller.CODE_SERV_ERROR;
            errorMsg = ex.getLocalizedMessage();
            log.error("Server Internal Error ({})", requestId, ex);
        } finally {
            UserContextHolder.clear();
        }

        JSON error = formatFailure(StringUtils.defaultIfBlank(errorMsg, "Server Internal Error"), errorCode);
        try {
            logRequestAsync(reuqestTime, remoteIp, requestId, apiName, context, error);
        } catch (Exception ignored) {
        }

        log.error("{} : {}", requestId, error.toJSONString());
        ServletUtils.writeJson(response, error.toJSONString());
    }

    /**
     * 验证请求并构建请求上下文
     *
     * @param request
     * @param useApi
     * @return
     */
    protected ApiContext verfiy(HttpServletRequest request, @SuppressWarnings("unused") BaseApi useApi) {
        final Map<String, String> sortedMap = new TreeMap<>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            String[] vv = e.getValue();
            sortedMap.put(e.getKey(), vv == null || vv.length == 0 ? null : vv[0]);
        }

        final String appid = getParameterNotNull(sortedMap, "appid");
        final String sign = getParameterNotNull(sortedMap, "sign");

        final ConfigBean apiConfig = RebuildApiManager.instance.getApp(appid);
        if (apiConfig == null) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [appid] : " + appid);
        }

        // v3.1.1
        final String bindIps = apiConfig.getString("bindIps");
        if (StringUtils.isNotBlank(bindIps)) {
            String clientIp = ServletUtils.getRemoteAddr(request);
            if (!bindIps.contains(clientIp)) {
                throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Client ip not in whitelist : " + clientIp);
            }
        }

        // 验证签名

        final String timestamp = sortedMap.get("timestamp");
        final String signType = sortedMap.get("sign_type");
        sortedMap.remove("sign");

        // 明文签名
        if (timestamp == null && signType == null) {
            if (RebuildConfiguration.getBool(ConfigurationItem.SecurityEnhanced)) {
                throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [timestamp] or [sign_type]");
            }
            if (!apiConfig.getString("appSecret").equals(sign)) {
                throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [sign] : " + sign);
            }
        }
        // 密文签名
        else {
            long systemTime = System.currentTimeMillis() / 1000;
            if (Math.abs(systemTime - ObjectUtils.toLong(timestamp)) > (Application.devMode() ? 100 : 15)) {
                throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [timestamp] : " + timestamp);
            }

            StringBuilder sign2 = new StringBuilder();
            for (Map.Entry<String, String> e : sortedMap.entrySet()) {
                sign2.append(e.getKey())
                        .append('=')
                        .append(e.getValue())
                        .append('&');
            }
            sign2.append(appid)
                    .append('.')
                    .append(apiConfig.getString("appSecret"));

            String sign2sign;
            if ("MD5".equals(signType)) {
                sign2sign = EncryptUtils.toMD5Hex(sign2.toString());
            } else if ("SHA1".equals(signType)) {
                sign2sign = EncryptUtils.toSHA1Hex(sign2.toString());
            } else {
                throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [sign_type] : " + signType);
            }

            if (!sign.equals(sign2sign)) {
                throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [sign] : " + sign);
            }
        }

        // 组合请求数据

        String postData = ServletUtils.getRequestString(request);
        JSON postJson = postData != null ? (JSON) JSON.parse(postData) : null;
        ID bindUser = apiConfig.getID("bindUser");
        // 默认绑定系统用户
        if (bindUser == null) bindUser = UserService.SYSTEM_USER;

        return new ApiContext(sortedMap, postJson, appid, bindUser);
    }

    /**
     * @param params
     * @param name
     * @return
     */
    private String getParameterNotNull(Map<String, String> params, String name) {
        String v = params.get(name);
        if (StringUtils.isBlank(v)) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Parameter [" + name + "] cannot be null");
        }
        return v;
    }

    /**
     * @param apiName
     * @return
     */
    private BaseApi createApi(String apiName) {
        if (!API_CLASSES.containsKey(apiName)) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADAPI, "Unknown API : " + apiName);
        }
        return (BaseApi) ReflectUtils.newInstance(API_CLASSES.get(apiName));
    }

    /**
     * 记录请求日志
     *
     * @param requestTime
     * @param remoteIp
     * @param requestId
     * @param apiName
     * @param context
     * @param result
     */
    protected void logRequestAsync(Date requestTime, String remoteIp, String requestId, String apiName, ApiContext context, JSON result) {
        Record record = EntityHelper.forNew(EntityHelper.RebuildApiRequest, UserService.SYSTEM_USER);
        record.setString("requestUrl", apiName);
        record.setString("remoteIp", remoteIp);
        record.setString("responseBody", requestId + ":" + (result == null ? "{}" : CommonsUtils.maxstr(result.toJSONString(), 10000)));
        record.setDate("requestTime", requestTime);
        record.setDate("responseTime", CalendarUtils.now());

        if (context != null) {
            record.setString("appId", context.getAppId());
            if (context.getPostData() != null) {
                record.setString("requestBody",
                        CommonsUtils.maxstr(context.getPostData().toJSONString(), 10000));
            }
            if (!context.getParameterMap().isEmpty()) {
                record.setString("requestUrl",
                        CommonsUtils.maxstr(apiName + "?" + context.getParameterMap(), 300));
            }
        } else {
            record.setString("appId", "0");
        }

        TaskExecutors.queue(() -> Application.getCommonsService().create(record, false));
    }
}
