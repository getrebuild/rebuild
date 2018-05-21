/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.utils;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class AppUtils {

	/**
	 * @param request
	 * @return
	 */
	public static ID getRequestUser(HttpServletRequest request) {
		return null;
	}
	
	/**
	 * @param errCode
	 * @param errMsg
	 * @return
	 */
	public static String formatClientMsg(int errCode, String errMsg) {
		JSONObject jo = new JSONObject();
		jo.put("error_code", errCode);
		if (errMsg != null) {
			jo.put("error_msg", errMsg);
		}
		return jo.toJSONString();
	}
	
	/**
	 * @param fileName
	 * @return
	 */
	public static File getFileOfTemp(String fileName) {
		return new File(FileUtils.getTempDirectory(), fileName);
	}
}
