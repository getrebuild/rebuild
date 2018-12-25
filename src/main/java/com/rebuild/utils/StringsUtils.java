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

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author devezhao
 * @since 12/25/2018
 */
public class StringsUtils {

	/**
	 * 加星号/打码
	 * 
	 * @param text
	 * @return
	 */
	public static String stars(String text) {
		if (StringUtils.isBlank(text)) {
			return text;
		}
		
		int textLen = text.length();
		if (textLen <= 3) {
			return text.substring(0, 1) + "**";
		} else if (textLen <= 20) {
			return text.substring(0, 1) + "**" + text.substring(textLen - 1);
		} else if (textLen <= 30) {
			return text.substring(0, 2) + "****" + text.substring(textLen - 2);
		}  else if (textLen <= 40) {
			return text.substring(0, 4) + "**********" + text.substring(textLen - 4);
		} else {
			return text.substring(0, 4) + "********************" + text.substring(textLen - 4);
		}
	}
	
}
