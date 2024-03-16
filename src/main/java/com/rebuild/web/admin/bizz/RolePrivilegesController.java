/*!
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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.RoleService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@RestController
@RequestMapping("/admin/bizuser/")
public class RolePrivilegesController extends EntityController {

    @GetMapping("role-privileges")
    public ModelAndView pageList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges", "Role", user);

        setEntities(mv);
        return mv;
    }

    @GetMapping("role/{id}")
    public ModelAndView pagePrivileges(@PathVariable ID id, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges", "Role", user);

        setEntities(mv);
        mv.getModel().put("RoleId", id);
        return mv;
    }

    private void setEntities(ModelAndView mv) {
        List<Object[]> entities = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities()) {
            if (MetadataHelper.hasPrivilegesField(e)) {
                entities.add(new Object[]{e.getEntityCode(), e.getName(), EasyMetaFactory.getLabel(e)});
            }
        }
        mv.getModel().put("Entities", entities);
    }

    @GetMapping("role-list")
    public JSON roleList() {
        Object[][] array = Application.createQuery(
                "select roleId,name,isDisabled from Role")
                .array();
        // 排序 a-z
        Arrays.sort(array, (o1, o2) -> {
            if (RoleService.ADMIN_ROLE.equals(o1[0])) return -1;
            else if (RoleService.ADMIN_ROLE.equals(o2[0])) return 1;
            else return ((String) o1[1]).compareTo((String) o2[1]);
        });

        return JSONUtils.toJSONObjectArray(
                new String[] { "id", "name", "disabled" }, array);
    }

    @GetMapping("privileges-list")
    public RespBody privilegesList(@IdParam(name = "role") ID roleId) {
        if (RoleService.ADMIN_ROLE.equals(roleId)) {
            return RespBody.errorl("系统内置管理员角色，不允许修改。此角色拥有高级系统权限，请谨慎使用");
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

        JSON retJson = JSONUtils.toJSONObjectArray(
                new String[] { "name", "definition" }, array);
        return RespBody.ok(retJson);
    }

    @PostMapping("privileges-update")
    public RespBody privilegesUpdate(@IdParam(name = "role") ID roleId, HttpServletRequest request) {
        JSON post = ServletUtils.getRequestJson(request);

        Application.getBean(RoleService.class).updatePrivileges(roleId, (JSONObject) post);
        return RespBody.ok();
    }

    @PostMapping("role-delete")
    public RespBody roleDelete(@IdParam ID roleId, HttpServletRequest request) {
        ID transferTo = getIdParameter(request, "transfer");  // TODO 转移到新角色
        Application.getBean(RoleService.class).deleteAndTransfer(roleId, transferTo);
        return RespBody.ok();
    }

    @PostMapping("role-copyto")
    public RespBody roleCopyTo(@RequestBody JSONObject post) {
        ID from  = ID.valueOf(post.getString("from"));
        Set<ID> tos = new HashSet<>();
        for (Object s : post.getJSONArray("copyTo")) {
            if (ID.isId(s)) tos.add(ID.valueOf(s.toString()));
        }

        Application.getBean(RoleService.class).updateWithCopyTo(from, tos.toArray(new ID[0]));
        return RespBody.ok();
    }
}
