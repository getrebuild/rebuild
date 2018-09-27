/*
rebuild - Building your system freely.
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

package com.rebuild.server;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer;

import com.rebuild.utils.AES;

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
	
	protected void afterLoad(Properties props) {
		String kpass = AES.getPassKey();
		for (Object key : props.keySet()) {
			if (key.toString().contains(".aes")) {
				props.put(key, AES.decrypt(kpass));
			}
		}
		propsHold = props;
	}
	
	protected String getItem(String key) {
		return propsHold.getProperty(key);
	}
}