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

package com.rebuild.server.business.charts;

/**
 * 显示样式
 * 
 * @author devezhao
 * @since 12/15/2018
 */
public class FormatStyle {
	
	private String fontsize = "";
	private String fontcolor = "";
	private String formatted = "";
	
	public FormatStyle(String fontsize, String fontcolor, String formatted) {
		this.fontsize = fontsize;
		this.fontcolor = fontcolor;
		this.formatted = formatted;
	}
	
	public String getFontsize() {
		return fontsize;
	}
	
	public String getFontcolor() {
		return fontcolor;
	}
	
	public String getFormatted() {
		return formatted;
	}
}
