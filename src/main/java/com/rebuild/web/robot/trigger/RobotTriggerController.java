/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.ActionFactory;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.admin.data.ReportTemplateController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
@RestController
@RequestMapping("/admin/robot/")
public class RobotTriggerController extends BaseController {

    @GetMapping("triggers")
    public ModelAndView pageList() {
        return createModelAndView("/admin/robot/trigger-list");
    }

    @GetMapping("trigger/{id}")
    public ModelAndView pageEditor(@PathVariable String id,
                                   HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID configId = ID.valueOf(id);
        Object[] config = Application.createQuery(
                "select belongEntity,actionType,when,whenFilter,actionContent,priority,name,whenTimer from RobotTriggerConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        if (config == null) {
            response.sendError(404);
            return null;
        }

        Entity sourceEntity = MetadataHelper.getEntity((String) config[0]);
        ActionType actionType = ActionType.valueOf((String) config[1]);

        ModelAndView mv = createModelAndView("/admin/robot/trigger-design");
        mv.getModel().put("configId", configId);
        mv.getModel().put("sourceEntity", sourceEntity.getName());
        mv.getModel().put("sourceEntityLabel", EasyMetaFactory.getLabel(sourceEntity));
        mv.getModel().put("actionType", actionType.name());
        mv.getModel().put("actionTypeLabel", getLang(request, actionType.name()));
        mv.getModel().put("when", config[2]);
        mv.getModel().put("whenTimer", config[7] == null ? StringUtils.EMPTY : config[7]);
        mv.getModel().put("whenFilter", StringUtils.defaultIfBlank((String) config[3], JSONUtils.EMPTY_OBJECT_STR));
        mv.getModel().put("actionContent", StringUtils.defaultIfBlank((String) config[4], JSONUtils.EMPTY_OBJECT_STR));
        mv.getModel().put("priority", config[5]);
        mv.getModel().put("name", config[6]);
        return mv;
    }

    @GetMapping("trigger/available-actions")
    public List<String[]> getAvailableActions(HttpServletRequest request) {
        List<String[]> alist = new ArrayList<>();
        for (ActionType t : ActionFactory.getAvailableActions()) {
            alist.add(new String[] { t.name(), getLang(request, t.name()) });
        }
        return alist;
    }

    @GetMapping("trigger/available-entities")
    public List<String[]> getAvailableEntities(HttpServletRequest request) {
        String actionType = getParameterNotNull(request, "action");
        TriggerAction action = ActionFactory.createAction(actionType);

        List<String[]> alist = new ArrayList<>();
        for (Entity e : MetadataSorter.sortEntities(null, false, true)) {
            if (action.isUsableSourceEntity(e.getEntityCode())) {
                alist.add(new String[]{e.getName(), EasyMetaFactory.getLabel(e)});
            }
        }
        return alist;
    }

    @GetMapping("trigger/list")
    public Object[][] triggerList(HttpServletRequest request) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");
        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn,when,actionType from RobotTriggerConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] array = ReportTemplateController.queryListOfConfig(sql, belongEntity, q);
        for (Object[] o : array) {
            o[7] = getLang(request, (String) o[7]);
        }
        return array;
    }
}
