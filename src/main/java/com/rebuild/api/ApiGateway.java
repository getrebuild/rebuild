/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.api;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.ObjectUtils;
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
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAPI 入口
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
@Controller
public class ApiGateway extends Controll {

	private static final Log LOG = LogFactory.getLog(ApiGateway.class);

	@RequestMapping("/gw/api/{apiName}")
	public void api(@PathVariable String apiName,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		final Date reuqestTime = CalendarUtils.now();
		final String remoteIp = ServletUtils.getRemoteAddr(request);

		int errorCode;
		String errorMsg;

		ApiContext context = null;
		try {
			BaseApi api = createApi(apiName);
			context = verfiy(request.getParameterMap(), ServletUtils.getRequestString(request));
			if (context.getBindUser() != null) {
				Application.getSessionStore().set(context.getBindUser());
			}

			JSON data = api.execute(context);
			JSON ok = formatSuccess(data);
			ServletUtils.writeJson(response, ok.toJSONString());
			logRequestAsync(reuqestTime, remoteIp, apiName, context, ok);
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

		JSON err = formatFailure(errorMsg == null ? "Server Internal Error" : errorMsg, errorCode);
		ServletUtils.writeJson(response, err.toJSONString());
		logRequestAsync(reuqestTime, remoteIp, apiName, context, err);
	}

	/**
	 * 验证请求并构建请求上下文
	 *
	 * @param parameterMap
	 * @param post
	 * @return
	 * @throws IOException
	 */
	protected ApiContext verfiy(Map<String, String[]> parameterMap, String post) throws IOException {
		Map<String, String> sortedMap = new TreeMap<>();
		for (Map.Entry<String, String[]> e : parameterMap.entrySet()) {
			String[] vv = e.getValue();
			sortedMap.put(e.getKey(), vv == null || vv.length == 0 ? null : vv[0]);
		}

		String appid = getParameterNotNull(sortedMap,"appid");
		ConfigEntry apiConfig = RebuildApiManager.instance.getApp(appid);
		if (apiConfig == null) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid appid=" + appid);
		}

		String timestamp = getParameterNotNull(sortedMap,"timestamp");
		long systemTime = System.currentTimeMillis() / 1000;
		if (Math.abs(systemTime - ObjectUtils.toLong(timestamp)) > (AppUtils.devMode() ? 100 : 10)) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid timestamp=" + appid);
		}

		// 验证签名

		final String sign = getParameterNotNull(sortedMap,"sign");
		sortedMap.remove("sign");

		String signType = getParameterNotNull(sortedMap,"sign_type");
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
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid sign_type=" + signType);
		}

		if (!sign.equals(sign2sign)) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "Invalid sign=" + sign);
		}

		JSON postJson = post != null ? (JSON) JSON.parse(post) : null;
		ID bindUser = apiConfig.getID("bindUser");
        return new ApiContext(sortedMap, postJson, appid, bindUser);
	}

	/**
	 * @param params
	 * @param name
	 * @return
	 */
	protected String getParameterNotNull(Map<String, String> params, String name) {
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
	protected BaseApi createApi(String apiName) {
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
		if (context == null || result == null) {
			return;
		}

		Record record = EntityHelper.forNew(EntityHelper.RebuildApiRequest, UserService.SYSTEM_USER);
		record.setString("appId", context.getAppId());
		record.setString("remoteIp", remoteIp);
		record.setString("requestUrl", apiName + " " + context.getParameterMap());
		if (context.getPostData() != null) {
			record.setString("requestBody", context.getPostData().toJSONString());
		}
		record.setString("responseBody", result.toJSONString());
		record.setDate("requestTime", requestTime);
		record.setDate("responseTime", CalendarUtils.now());
		Application.getCommonService().create(record);
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
			LOG.warn("Replaced API : " + apiName);
		}

		API_CLASSES.put(apiName, clazz);
		LOG.info("New API registered : " + apiName);
	}

	static {
		registerApi(SystemTime.class);
		registerApi(LoginToken.class);
	}
}
