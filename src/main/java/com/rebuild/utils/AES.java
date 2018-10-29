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
		String key = StringUtils.defaultIfEmpty(System.getProperty("rbpass"), "REBUILD2018");
		key = StringUtils.leftPad(key, 16, "0").substring(0, 16);
		return key;
	}
	
	// for Encrypt
	public static void main(String[] args) {
		System.out.println(encrypt(""));
	}
}