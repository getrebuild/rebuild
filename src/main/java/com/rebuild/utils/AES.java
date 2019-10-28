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

package com.rebuild.utils;

import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 加/解密
 * 
 * @author Zhao Fangfang
 * @since 2017-1-2
 */
public class AES {
	
	/**
	 * @param input
	 * @return
	 * @throws RebuildException
	 */
	public static String encrypt(String input) throws RebuildException {
		return encrypt(input, getPassKey());
	}

	/**
	 * @param input
	 * @param key
	 * @return
	 * @throws RebuildException
	 */
	public static String encrypt(String input, String key) throws RebuildException {
		key = StringUtils.leftPad(key, 16, "0").substring(0, 16);
		byte[] crypted = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			crypted = cipher.doFinal(input.getBytes());
		} catch (Exception ex) {
			throw new RebuildException("加密失败", ex);
		}
		return new String(Base64.encodeBase64(crypted));
	}

	/**
	 * @param input
	 * @return
	 * @throws RebuildException
	 */
	public static String decrypt(String input) throws RebuildException {
		return decrypt(input, getPassKey());
	}
	
	/**
	 * @param input
	 * @param key
	 * @return
	 * @throws RebuildException
	 */
	public static String decrypt(String input, String key) throws RebuildException {
		key = StringUtils.leftPad(key, 16, "0").substring(0, 16);
		byte[] output = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			output = cipher.doFinal(Base64.decodeBase64(input));
		} catch (Exception ex) {
			throw new RebuildException("Decrypting Error", ex);
		}
		return new String(output);
	}
	
	/**
	 * @param input
	 * @return
	 */
	public static String decryptNothrow(String input) {
		try {
			return decrypt(input);
		} catch (RebuildException ex) {
			Application.LOG.warn("Decrypting Error! Use input: " + input);
			return input;
		}
	}
	
	/**
	 * 通过 `-Drbpass=KEY` 指定 AES 秘钥
	 * 
	 * @return
	 */
	public static String getPassKey() {
		String key = StringUtils.defaultIfEmpty(System.getenv("rbpass"), System.getProperty("rbpass"));
		key = StringUtils.defaultIfEmpty(key, "REBUILD2018");
		return key;
	}
	
	// for Encrypt
	public static void main(String[] args) {
		System.out.println(encrypt("428115fbdc40413c43a1e977a83c8a5a"));
	}
}