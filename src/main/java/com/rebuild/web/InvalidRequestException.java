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

package com.rebuild.web;

import com.rebuild.server.RebuildException;

/**
 * 无效请求。如参数错误等
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class InvalidRequestException extends RebuildException {
	private static final long serialVersionUID = 1104144276994648297L;

	private int errCode = 400;
	
	public InvalidRequestException() {
		super();
	}

	public InvalidRequestException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public InvalidRequestException(String msg) {
		super(msg);
	}

	public InvalidRequestException(Throwable cause) {
		super(cause);
	}
	
	public InvalidRequestException(int errCode, String msg) {
		super(msg);
	}
	
	@Override
	protected int getErrCode() {
		return errCode;
	}
}
