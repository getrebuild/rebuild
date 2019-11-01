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

package com.rebuild.web.feeds;

import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO
 *
 * @author devezhao
 * @since 2019/11/1
 */
@Controller
@RequestMapping("/feeds/post/")
public class FeedsPostControll extends BaseControll {

    @RequestMapping("publish")
    public void publish(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("comment")
    public void comment(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("like")
    public void like(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
