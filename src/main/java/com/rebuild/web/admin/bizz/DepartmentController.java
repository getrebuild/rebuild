/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.privileges.DepartmentService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class DepartmentController extends EntityController {

    @GetMapping("departments")
    public ModelAndView pageList(HttpServletRequest request) {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/dept-list", "Department", user);

        JSON config = DataListManager.instance.getFieldsLayout("Department", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @PostMapping("dept-delete")
    public void deptDelete(HttpServletRequest request, HttpServletResponse response) {
        ID dept = getIdParameterNotNull(request, "id");
        ID transfer = getIdParameter(request, "transfer");  // TODO 转移到新部门

        Application.getBean(DepartmentService.class).deleteAndTransfer(dept, transfer);
        writeSuccess(response);
    }

    @RequestMapping("dept-tree")
    public void deptTreeGet(HttpServletResponse response) {
        JSONArray dtree = new JSONArray();
        for (Department root : Application.getUserStore().getTopDepartments()) {
            dtree.add(recursiveDeptTree(root));
        }
        writeSuccess(response, dtree);
    }

    /**
     * 部门结构
     *
     * @param parent
     */
    private JSONObject recursiveDeptTree(Department parent) {
        JSONObject parentJson = new JSONObject();
        parentJson.put("id", parent.getIdentity());
        parentJson.put("name", parent.getName());
        parentJson.put("disabled", parent.isDisabled());
        JSONArray children = new JSONArray();
        for (BusinessUnit child : parent.getChildren()) {
            children.add(recursiveDeptTree((Department) child));
        }
        if (!children.isEmpty()) {
            parentJson.put("children", children);
        }
        return parentJson;
    }
}
