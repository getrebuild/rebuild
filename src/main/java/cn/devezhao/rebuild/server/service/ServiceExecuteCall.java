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

package cn.devezhao.rebuild.server.service;

import cn.devezhao.persist4j.Record;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 03/20/2018
 */
public interface ServiceExecuteCall {

	/**
	 * @return
	 */
	int getEntity();
	
	/**
	 * @return
	 */
	boolean isAsync();
	
	/**
	 * @param record
	 * @return
	 */
	Object call(Record record);
}
