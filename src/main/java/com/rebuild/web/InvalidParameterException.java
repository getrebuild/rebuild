/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.server.service.DataSpecificationException;

/**
 * 无效请求参数
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class InvalidParameterException extends DataSpecificationException {
	private static final long serialVersionUID = 1104144276994648297L;
	
	public InvalidParameterException() {
		this("无效请求参数");
	}
	
	public InvalidParameterException(String msg) {
		super(msg);
	}
	
	public InvalidParameterException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public InvalidParameterException(int errorCode, String msg) {
		super(errorCode, msg);
	}
}
