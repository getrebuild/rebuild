/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.RoleService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class RolePrivilegesControl extends EntityController {

    @GetMapping("role-privileges")
    public ModelAndView pageList(HttpServletRequest request) {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges", "Role", user);
        setEntities(mv);
        return mv;
    }

    @GetMapping("role/{id}")
    public ModelAndView pagePrivileges(@PathVariable String id, HttpServletRequest request) {
        ID user = getRequestUser(request);
        ID roleId = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges", "Role", user);
        setEntities(mv);
        mv.getModel().put("RoleId", roleId);
        return mv;
    }

    /**
     * @param mv
     */
    private void setEntities(ModelAndView mv) {
        List<Object[]> entities = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities()) {
            if (MetadataHelper.hasPrivilegesField(e)) {
                entities.add(new Object[]{e.getEntityCode(), EasyMeta.getLabel(e)});
            }
        }
        mv.getModel().put("Entities", entities);
    }

    @GetMapping("role-list")
    public void roleList(HttpServletResponse response) {
        Object[][] array = Application.createQuery("select roleId,name,isDisabled from Role").array();
        JSON retJson = JSONUtils.toJSONObjectArray(new String[]{"id", "name", "disabled"}, array);
        writeSuccess(response, retJson);
    }

    @GetMapping("privileges-list")
    public void privilegesList(HttpServletRequest request, HttpServletResponse response) {
        ID roleId = getIdParameterNotNull(request, "role");
        if (RoleService.ADMIN_ROLE.equals(roleId)) {
            writeFailure(response, getLang(request, "NotModifyAdminRole"));
            return;
        }

        Object[][] array = Application.createQuery(
                "select entity,definition,zeroKey from RolePrivileges where roleId = ?")
                .setParameter(1, roleId)
                .array();
        for (Object[] o : array) {
            if ((int) o[0] == 0) {
                o[0] = o[2];
            }
        }

        JSON retJson = JSONUtils.toJSONObjectArray(new String[]{"name", "definition"}, array);
        writeSuccess(response, retJson);
    }

    @PostMapping("privileges-update")
    public void privilegesUpdate(HttpServletRequest request, HttpServletResponse response) {
        JSON post = ServletUtils.getRequestJson(request);
        ID role = getIdParameterNotNull(request, "role");
        Application.getBean(RoleService.class).updatePrivileges(role, (JSONObject) post);
        writeSuccess(response);
    }

    @PostMapping("role-delete")
    public void roleDelete(HttpServletRequest request, HttpServletResponse response) {
        ID role = getIdParameterNotNull(request, "id");
        ID transfer = getIdParameter(request, "transfer");  // TODO 转移到新角色

        Application.getBean(RoleService.class).deleteAndTransfer(role, transfer);
        writeSuccess(response);
    }
}
