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

import cn.devezhao.persist4j.engine.ID;

/**
 * 当前请求用户（线程量）
 * 
 * @author zhaofang123@gmail.com
 * @since 11/20/2017
 */
public class CurrentCaller {

	private static final ThreadLocal<ID> CALLER = new ThreadLocal<>();
	
	protected void set(ID user) {
		CALLER.set(user);
	}
	
	protected void clear() {
		CALLER.remove();
	}

	/**
	 * @return
	 * @throws InvalidRequestException
	 */
	public ID get() throws InvalidRequestException {
		return get(false);
	}
	
	/**
	 * @return
	 * @throws InvalidRequestException
	 */
	public ID get(boolean allowNull) throws InvalidRequestException {
		ID user = CALLER.get();
		if (user == null && allowNull == false) {
			throw new InvalidRequestException(403, "无效请求用户");
		}
		return user;
	}
}
