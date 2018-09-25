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

package com.rebuild.server;

import org.apache.commons.lang.exception.NestableRuntimeException;

import com.rebuild.utils.AppUtils;

/**
 * RB Root Exception
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class RebuildException extends NestableRuntimeException {
	private static final long serialVersionUID = -889444005870894361L;

	public RebuildException() {
		super();
	}

	public RebuildException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public RebuildException(String msg) {
		super(msg);
	}

	public RebuildException(Throwable cause) {
		super(cause);
	}
	
	public String toClientMsgString() {
		return AppUtils.formatClientMsg(getErrCode(), this.getMessage());
	}
	
	protected int getErrCode() {
		return 500;
	}
}
