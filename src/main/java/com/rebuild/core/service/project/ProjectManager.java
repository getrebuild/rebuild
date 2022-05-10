/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/29
 */
public class ProjectManager implements ConfigManager {

    public static final ProjectManager instance = new ProjectManager();


    private ProjectManager() {
    }

    // 已归档
    public static final int STATUS_ARCHIVED = 2;

    private static final String CKEY_PROJECTS = "ProjectManager2";
    private static final String CKEY_PLANS = "ProjectPlan-";
    private static final String CKEY_TP2P = "TP2Project-";

    /**
     * 获取指定用户可用项目
     *
     * @param user
     * @return
     */
    public ConfigBean[] getAvailable(ID user) {
        ConfigBean[] projects = getProjects();

        // 管理员可见全部
        boolean isAdmin = UserHelper.isAdmin(user);

        List<ConfigBean> alist = new ArrayList<>();
        for (ConfigBean e : projects) {
            boolean isMember = e.get("members", Set.class).contains(user);
            boolean isPublic = e.getInteger("scope") == ProjectConfigService.SCOPE_ALL;
            if (isAdmin || isMember || isPublic) {
                boolean isArchived = e.getInteger("status") == STATUS_ARCHIVED;
                if (isArchived) {
                    // 仅负责人
                    boolean isPrincipal = user.equals(e.getID("principal"));
                    if (isAdmin || isPrincipal) alist.add(e.clone());
                } else {
                    alist.add(e.clone());
                }
            }
        }
        return alist.toArray(new ConfigBean[0]);
    }

    /**
     * 获取全部项目
     *
     * @return
     */
    private ConfigBean[] getProjects() {
        ConfigBean[] cached = (ConfigBean[]) Application.getCommonsCache().getx(CKEY_PROJECTS);

        if (cached == null) {
            Object[][] array = Application.createQueryNoFilter(
                    "select configId,projectCode,projectName,iconName,scope,members,principal,extraDefinition,status from ProjectConfig")
                    .array();

            List<ConfigBean> alist = new ArrayList<>();
            for (Object[] o : array) {
                String members = (String) o[5];
                if (o[6] != null) {
                    members = StringUtils.isBlank(members) ? o[6].toString() : members + "," + o[6];
                }

                ConfigBean e = new ConfigBean()
                        .set("id", o[0])
                        .set("projectCode", o[1])
                        .set("projectName", o[2])
                        .set("iconName", StringUtils.defaultIfBlank((String) o[3], "texture"))
                        .set("scope", o[4])
                        .set("_members", members)
                        .set("principal", o[6])
                        .set("status", ObjectUtils.toInt(o[8], 1));

                // 扩展配置
                String extraDefinition = (String) o[7];
                if (JSONUtils.wellFormat(extraDefinition)) {
                    JSONObject extraDefinitionJson = JSON.parseObject(extraDefinition);
                    for (String name : extraDefinitionJson.keySet()) {
                        e.set(name, extraDefinitionJson.get(name));
                    }
                }
                alist.add(e);
            }

            cached = alist.toArray(new ConfigBean[0]);
            Application.getCommonsCache().putx(CKEY_PROJECTS, cached);
        }

        for (ConfigBean p : cached) {
            Set<ID> members = Collections.emptySet();
            String userDefs = p.getString("_members");
            if (StringUtils.isNotBlank(userDefs) && userDefs.length() >= 20) {
                members = UserHelper.parseUsers(Arrays.asList(userDefs.split(",")), null);
            }
            p.set("members", members);
        }
        return cached;
    }

    /**
     * 获取指定项目
     *
     * @param projectId
     * @param checkUser
     * @return
     * @throws ConfigurationException If not found
     */
    public ConfigBean getProject(ID projectId, ID checkUser) throws ConfigurationException {
        ConfigBean[] ee = checkUser == null ? getProjects() : getAvailable(checkUser);
        for (ConfigBean e : ee) {
            if (projectId.equals(e.getID("id"))) {
                return e.clone();
            }
        }
        throw new ConfigurationException(Language.L("无权访问该项目或项目已删除"));
    }

    /**
     * 获取项目的任务面板
     *
     * @param projectId
     * @return
     */
    public ConfigBean[] getPlansOfProject(ID projectId) {
        Assert.notNull(projectId, "[projectId] cannot be null");

        final String ckey = CKEY_PLANS + projectId;
        ConfigBean[] cached = (ConfigBean[]) Application.getCommonsCache().getx(ckey);

        if (cached == null) {
            Object[][] array = Application.createQueryNoFilter(
                    "select configId,planName,flowStatus,flowNexts from ProjectPlanConfig where projectId = ? order by seq")
                    .setParameter(1, projectId)
                    .array();

            List<ConfigBean> alist = new ArrayList<>();
            for (Object[] o : array) {
                ConfigBean e = new ConfigBean()
                        .set("id", o[0])
                        .set("planName", o[1])
                        .set("flowStatus", o[2]);

                if (StringUtils.isNotBlank((String) o[3])) {
                    List<ID> nexts = new ArrayList<>();
                    for (String s : ((String) o[3]).split(",")) {
                        nexts.add(ID.valueOf(s));
                    }
                    e.set("flowNexts", nexts);
                }
                alist.add(e);
            }

            cached = alist.toArray(new ConfigBean[0]);
            Application.getCommonsCache().putx(ckey, cached);
        }
        return cached.clone();
    }

    /**
     * @param planId
     * @param projectId
     * @return
     */
    public ConfigBean getPlanOfProject(ID planId, ID projectId) {
        if (projectId == null) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(planId, "projectId");
            projectId = o != null ? (ID) o[0] : null;
        }

        for (ConfigBean e : getPlansOfProject(projectId)) {
            if (e.getID("id").equals(planId)) return e;
        }
        throw new ConfigurationException(Language.L("无效任务面板 (%s)", planId));
    }

    /**
     * @param taskOrPlan
     * @param checkUser
     * @return
     */
    public ConfigBean getProjectByX(ID taskOrPlan, ID checkUser) {
        final String ckey = CKEY_TP2P + taskOrPlan;
        ID projectId = (ID) Application.getCommonsCache().getx(ckey);

        if (projectId == null) {
            Object[] x = Application.getQueryFactory().uniqueNoFilter(taskOrPlan, "projectId");
            projectId = x == null ? null : (ID) x[0];
            if (projectId != null) {
                Application.getCommonsCache().putx(ckey, projectId);
            }
        }

        if (projectId == null) {
            throw new ConfigurationException(Language.L("任务/面板不存在或已被删除"));
        }

        try {
            return getProject(projectId, checkUser);
        } catch (ConfigurationException ex) {
            throw new AccessDeniedException(Language.L("无权访问该项目"), ex);
        }
    }


    @Override
    public void clean(Object nullOrAnyProjectId) {
        int e = nullOrAnyProjectId == null ? -1 : ((ID) nullOrAnyProjectId).getEntityCode();
        // 清理项目
        if (e == -1) {
            Application.getCommonsCache().evict(CKEY_PROJECTS);
        }
        // 清理面板
        else if (e == EntityHelper.ProjectConfig) {
            Application.getCommonsCache().evict(CKEY_PLANS + nullOrAnyProjectId);
        }
        // 清理任务
        else if (e == EntityHelper.ProjectTask || e == EntityHelper.ProjectPlanConfig) {
            Application.getCommonsCache().evict(CKEY_TP2P + nullOrAnyProjectId);
            Application.getCommonsCache().evict(CKEY_TP2P + nullOrAnyProjectId);
        }
    }
}
