/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.server.RebuildException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 加/解密
 * 
 * @author Zhao Fangfang
 * @since 2017-1-2
 */
public class AES {

	private static final Log LOG = LogFactory.getLog(AES.class);

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
		byte[] crypted;
		try {
			SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			crypted = cipher.doFinal(input.getBytes());
		} catch (Exception ex) {
			throw new RebuildException("Encrypting error : " + input, ex);
		}
		return new String(Base64.encodeBase64(crypted));
	}

	/**
	 * @param input
	 * @return
	 * @throws RebuildException
	 * @see #decryptQuietly(String)
	 */
	public static String decrypt(String input) throws RebuildException {
		return decrypt(input, getPassKey());
	}

	/**
	 * 解密失败则返回空（无异常抛出）
	 *
	 * @param input
	 * @return
	 */
	public static String decryptQuietly(String input) {
		try {
			return decrypt(input);
		} catch (RebuildException ex) {
            LOG.debug("Decrypting error (Use blank input) : " + input);
			return StringUtils.EMPTY;
		}
	}
	
	/**
	 * @param input
	 * @param key
	 * @return
	 * @throws RebuildException
	 */
	public static String decrypt(String input, String key) throws RebuildException {
		key = StringUtils.leftPad(key, 16, "0").substring(0, 16);
		byte[] output;
		try {
			SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			output = cipher.doFinal(Base64.decodeBase64(input));
		} catch (Exception ex) {
			throw new RebuildException("Decrypting error : " + input, ex);
		}
		return new String(output);
	}

	/**
	 * 通过 `-Drbpass=KEY` 指定 AES 秘钥
	 * 
	 * @return
	 */
	public static String getPassKey() {
		String key = StringUtils.defaultIfEmpty(System.getenv("rbpass"), System.getProperty("rbpass"));
		return StringUtils.defaultIfEmpty(key, "REBUILD2018");
	}

	// for Encrypt
	public static void main(String[] args) {
		System.out.println(encrypt("428115fbdc40413c43a1e977a83c8a5a"));
	}
}