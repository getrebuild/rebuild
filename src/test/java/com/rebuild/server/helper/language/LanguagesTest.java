/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.helper.language;

import com.rebuild.server.TestSupport;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/10/31
 */
public class LanguagesTest extends TestSupport {

    @Test
    public void getBundle() {
        System.out.println(Languages.instance.getDefaultBundle());       // zh_CN
        System.out.println(Languages.instance.getBundle(Locale.US));     // en_US
        System.out.println(Languages.instance.getBundle(Locale.JAPAN));  // ja_JP
        System.out.println(Languages.instance.getBundle(Locale.GERMAN)); // zh_CN
    }

    @Test
    public void getLang() {
        System.out.println(Languages.instance.getDefaultBundle().lang("Password"));
        System.out.println(Languages.instance.getLang("Password"));  // Matchs
    }
}