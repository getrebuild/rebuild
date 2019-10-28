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

package com.rebuild.utils;

import cn.devezhao.commons.ThreadPool;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/10/25
 */
public class RequestFrequencyCounterTest {

    @Test
    public void times() {
        RequestFrequencyCounter counter = new RequestFrequencyCounter();
        for (int i = 0; i < 20; i++) {
            System.out.println(counter.add().seconds(3).times());
            ThreadPool.waitFor(10);
        }
        ThreadPool.waitFor(3000);
        System.out.println("Last : " + counter.times());
    }
}