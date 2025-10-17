/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.EasyActionManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.EntityOverview;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.CopyEntity;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.metadata.impl.ExcelEntity;
import com.rebuild.core.metadata.impl.MetaEntityService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.rbstore.MetaschemaExporter;
import com.rebuild.core.service.general.QuickCodeReindexTask;
import com.rebuild.core.service.general.series.SeriesGeneratorFactory;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.EntityController;
import com.rebuild.web.commons.FileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.rebuild.web.commons.LanguageController.putLocales;
import static org.apache.commons.lang3.Strings.CI;

/**
 * @author Zixin (RB)
 * @since 08/03/2018
 */
@Slf4j
@RestController
@RequestMapping("/admin/")
public class MetaEntityController extends EntityController {

    @GetMapping("entities")
    public ModelAndView page(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entities");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        return mv;
    }

    @GetMapping("entity/{entity}/base")
    public ModelAndView pageBase(@PathVariable String entity, HttpServletResponse response) throws IOException {
        if (StringUtils.isNumeric(entity)) {
            int entityCode = NumberUtils.toInt(entity);
            if (MetadataHelper.containsEntity(entityCode)) {
                response.sendRedirect("../" + MetadataHelper.getEntity(entityCode).getName() + "/base");
                return null;
            }
        }

        Entity metaEntity = MetadataHelper.getEntity(entity);

        // 不允许访问
        if (!(MetadataHelper.isBusinessEntity(metaEntity) || MetadataHelper.isBizzEntity(metaEntity))) {
            response.sendError(403, "SYSTEM ENTITY IS PROHIBITED");
            return null;
        }

        ModelAndView mv = createModelAndView("/admin/metadata/entity-edit");
        EasyEntity easyEntity = setEntityBase(mv, entity);

        mv.getModel().put("nameField", metaEntity.getNameField().getName());

        mv.getModel().put("currentEntity", metaEntity.getName());
        if (metaEntity.getMainEntity() != null) {
            mv.getModel().put("mainEntity", metaEntity.getMainEntity().getName());
            mv.getModel().put("detailEntity", metaEntity.getName());
            mv.getModel().put("detailEntities", buildDetailEntities(metaEntity.getMainEntity()));
        } else if (metaEntity.getDetailEntity() != null) {
            mv.getModel().put("mainEntity", metaEntity.getName());
            mv.getModel().put("detailEntity", metaEntity.getDetailEntity().getName());
            mv.getModel().put("detailEntities", buildDetailEntities(metaEntity));
        }

        // 扩展配置
        mv.getModel().put("entityExtConfig", easyEntity.getExtraAttrs(true));

        return mv;
    }

    @GetMapping("entity/{entity}/advanced")
    public ModelAndView pageAdvanced(@PathVariable String entity, HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entity-advanced");
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
        EasyEntity easyEntity = setEntityBase(mv, entity);

        // 扩展配置
        mv.getModel().put("entityExtConfig", easyEntity.getExtraAttrs(true));

        boolean isBizz = MetadataHelper.isBizzEntity(easyEntity.getRawMeta());
        mv.getModel().put("isBizz", isBizz);
        return mv;
    }

    @GetMapping("entity/{entity}/overview")
    public ModelAndView pageOverview(@PathVariable String entity) {
        ModelAndView mv = createModelAndView("/admin/metadata/entity-overview");
        EasyEntity easyEntity = setEntityBase(mv, entity);

        EntityOverview o = new EntityOverview(easyEntity.getRawMeta());
        mv.getModel().put("overview", JSON.toJSON(o.overview()));

        return mv;
    }

    @GetMapping("entity/{entity}/easy-action")
    public ModelAndView pageEasyAction(@PathVariable String entity) {
        ModelAndView mv = createModelAndView("/admin/metadata/entity-easy-action");
        EasyEntity easyEntity = setEntityBase(mv, entity);
        if (easyEntity.getRawMeta().getMainEntity() != null) {
            mv.getModel().put("mainEntity", easyEntity.getRawMeta().getMainEntity().getName());
        }

        ConfigBean cb = EasyActionManager.instance.getEasyActionRaw(entity);
        if (cb != null) {
            mv.getModelMap().put("configId", cb.getID("id"));
            mv.getModelMap().put("config", cb.getJSON("config"));
        }

        boolean isBizz = MetadataHelper.isBizzEntity(easyEntity.getRawMeta());
        mv.getModel().put("isBizz", isBizz);
        return mv;
    }

    @RequestMapping("entity/entity-list")
    public RespBody listEntity(HttpServletRequest request) {
        boolean usesBizz = getBoolParameter(request, "bizz", false);
        boolean usesDetail = getBoolParameter(request, "detail", false);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Entity entity : MetadataSorter.sortEntities(null, usesBizz, usesDetail)) {
            EasyEntity easyMeta = EasyMetaFactory.valueOf(entity);
            Map<String, Object> map = new HashMap<>();
            map.put("entityName", easyMeta.getName());
            map.put("entityLabel", easyMeta.getLabel());
            map.put("quickCode", QuickCodeReindexTask.generateQuickCode(easyMeta.getLabel()));
            map.put("comments", easyMeta.getComments());
            map.put("icon", easyMeta.getIcon());
            map.put("builtin", easyMeta.isBuiltin());
            if (entity.getDetailEntity() != null) map.put("detailEntity", entity.getDetailEntity().getName());
            if (entity.getMainEntity() != null) map.put("mainEntity", entity.getMainEntity().getName());
            map.put("hadApproval", MetadataHelper.hasApprovalField(entity));
            map.put("tags", easyMeta.getExtraAttr(EasyEntityConfigProps.TAGS));
            data.add(map);
        }
        return RespBody.ok(data);
    }

    @PostMapping("entity/entity-new")
    public RespBody entityNew(HttpServletRequest request) {
        final JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

        String label = reqJson.getString("label");
        String comments = reqJson.getString("comments");
        String mainEntity = reqJson.getString("mainEntity");
        if (StringUtils.isNotBlank(mainEntity)) {
            if (!MetadataHelper.containsEntity(mainEntity)) {
                return RespBody.errorl("无效主实体 : %s", mainEntity);
            }

            Entity useMain = MetadataHelper.getEntity(mainEntity);
            if (useMain.getMainEntity() != null) {
                return RespBody.errorl("明细实体不能作为主实体");
            }

            if (useMain.getDetailEntity() != null && !License.isCommercial()) {
                return RespBody.errorl(
                        "免费版不支持%s功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)", "多明细");
            }
        }

        try {
            String entityName = new Entity2Schema().createEntity(
                    null, label, comments, mainEntity, getBoolParameter(request, "nameField"), getBoolParameter(request, "seriesField"));
            return RespBody.ok(entityName);
        } catch (Exception ex) {
            log.error("entity-new", ex);
            return RespBody.error(ex);
        }
    }

    @PostMapping("entity/entity-update")
    public RespBody entityUpdate(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final JSON formJson = ServletUtils.getRequestJson(request);

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

        return RespBody.ok();
    }

    @RequestMapping("entity/entity-drop")
    public RespBody entityDrop(HttpServletRequest request) {
        final Entity entity = getEntityById(getIdParameterNotNull(request, "id"));
        final boolean force = getBoolParameter(request, "force", false);

        try {
            boolean drop = new Entity2Schema().dropEntity(entity, force);
            return drop ? RespBody.ok() : RespBody.error();

        } catch (Exception ex) {
            log.error("entity-drop", ex);
            return RespBody.error(ex);
        }
    }

    @RequestMapping("entity/entity-truncate")
    public RespBody entityTruncate(HttpServletRequest request) {
        final Entity entity = getEntityById(getIdParameterNotNull(request, "id"));

        try {
            String dsql = String.format("TRUNCATE TABLE `%s`", entity.getPhysicalName());
            Application.getSqlExecutor().execute(dsql);
            // 置零
            for (Field s : MetadataSorter.sortFields(entity, DisplayType.SERIES)) {
                SeriesGeneratorFactory.zero(s);
            }
            return RespBody.ok();

        } catch (Exception ex) {
            log.error("entity-truncate", ex);
            return RespBody.error(ex);
        }
    }

    @GetMapping("entity/entity-export")
    public void entityExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Entity entity = getEntityById(getIdParameterNotNull(request, "id"));

        File dest = RebuildConfiguration.getFileOfTemp("schema-" + entity.getName() + ".json");
        if (dest.exists()) FileUtils.deleteQuietly(dest);

        new MetaschemaExporter(entity, true).export(dest);

        if (ServletUtils.isAjaxRequest(request)) {
            writeSuccess(response, JSONUtils.toJSONObject("file", dest.getName()));
        } else {
            FileDownloader.downloadTempFile(response, dest);
        }
    }

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
    protected static EasyEntity setEntityBase(ModelAndView mv, String entity) {
        EasyEntity entityMeta = EasyMetaFactory.valueOf(entity);
        mv.getModel().put("entityMetaId", entityMeta.getMetaId());
        mv.getModel().put("entityName", entityMeta.getName());
        mv.getModel().put("entityLabel", entityMeta.getLabel());
        mv.getModel().put("icon", entityMeta.getIcon());
        mv.getModel().put("comments", entityMeta.getComments());
        mv.getModel().put("entityCode", entityMeta.getRawMeta().getEntityCode());
        return entityMeta;
    }

    @PostMapping("entity/entity-copy")
    public RespBody entityCopy(HttpServletRequest request) {
        final JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

        Entity sourceEntity = MetadataHelper.getEntity(reqJson.getString("sourceEntity"));
        String entityName = reqJson.getString("entityName");
        String detailEntityName = reqJson.getString("detailEntityName");

        try {
            entityName = new CopyEntity(sourceEntity).copy(entityName, detailEntityName);
            return RespBody.ok(entityName);
        } catch (Exception ex) {
            log.error("entity-copy", ex);
            return RespBody.error(ex);
        }
    }

    @GetMapping("entity/entity-tags")
    public RespBody entityTags() {
        Set<String> set = new TreeSet<>();
        for (Entity entity : MetadataHelper.getEntities()) {
            String tags = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.TAGS);
            if (StringUtils.isNotBlank(tags)) {
                Collections.addAll(set, tags.split(","));
            }
        }
        return RespBody.ok(set);
    }

    @GetMapping("entities/sheet")
    public ModelAndView pageSheet(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/entities-sheet");

        String spec = getParameter(request, "s");
        Set<String> specList = null;
        if (StringUtils.isNotBlank(spec)) {
            specList = new HashSet<>();
            for (String s : spec.split(",")) specList.add(s.trim().toUpperCase());
        }

        List<Object[]> entities = new ArrayList<>();
        for (Entity e : MetadataHelper.getEntities()) {
            if (specList != null && !specList.contains(e.getName().toUpperCase())) continue;

            final EasyEntity ee = EasyMetaFactory.valueOf(e);
            if (ee.isBuiltin() && !MetadataHelper.isBizzEntity(e)) continue;

            List<Object[]> fields = new ArrayList<>();
            fields.add(new Object[]
                    { "ID", e.getName() + "Id", DisplayType.ID.getDisplayName(), "-", "-", "N/N/N/N" });

            for (Field f : MetadataSorter.sortFields(e)) {
                final EasyField ef = EasyMetaFactory.valueOf(f);
                final DisplayType dt = ef.getDisplayType();

                String ref = "-";
                if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
                    ref = "e:" + f.getReferenceEntity().getName();
                } else if (dt == DisplayType.CLASSIFICATION) {
                    ID cid = ClassificationManager.instance.getUseClassification(f, Boolean.FALSE);
                    ref = "c:" + FieldValueHelper.getLabelNotry(cid);
                }

                String opt = "-";
                if (dt == DisplayType.PICKLIST) {
                    ConfigBean[] cbs = PickListManager.instance.getPickListRaw(f, Boolean.TRUE);
                    List<String> texts = new ArrayList<>();
                    for (ConfigBean cb : cbs) texts.add(cb.getID("id") + ":" + cb.getString("text"));
                    opt = StringUtils.join(texts, "//");
                }

                fields.add(new Object[] {
                        ef.getLabel(), f.getName(), ef.getDisplayType().getDisplayName(), ref, opt,
                        (ef.isCreatable() ? "Y" : "N") + (ef.isUpdatable() ? "/Y" : "/N") + (ef.isNullable() ? "/Y" : "/N") + (ef.isRepeatable() ? "/Y" : "/N") });
            }

            entities.add(new Object[] { e.getName(), e.getEntityCode(), ee.getLabel(), fields });
        }

        mv.getModel().put("entities", entities);
        return mv;
    }

    @PostMapping("entity/entity-excel")
    public RespBody entityExcelEvalft(HttpServletRequest request) {
        final JSON post = ServletUtils.getRequestJson(request);
        String entityLabel = ((JSONObject) post).getString("entityLabel");
        JSONArray fields = ((JSONObject) post).getJSONArray("fields");

        String entityName = new ExcelEntity().imports(entityLabel, fields);
        return RespBody.ok(entityName);
    }

    @GetMapping("entities/search")
    public JSON entitiesSearch(HttpServletRequest request) {
        String q = getParameterNotNull(request, "q");
        q = StringEscapeUtils.escapeSql(q).toUpperCase();

        JSONArray res = new JSONArray();
        for (Entity e : MetadataSorter.sortEntities()) {
            String name = e.getName();
            String label = EasyMetaFactory.getLabel(e);
            String quickCode = QuickCodeReindexTask.generateQuickCode(label);
            if (CI.contains(name, q) || CI.contains(label, q) || CI.contains(quickCode, q)) {
                res.add(JSONUtils.toJSONObject(new String[]{"name", "label"}, new Object[]{name, label}));
            }

            for (Field f : MetadataSorter.sortFields(e)) {
                String fName = f.getName();
                String fLabel = EasyMetaFactory.getLabel(f);
                String fQuickCode = QuickCodeReindexTask.generateQuickCode(fLabel);
                if (CI.contains(fName, q) || CI.contains(fLabel, q) || CI.contains(fQuickCode, q)) {
                    res.add(JSONUtils.toJSONObject(
                            new String[]{"name", "label", "entity"}, new Object[]{fName, label + "." + fLabel, name}));
                }
            }
        }

        // 按 label 排序
        res.sort(Comparator.comparing((Object o) -> ((JSONObject) o).getString("label")));
        return res;
    }

    // @see com.rebuild.rbv.admin.LanguageAdminController

    @GetMapping("entity/{entity}/i18n")
    public ModelAndView pageI18n(@PathVariable String entity) {
        RbAssert.isCommercial(
                Language.L("免费版不支持多语言功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)"));

        ModelAndView mv = createModelAndView("/admin/metadata/entity-i18n");
        putLocales(mv, UserContextHolder.getLocale());
        setEntityBase(mv, entity);
        return mv;
    }

    @GetMapping("entity/{entity}/i18n-list")
    public RespBody listI18n(@PathVariable String entity) {
        Entity e = MetadataHelper.getEntity(entity);
        String key = "META." + e.getName();
        Set<String> locales = Application.getLanguage().availableLocales().keySet();

        List<Map<String, String>> i18nList = new ArrayList<>();
        i18nList.add(buildI18nItem(e, key, locales));

        key += ".";
        for (Field field : MetadataSorter.sortFields(e)) {
            i18nList.add(buildI18nItem(field, key + field.getName(), locales));
        }
        return RespBody.ok(i18nList);
    }

    private Map<String, String> buildI18nItem(BaseMeta entityOrField, String key, Set<String> locales) {
        key = key.toUpperCase();
        Map<String, String> i18n = new HashMap<>();
        i18n.put("_key", key);
        i18n.put("_def", entityOrField.getDescription());
        for (String L : locales) {
            i18n.put(L, Application.getLanguage().getBundle(L).getLang(key));
        }
        return i18n;
    }
}
