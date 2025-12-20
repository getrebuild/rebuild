/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.general.RepeatedRecordsException;
import com.rebuild.core.service.general.transform.RecordTransfomer39;
import com.rebuild.core.service.general.transform.RecordTransfomer43;
import com.rebuild.core.service.trigger.DataValidateException;
import com.rebuild.core.support.RbvFunction;
import com.rebuild.core.support.general.CalcFormulaSupport;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单/视图 功能扩展
 *
 * @author devezhao
 * @since 2019/05/20
 */
@Slf4j
@RestController
@RequestMapping("/app/entity/extras/")
public class ModelExtrasController extends BaseController {

    // 获取表单回填数据
    @RequestMapping("fillin-value")
    public JSON getFillinValue(@EntityParam Entity entity, @IdParam(name = "source") ID sourceRecord,
                               HttpServletRequest request) {
        String field = getParameterNotNull(request, "field");
        Field useField = entity.getField(field);
        JSONObject formData40 = (JSONObject) ServletUtils.getRequestJson(request);

        return AutoFillinManager.instance.getFillinValue(useField, sourceRecord, formData40);
    }

    // 记录转换
    @PostMapping("transform39")
    public RespBody transform39(HttpServletRequest request) {
        final JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
        final ID transid = ID.valueOf(post.getString("transid"));

        Object sourceRecordAny = post.get("sourceRecord");
        if (sourceRecordAny instanceof JSONArray) {
            if (((JSONArray) sourceRecordAny).size() == 1) {
                sourceRecordAny = ((JSONArray) sourceRecordAny).get(0);
            }
            // 多个
            else {
                return this.transform39Muilt(transid, (JSONArray) sourceRecordAny);
            }
        }
        // 单个
        ID sourceRecord = ID.valueOf(sourceRecordAny.toString());

        RecordTransfomer39 transfomer39 = RecordTransfomer43.create(transid);
        if (!transfomer39.checkFilter(sourceRecord)) {
            return RespBody.error(Language.L("当前记录不符合转换条件"), 400);
        }

        ID mainRecord = ID.isId(post.getString("mainRecord")) ? ID.valueOf(post.getString("mainRecord")) : null;
        ID existsRecord = ID.isId(post.getString("existsRecord")) ? ID.valueOf(post.getString("existsRecord")) : null;

        // v4.2 弱校验
        final ID weakMode = getIdParameter(request, "weakMode");
        if (weakMode != null) RbvFunction.call().setWeakMode(weakMode);

        try {
            Object res;
            if (post.getBooleanValue("preview")) {
                res = transfomer39.preview(sourceRecord, mainRecord, existsRecord);
            } else {
                res = transfomer39.transform(sourceRecord, mainRecord, existsRecord);
            }
            return RespBody.ok(res);

        } catch (Exception ex) {
            String errorMsg = CommonsUtils.getRootMessage(ex);
            log.warn(">>>>> {} : {}", sourceRecord, errorMsg);

            // v4.2 弱校验
            if (ex instanceof DataValidateException && ((DataValidateException) ex).isWeakMode()) {
                errorMsg = ex.getLocalizedMessage() + "$$$$" + ((DataValidateException) ex).getWeakModeTriggerId();
                return RespBody.error(errorMsg, DefinedException.CODE_WEAK_VALIDATE);
            }

            if (ex instanceof RepeatedRecordsException) {
                errorMsg = Language.L("存在重复记录");
            }
            return RespBody.errorl("记录转换失败 (%s)", errorMsg);

        } finally {
            if (weakMode != null) RbvFunction.call().getWeakMode(true);
        }
    }

    // 批量转换
    private RespBody transform39Muilt(ID transid, JSONArray sourceRecords) {
        List<ID> newIds = new ArrayList<>();
        for (Object o : sourceRecords) {
            RecordTransfomer39 transfomer39 = RecordTransfomer43.create(transid);
            ID sourceRecord = ID.valueOf((String) o);
            if (!transfomer39.checkFilter(sourceRecord)) continue;

            try {
                ID newId = transfomer39.transform(sourceRecord, null, null);
                newIds.add(newId);
            } catch (Exception ex) {
                log.warn(">>>>> {} : {}", sourceRecord, ex.getLocalizedMessage());
            }
        }
        return RespBody.ok(newIds);
    }

    @GetMapping("record-last-modified")
    public JSONAware fetchRecordLastModified(@IdParam ID id) {
        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        String sql = String.format("select modifiedOn from %s where %s = '%s'",
                entity.getName(), entity.getPrimaryField().getName(), id);
        Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
        if (recordMeta == null) {
            return RespBody.error("NO_EXISTS");
        }

        return JSONUtils.toJSONObject(
                new String[] { "lastModified" },
                new Object[] { ((Date) recordMeta[0]).getTime() });
    }

    @GetMapping("record-meta")
    public JSONAware fetchRecordMeta(@IdParam ID id) {
        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        String sql = "select createdOn,modifiedOn from %s where %s = '%s'";
        if (MetadataHelper.hasPrivilegesField(entity)) {
            sql = sql.replace(",modifiedOn", ",modifiedOn,owningUser");
        }

        sql = String.format(sql, entity.getName(), entity.getPrimaryField().getName(), id);
        Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
        if (recordMeta == null) {
            return RespBody.errorl("记录不存在");
        }

        recordMeta[0] = I18nUtils.formatDate((Date) recordMeta[0]);
        recordMeta[1] = I18nUtils.formatDate((Date) recordMeta[1]);

        String[] owning = null;
        List<String[]> sharingList = null;
        if (recordMeta.length == 3) {
            User user = Application.getUserStore().getUser((ID) recordMeta[2]);
            String dept = user.getOwningDept() == null ? null : user.getOwningDept().getName();
            owning = new String[]{user.getIdentity().toString(), user.getFullName(), dept};

            Object[][] shareTo = Application.createQueryNoFilter(
                    "select shareTo from ShareAccess where belongEntity = ? and recordId = ?")
                    .setParameter(1, entity.getName())
                    .setParameter(2, id)
                    .setLimit(9)  // 最多显示9个
                    .array();
            sharingList = new ArrayList<>();
            for (Object[] st : shareTo) {
                sharingList.add(new String[]{st[0].toString(), UserHelper.getName((ID) st[0])});
            }
        }

        return JSONUtils.toJSONObject(
                new String[] { "createdOn", "modifiedOn", "owningUser", "sharingList" },
                new Object[] { recordMeta[0], recordMeta[1], owning, sharingList });
    }

    @GetMapping("record-history")
    public JSONAware fetchRecordHistory(@IdParam ID id) {
        Object[][] array = Application.createQueryNoFilter(
                "select revisionType,revisionOn,revisionBy,channelWith from RevisionHistory where recordId = ? order by autoId desc")
                .setParameter(1, id)
                .setLimit(100)
                .array();

        for (Object[] o : array) {
            int revType = (int) o[0];
            if (revType == 1) o[0] = Language.L("新建");
            else if (revType == 2) o[0] = Language.L("删除");
            else if (revType == 4) o[0] = Language.L("更新");
            else if (revType == 16) o[0] = Language.L("分配");
            else if (revType == 32) o[0] = Language.L("共享");
            else if (revType == 64) o[0] = Language.L("取消共享");
            else if (revType == 991) o[0] = Language.L("审批通过");
            else if (revType == 992) o[0] = Language.L("审批撤销");
            else if (revType == 993) o[0] = Language.L("恢复");
            else o[0] = Language.L("其他") + String.format(" (%d)", revType);

            o[1] = I18nUtils.formatDate((Date) o[1]);
            o[2] = new Object[] { o[2], UserHelper.getName((ID) o[2]) };
        }

        return JSONUtils.toJSONObjectArray(
                new String[] { "revisionType", "revisionOn", "revisionBy" }, array);
    }

    @GetMapping("check-creates")
    public JSON checkCreates(HttpServletRequest request) {
        ID user = getRequestUser(request);
        boolean isAdmin = UserHelper.isAdmin(user);
        String entity = getParameter(request, "entity", "");

        JSONArray allowed = new JSONArray();
        for (String e : entity.split(",")) {
            if (!MetadataHelper.containsEntity(e)) continue;

            Entity ee = MetadataHelper.getEntity(e);
            if (!MetadataHelper.hasPrivilegesField(ee)) {
                if (isAdmin && MetadataHelper.isBizzEntity(ee)) {
                    // 管理员允许 BIZZ
                } else {
                    continue;
                }
            }

            if (Application.getPrivilegesManager().allow(user, ee.getEntityCode(), BizzPermission.CREATE)) {
                allowed.add(EasyMetaFactory.toJSON(ee));
            }
        }
        return allowed;
    }

    @PostMapping("eval-calc-formula")
    public RespBody evalCalcFormula(@EntityParam Entity entity, HttpServletRequest request) {
        String targetField = getParameterNotNull(request, "field");
        if (!entity.containsField(targetField)) return RespBody.error();

        JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
        Map<String, Object> varsInFormula = new HashMap<>(post.getInnerMap());
        for (Object value : varsInFormula.values()) {
            if (value == null || StringUtils.isBlank(value.toString())) {
                return RespBody.ok();
            }
        }

        Object evalVal = CalcFormulaSupport.evalCalcFormula(entity.getField(targetField), varsInFormula);
        return evalVal == null ? RespBody.ok() : RespBody.ok(evalVal);
    }
}
