package com.rebuild.server;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer;

import com.rebuild.utils.AES;

/**
 * @author Zhao Fangfang
 * @version $Id: DynamicSecurityPreferencesConfigurer.java 1 2014-11-26 17:20:23Z zhaoff@DS.Works $
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