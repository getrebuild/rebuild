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

package com.rebuild.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

/**
 * @author Zhao Fangfang
 * @since 2017-1-2
 */
public class AES {
	
	/**
	 * @param input
	 * @return
	 */
	public static String encrypt(String input) {
		return encrypt(input, getPassKey());
	}

	/**
	 * @param input
	 * @param key
	 * @return
	 */
	public static String encrypt(String input, String key) {
		byte[] crypted = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			crypted = cipher.doFinal(input.getBytes());
		} catch (Exception ex) {
			throw new RuntimeException("加密失败", ex);
		}
		return new String(Base64.encodeBase64(crypted));
	}

	/**
	 * @param input
	 * @return
	 */
	public static String decrypt(String input) {
		return decrypt(input, getPassKey());
	}
	
	/**
	 * @param input
	 * @param key
	 * @return
	 */
	public static String decrypt(String input, String key) {
		byte[] output = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			output = cipher.doFinal(Base64.decodeBase64(input));
		} catch (Exception ex) {
			throw new RuntimeException("解密失败", ex);
		}
		return new String(output);
	}
	
	/**
	 * @return
	 */
	public static String getPassKey() {
		return StringUtils.defaultIfEmpty(System.getProperty("kpass"), "REBUILD2018");
	}
}