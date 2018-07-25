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

package cn.devezhao.rebuild.web.commons;

import cn.devezhao.commons.web.JhtmlForward;
import cn.devezhao.commons.web.RequestContext;

/**
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
public class JhtmlForwardExt extends JhtmlForward {
	private static final long serialVersionUID = -3419137991587451790L;

	@Override
	public void execute(RequestContext context) throws Exception {
		context.getRequest().setAttribute("rootpath", contextPath);
		super.execute(context);
	}
}
