/*!
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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.privileges.DepartmentService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@RestController
@RequestMapping("/admin/bizuser/")
public class DepartmentController extends EntityController {

    @GetMapping("departments")
    public ModelAndView pageList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/dept-list", "Department", user);

        JSON config = DataListManager.instance.getListFields("Department", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @PostMapping("dept-delete")
    public RespBody deptDelete(@IdParam ID deptId, HttpServletRequest request) {
        ID transfer = getIdParameter(request, "transfer");  // TODO 转移到新部门

        Application.getBean(DepartmentService.class).deleteAndTransfer(deptId, transfer);
        return RespBody.ok();
    }

    @GetMapping("dept-tree")
    public JSON deptTreeGet() {
        JSONArray dtree = new JSONArray();

        Department[] ds = Application.getUserStore().getTopDepartments();
        sortByName(ds);
        for (Department root : ds) {
            dtree.add(recursiveDeptTree(root));
        }
        return dtree;
    }

    private JSONObject recursiveDeptTree(Department parent) {
        JSONObject parentJson = new JSONObject();
        parentJson.put("id", parent.getIdentity());
        parentJson.put("name", parent.getName());
        parentJson.put("disabled", parent.isDisabled());
        JSONArray children = new JSONArray();

        BusinessUnit[] ds = parent.getChildren().toArray(new BusinessUnit[0]);
        sortByName(ds);
        for (BusinessUnit child : ds) {
            children.add(recursiveDeptTree((Department) child));
        }

        if (!children.isEmpty()) {
            parentJson.put("children", children);
        }
        return parentJson;
    }

    private void sortByName(BusinessUnit[] depts) {
        // 排序 a-z
        Arrays.sort(depts, (o1, o2) -> {
            if (DepartmentService.ROOT_DEPT.equals(o1.getIdentity())) return -1;
            else if (DepartmentService.ROOT_DEPT.equals(o2.getIdentity())) return 1;
            else return o1.getName().compareTo(o2.getName());
        });
    }
}
