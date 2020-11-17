/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.metadata.impl.MetaEntityService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.rbstore.MetaSchemaGenerator;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.FileDownloader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Controller
@RequestMapping("/admin/")
public class MetaEntityController extends BaseController {

    @GetMapping("entities")
    public ModelAndView page(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entities");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        return mv;
    }

    @GetMapping("entity/{entity}/base")
    public ModelAndView pageBase(@PathVariable String entity, HttpServletResponse response) throws IOException {
        Entity metaEntity = MetadataHelper.getEntity(entity);
        if (!(MetadataHelper.isBusinessEntity(metaEntity) || MetadataHelper.isBizzEntity(metaEntity.getEntityCode()))) {
            response.sendError(403);
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/metadata/entity-edit");
        setEntityBase(mv, entity);

        mv.getModel().put("nameField", MetadataHelper.getNameField(metaEntity).getName());

        if (metaEntity.getMainEntity() != null) {
            mv.getModel().put("mainEntity", metaEntity.getMainEntity().getName());
            mv.getModel().put("detailEntity", metaEntity.getName());
        } else if (metaEntity.getDetailEntity() != null) {
            mv.getModel().put("mainEntity", metaEntity.getName());
            mv.getModel().put("detailEntity", metaEntity.getDetailEntity().getName());
        }

        // 扩展配置
        mv.getModel().put("entityExtConfig", EasyMetaFactory.valueOf(metaEntity).getExtraAttrs(true));

        return mv;
    }

    @GetMapping("entity/{entity}/advanced")
    public ModelAndView pageAdvanced(@PathVariable String entity, HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entity-advanced");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        setEntityBase(mv, entity);
        return mv;
    }

    @ResponseBody
    @RequestMapping("entity/entity-list")
    public Object listEntity(HttpServletRequest request) {
        // 默认无BIZZ实体
        final boolean usesBizz = getBoolParameter(request, "bizz", false);
        // 默认无明细实体
        final boolean usesDetail = getBoolParameter(request, "detail", false);

        List<Map<String, Object>> ret = new ArrayList<>();
        for (Entity entity : MetadataSorter.sortEntities(null, usesBizz, usesDetail)) {
            EasyEntity easyMeta = EasyMetaFactory.valueOf(entity);
            Map<String, Object> map = new HashMap<>();
            map.put("entityName", easyMeta.getName());
            map.put("entityLabel", easyMeta.getLabel());
            map.put("comments", easyMeta.getComments());
            map.put("icon", easyMeta.getIcon());
            map.put("builtin", easyMeta.isBuiltin());
            if (entity.getDetailEntity() != null) {
                map.put("detailEntity", entity.getDetailEntity().getName());
            }
            if (entity.getMainEntity() != null) {
                map.put("mainEntity", entity.getMainEntity().getName());
            }
            ret.add(map);
        }
        return ret;
    }

    @PostMapping("entity/entity-new")
    public void entityNew(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

        String label = reqJson.getString("label");
        String comments = reqJson.getString("comments");
        String mainEntity = reqJson.getString("mainEntity");
        if (StringUtils.isNotBlank(mainEntity)) {
            if (!MetadataHelper.containsEntity(mainEntity)) {
                writeFailure(response,
                        getLang(request, "SomeInvalid", "MainEntity") + " : " + mainEntity);
                return;
            }

            Entity useMain = MetadataHelper.getEntity(mainEntity);
            if (useMain.getMainEntity() != null) {
                writeFailure(response, getLang(request, "DetailEntityNotBeMain"));
                return;
            } else if (useMain.getDetailEntity() != null) {
                writeFailure(response,
                        String.format(getLang(request, "SelectMainEntityBeXUsed"), useMain.getDetailEntity()));
                return;
            }
        }

        try {
            String entityName = new Entity2Schema(user)
                    .createEntity(label, comments, mainEntity, getBoolParameter(request, "nameField"));
            writeSuccess(response, entityName);
        } catch (Exception ex) {
            LOG.error("entity-new", ex);
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    @RequestMapping("entity/entity-update")
    public void entityUpdate(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);

        // 修改了名称字段
        String needReindex = null;
        String nameField = record.getString("nameField");
        if (nameField != null) {
            Object[] nameFieldOld = Application.createQueryNoFilter(
                    "select nameField,entityName from MetaEntity where entityId = ?")
                    .setParameter(1, record.getPrimary())
                    .unique();
            if (!nameField.equalsIgnoreCase((String) nameFieldOld[0])) {
                needReindex = (String) nameFieldOld[1];
            }
        }

        Application.getBean(MetaEntityService.class).update(record);

        if (needReindex != null) {
            Entity entity = MetadataHelper.getEntity(needReindex);
            if (entity.containsField(EntityHelper.QuickCode)) {
                QuickCodeReindexTask reindexTask = new QuickCodeReindexTask(entity);
                TaskExecutors.submit(reindexTask, user);
            }
        }

        writeSuccess(response);
    }

    @RequestMapping("entity/entity-drop")
    public void entityDrop(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        Entity entity = getEntityById(getIdParameterNotNull(request, "id"));
        boolean force = getBoolParameter(request, "force", false);

        try {
            boolean drop = new Entity2Schema(user).dropEntity(entity, force);
            if (drop) writeSuccess(response);
            else writeFailure(response);

        } catch (Exception ex) {
            LOG.error("entity-drop", ex);
            writeFailure(response, ex.getLocalizedMessage());
        }
    }

    @GetMapping("entity/entity-export")
    public void entityExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Entity entity = getEntityById(getIdParameterNotNull(request, "id"));

        File dest = RebuildConfiguration.getFileOfTemp("schema-" + entity.getName() + ".json");
        if (dest.exists()) {
            FileUtils.deleteQuietly(dest);
        }
        new MetaSchemaGenerator(entity).generate(dest);

        if (ServletUtils.isAjaxRequest(request)) {
            writeSuccess(response, JSONUtils.toJSONObject("file", dest.getName()));
        } else {
            FileDownloader.setDownloadHeaders(request, response, dest.getName());
            FileDownloader.writeLocalFile(dest.getName(), true, response);
        }
    }

    /**
     * @param metaId
     * @return
     */
    private Entity getEntityById(ID metaId) {
        Object[] entityRecord = Application.createQueryNoFilter(
                "select entityName from MetaEntity where entityId = ?")
                .setParameter(1, metaId)
                .unique();
        String entityName = (String) entityRecord[0];
        return MetadataHelper.getEntity(entityName);
    }

    /**
     * 设置实体信息
     *
     * @param mv
     * @param entity
     * @return
     */
    static EasyEntity setEntityBase(ModelAndView mv, String entity) {
        EasyEntity entityMeta = EasyMetaFactory.valueOf(entity);
        mv.getModel().put("entityMetaId", entityMeta.getMetaId());
        mv.getModel().put("entityName", entityMeta.getName());
        mv.getModel().put("entityLabel", entityMeta.getLabel());
        mv.getModel().put("icon", entityMeta.getIcon());
        mv.getModel().put("comments", entityMeta.getComments());
        return entityMeta;
    }
}
