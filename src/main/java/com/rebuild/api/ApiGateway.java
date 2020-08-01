/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.RebuildApiManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.RateLimiters;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import org.apache.commons.lang.StringUtils;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAPI 网关
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
@Controller
public class ApiGateway extends Controll {

    private static final RequestRateLimiter RRL = RateLimiters.createRateLimiter(1, 200);

    @CrossOrigin
	@RequestMapping("/gw/api/**")
	public void api(HttpServletRequest request, HttpServletResponse response) {
		String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
		String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
		final String apiName = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

		final Date reuqestTime = CalendarUtils.now();
		final String remoteIp = ServletUtils.getRemoteAddr(request);

		response.setHeader("X-Powered", "RB/" + Application.VER);

		if (RRL.overLimitWhenIncremented("ip:" + remoteIp)) {
            JSON err = formatFailure("Request frequency exceeded", ApiInvokeException.ERR_FREQUENCY);
            LOG.error(err.toJSONString());
            ServletUtils.writeJson(response, err.toJSONString());
            return;
        }

		int errorCode;
		String errorMsg;

		ApiContext context = null;
		try {
			final BaseApi api = createApi(apiName);
			context = verfiy(request, api);
			if (context.getBindUser() != null) {
				Application.getSessionStore().set(context.getBindUser());
			}

			JSON data = api.execute(context);
			JSON success = formatSuccess(data);
			ServletUtils.writeJson(response, success.toJSONString());
			logRequestAsync(reuqestTime, remoteIp, apiName, context, success);

			return;

		} catch (ApiInvokeException ex) {
			errorCode = ex.getErrorCode();
			errorMsg = ex.getErrorMsg();
		} catch (DataSpecificationException ex) {
			errorCode = ApiInvokeException.ERR_DATASPEC;
			errorMsg = ex.getLocalizedMessage();
		} catch (Throwable ex) {
			errorCode = ApiInvokeException.ERR_SERVER;
			errorMsg = ex.getLocalizedMessage();
		} finally {
			Application.getSessionStore().clean();
		}

		JSON err = formatFailure(StringUtils.defaultIfBlank(errorMsg, "Server Internal Error"), errorCode);
		LOG.error(err.toJSONString());
		ServletUtils.writeJson(response, err.toJSONString());
		try {
			logRequestAsync(reuqestTime, remoteIp, apiName, context, err);
		} catch (Exception ignored) {
		}
	}

	/**
	 * 验证请求并构建请求上下文
	 *
	 * @param request
	 * @param useApi
	 * @return
	 */
	protected ApiContext verfiy(HttpServletRequest request, BaseApi useApi) {
		final Map<String, String> sortedMap = new TreeMap<>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			String[] vv = e.getValue();
			sortedMap.put(e.getKey(), vv == null || vv.length == 0 ? null : vv[0]);
		}

		final String appid = getParameterNotNull(sortedMap,"appid");
		final String sign = getParameterNotNull(sortedMap,"sign");

		final ConfigEntry apiConfig = RebuildApiManager.instance.getApp(appid);
		if (apiConfig == null) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [appid] " + appid);
		}

		// 验证签名

		final String timestamp = sortedMap.get("timestamp");
		final String signType = sortedMap.get("sign_type");
		sortedMap.remove("sign");

		// 明文签名
		if (timestamp == null && signType == null) {
			if (!apiConfig.getString("appSecret").equals(sign)) {
				throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [sign] " + sign);
			}
		}
		// 密文签名
		else {
			long systemTime = System.currentTimeMillis() / 1000;
			if (Math.abs(systemTime - ObjectUtils.toLong(timestamp)) > (Application.devMode() ? 100 : 15)) {
				throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [timestamp] " + appid);
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
				throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [sign_type] " + signType);
			}

			if (!sign.equals(sign2sign)) {
				throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid [sign] " + sign);
			}
		}

		// 组合请求数据

		String postData = ServletUtils.getRequestString(request);
		JSON postJson = postData != null ? (JSON) JSON.parse(postData) : null;
		ID bindUser = apiConfig.getID("bindUser");

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
			throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Parameter [" + name + "] cannot be empty");
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
	 * @param apiName
	 * @param context
	 * @param result
	 */
	protected void logRequestAsync(Date requestTime, String remoteIp, String apiName, ApiContext context, JSON result) {
		if (context == null || result == null || !isLogRequest()) {
			return;
		}

		ThreadPool.exec(() -> {
			Record record = EntityHelper.forNew(EntityHelper.RebuildApiRequest, UserService.SYSTEM_USER);
			record.setString("appId", context.getAppId());
			record.setString("remoteIp", remoteIp);
			record.setString("requestUrl", CommonsUtils.maxstr(apiName + "?" + context.getParameterMap(),300));
			if (context.getPostData() != null) {
				record.setString("requestBody", CommonsUtils.maxstr(context.getPostData().toJSONString(), 10000));
			}
			record.setString("responseBody", CommonsUtils.maxstr(result.toJSONString(), 10000));
			record.setDate("requestTime", requestTime);
			record.setDate("responseTime", CalendarUtils.now());
			Application.getCommonService().create(record, false);
		});
	}

	/**
	 * 是否记录日志
	 *
	 * @return
	 */
	protected boolean isLogRequest() {
		return true;
	}

	// -- 注册 API

	private static final Map<String, Class<? extends BaseApi>> API_CLASSES = new HashMap<>();

	/**
	 * @param clazz
	 */
	public static void registerApi(Class<? extends BaseApi> clazz) {
		BaseApi api = (BaseApi) ReflectUtils.newInstance(clazz);
		String apiName = api.getApiName();
		if (API_CLASSES.containsKey(apiName)) {
			LOG.error("Replaced API : " + apiName);
		}

		API_CLASSES.put(apiName, clazz);
		LOG.info("New API registered : " + apiName + " : " + clazz.getName());
	}
}
