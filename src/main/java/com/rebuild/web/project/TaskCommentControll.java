/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 任务凭论
 *
 * @author devezhao
 * @since 2020/6/29
 */
@RequestMapping("/project/comments/")
@Controller
public class TaskCommentControll extends BaseControll {

    @RequestMapping("post")
    public void commentPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("delete")
    public void commentDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("list")
    public void commentList(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
