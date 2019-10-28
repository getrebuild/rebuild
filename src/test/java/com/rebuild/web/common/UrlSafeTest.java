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

package com.rebuild.web.common;

import com.rebuild.server.TestSupport;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/10/17
 */
public class UrlSafeTest extends TestSupport {

    @Test
    public void isTrusted() {
        assertTrue(UrlSafe.isTrusted("https://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&rsv_idx=1&tn=baidu&wd=spring%20mvc%20url%20%E5%8C%B9%E9%85%8D&oq=java%2520%25E5%258C%2585%25E5%2590%25AB%25E5%258C%25B9%25E9%2585%258D&rsv_pq=85cdb19f0006d519&rsv_t=a87c4Ts8FrKKX60Kh3cP9vK7%2FHuR1y%2FfUy9ZmZl2jln7nelF8pgXjLThuFg&rqlang=cn&rsv_enter=1&rsv_dl=tb&inputT=9403&rsv_sug3=49&rsv_sug1=28&rsv_sug7=100&rsv_sug2=0&rsv_sug4=10508"));
        assertTrue(UrlSafe.isTrusted("http://image.baidu.com/search/index?tn=baiduimage&ps=1&ct=201326592&lm=-1&cl=2&nc=1&ie=utf-8&word=spring+mvc+url+%E5%8C%B9%E9%85%8D"));
        assertTrue(UrlSafe.isTrusted("https://www.qq.com/"));
        assertFalse(UrlSafe.isTrusted("https://new.qq.com/ch/milite/"));
    }
}