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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.RebuildApiManager;
import com.rebuild.server.service.DataSpecificationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Enumeration;
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

	@RequestMapping("/gw/api/{apiName.*}")
	public void api(@PathVariable String apiName,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		int errorCode = 0;
		String errorMsg = null;

		try {
			JSON data = doApi(apiName, request);
			JSON ok = formatSuccess(data);
			ServletUtils.writeJson(response, ok.toJSONString());
		} catch (ApiInvokeException ex) {
			errorCode = ex.getErrorCode();
			errorMsg = ex.getErrorMsg();
		} catch (DataSpecificationException ex) {
			errorCode = ApiInvokeException.ERR_UNDATA;
			errorMsg = ex.getLocalizedMessage();
		} catch (Throwable ex) {
			errorCode = ApiInvokeException.ERR_SERVER;
			errorMsg = ex.getLocalizedMessage();
		}

		if (errorCode > 0) {
			JSON error = formatFailure(errorCode, errorMsg == null ? "未知错误" : errorMsg);
			ServletUtils.writeJson(response, error.toJSONString());
		}
	}

	/**
	 * API 执行
	 *
	 * @param apiName
	 * @param request
	 * @return
	 * @throws ApiInvokeException
	 * @throws IOException
	 */
	protected JSON doApi(String apiName, HttpServletRequest request) throws ApiInvokeException, IOException {
		BaseApi api = newApi(apiName);
		if (api == null) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAPI, "无效 API : " + apiName);
		}

		ApiContext context = verfiy(request);
		return api.execute(context);
	}

	/**
	 * 验证请求并构建请求上下文
	 *
	 * @param request
	 * @return
	 * @throws IOException
	 */
	protected ApiContext verfiy(HttpServletRequest request) throws IOException {
		Enumeration names = request.getParameterNames();
		Map<String, String> sortMap = new TreeMap<>();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			sortMap.put(name, request.getParameter(name));
		}

		String appid = getParameterNotNull(sortMap,"appid");
		ConfigEntry apiConfig = RebuildApiManager.instance.getApp(appid);
		if (apiConfig == null) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "无效 appid=" + appid);
		}
		String timestamp = getParameterNotNull(sortMap,"timestamp");
		long systemTime = System.currentTimeMillis() / 1000;
		if (Math.abs(systemTime - ObjectUtils.toLong(timestamp)) > 6) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "无效 timestamp=" + appid);
		}

		// 验证签名

		final String sign = getParameterNotNull(sortMap,"sign");
		sortMap.remove("sign");

		String signType = getParameterNotNull(sortMap,"sign_type");
		StringBuffer sign2 = new StringBuffer();
		for (Map.Entry<String, String> e : sortMap.entrySet()) {
			sign2.append(e.getKey())
					.append('=')
					.append(e.getValue())
					.append('&');
		}
		sign2.append(appid)
				.append('.')
				.append(apiConfig.getString("appSecret"));

		String sign2sign = null;
		if ("MD5".equals(signType)) {
			sign2sign = EncryptUtils.toMD5Hex(sign2.toString());
		} else if ("SHA1".equals(signType)) {
			sign2sign = EncryptUtils.toSHA1Hex(sign2.toString());
		} else {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "无效 signType=" + signType);
		}

		if (!sign.equals(sign2sign)) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADAUTH, "无效 sign=" + sign);
		}

		JSON posted = null;
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			posted = ServletUtils.getRequestJson(request);
		}

		ApiContext context = new ApiContext(appid, null, sortMap, posted);
		return context;
	}

	/**
	 * @param params
	 * @param name
	 * @return
	 */
	private String getParameterNotNull(Map<String, String> params, String name) {
		String v = params.get(name);
		if (StringUtils.isBlank(v)) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "参数 [" + name + "] 不能为空");
		}
		return v;
	}

	/**
	 * @param apiName
	 * @return
	 */
	protected BaseApi newApi(String apiName) {
		return null;
	}
}
