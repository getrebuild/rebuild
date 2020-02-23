/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.server.Application;
import com.rebuild.server.helper.language.LanguageBundle;
import com.rebuild.server.helper.language.Languages;
import org.springframework.web.servlet.ModelAndView;

/**
 * 页面 Controll
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
public abstract class BasePageControll extends BaseControll {

	/**
	 * @param page
	 * @return
	 */
	protected ModelAndView createModelAndView(String page) {
		ModelAndView mv = new ModelAndView(page);

		// 语言包
		String locale = Application.getSessionStore().getLocale();
		LanguageBundle bundle = Languages.instance.getBundle(locale);
		mv.getModel().put("bundle", bundle);

		return mv;
	}
}
