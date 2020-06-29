/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 任务
 *
 * @author devezhao
 * @since 2020/6/29
 */
@Controller
public class ProjectTaskControll extends BasePageControll {

    @RequestMapping("/project/task/{taskId}")
    public ModelAndView pageTask(@PathVariable String taskId,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {
        return null;
    }

    @RequestMapping("/project/tasks/post")
    public void taskPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("/project/tasks/delete")
    public void taskDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("/project/tasks/list")
    public void taskList(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("/project/tasks/get")
    public void taskGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
