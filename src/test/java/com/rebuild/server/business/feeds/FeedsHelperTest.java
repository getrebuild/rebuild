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

package com.rebuild.server.business.feeds;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import org.junit.Test;

import java.util.Map;

/**
 * @author devezhao
 * @since 2019/11/7
 */
public class FeedsHelperTest extends TestSupport {

    @Test
    public void findMentions() {
        Map<String, ID> map = FeedsHelper.findMentionsMap("@RB示例用户 @超级管理员 @没有 @RB 示例用户 你还的呵呵我复合 @ @  ");
        System.out.println(map);
    }
}