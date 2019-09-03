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

package com.rebuild.server.helper;

import com.rebuild.server.Application;
import com.rebuild.utils.AES;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Zhao Fangfang
 * @since 1.0, 2013-7-7
 */
public class AesPreferencesConfigurer extends PreferencesPlaceholderConfigurer {

	@Override
	protected void loadProperties(Properties props) throws IOException {
		super.loadProperties(props);
		this.afterLoad(props);
	}
	
	private Properties propsHold = null;
	
	private void afterLoad(Properties props) {
		final Object[] keys = props.keySet().toArray(new Object[0]);
		for (Object key : keys) {
			String cleanKey = key.toString();
			// AES decrypt if have `.aes` suffix
			if (cleanKey.endsWith(".aes")) {
				String val = props.getProperty(cleanKey);
				val = AES.decryptNothrow(val);

				props.remove(cleanKey);
				cleanKey = cleanKey.replace(".aes", "");
				props.put(cleanKey, val);
			}
			
			// Overrides by command-line
			String valInCL = System.getProperty(cleanKey);
			if (StringUtils.isNotBlank(valInCL)) {
				props.put(cleanKey, valInCL);
			}
		}
		
		String mysqlPort = System.getProperty("mysql.port");
		if (StringUtils.isNotBlank(mysqlPort)) {
			String dbUrl = props.getProperty("db.url");
			dbUrl = dbUrl.replace("3306", "4653");
			props.put("db.url", dbUrl);
		}
		
		propsHold = (Properties) props.clone();
		
		if (Application.devMode()) {
			Application.LOG.info("System properties : " + propsHold);
		}
	}
	
	/**
	 * 获取配置项
	 * 
	 * @param name
	 * @return
	 */
	public String getItem(String name) {
		return StringUtils.defaultIfBlank(propsHold.getProperty(name), null);
	}
}