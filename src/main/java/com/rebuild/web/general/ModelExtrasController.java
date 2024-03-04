/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
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
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.general.RepeatedRecordsException;
import com.rebuild.core.service.general.transform.RecordTransfomer;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 表单/视图 功能扩展
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
@Slf4j
@RestController
@RequestMapping("/app/entity/extras/")
public class ModelExtrasController extends BaseController {

    // 获取表单回填数据
    @GetMapping("fillin-value")
    public JSON getFillinValue(@EntityParam Entity entity, @IdParam(name = "source") ID sourceRecord,
                               HttpServletRequest request) {
        String field = getParameterNotNull(request, "field");
        Field useField = entity.getField(field);

        return AutoFillinManager.instance.getFillinValue(useField, sourceRecord);
    }

    // 记录转换
    @RequestMapping("transform")
    public RespBody transform(HttpServletRequest request) {
        ID transid = getIdParameterNotNull(request, "transid");
        ID sourceRecord = getIdParameterNotNull(request, "source");
        ID mainid = getIdParameter(request, "mainid");

        RecordTransfomer transfomer = new RecordTransfomer(transid);
        if (!transfomer.checkFilter(sourceRecord)) {
            return RespBody.error(Language.L("当前记录不符合转换条件"), 400);
        }

        try {
            ID theNewId = transfomer.transform(sourceRecord, mainid);
            return RespBody.ok(theNewId);
        } catch (Exception ex) {
            log.warn(">>>>> {}", ex.getLocalizedMessage());

            String error = ex.getLocalizedMessage();
            if (ex instanceof RepeatedRecordsException) {
                error = Language.L("存在重复记录");
            }

            return RespBody.errorl("记录转换失败 (%s)",
                    Objects.toString(error, ex.getClass().getSimpleName()));
        }
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
            else o[0] = Language.L("其他") + String.format(" (%d)", revType);

            o[1] = I18nUtils.formatDate((Date) o[1]);
            o[2] = new Object[] { o[2], UserHelper.getName((ID) o[2]) };
        }

        return JSONUtils.toJSONObjectArray(
                new String[] { "revisionType", "revisionOn", "revisionBy" }, array);
    }

    @GetMapping("check-creates")
    public JSON checkCreates(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String entity = getParameter(request, "entity", "");

        JSONArray allowed = new JSONArray();
        for (String e : entity.split(",")) {
            if (!MetadataHelper.containsEntity(e)) continue;

            EasyEntity easyEntity = EasyMetaFactory.valueOf(e);
            if (!MetadataHelper.hasPrivilegesField(easyEntity.getRawMeta())) continue;

            if (Application.getPrivilegesManager()
                    .allow(user, easyEntity.getRawMeta().getEntityCode(), BizzPermission.CREATE)) {
                allowed.add(easyEntity.toJSON());
            }
        }
        return allowed;
    }

    @PostMapping("eval-calc-formula")
    public RespBody evalCalcFormula(@EntityParam Entity entity, HttpServletRequest request) {
        String targetField = getParameterNotNull(request, "field");
        if (!entity.containsField(targetField)) return RespBody.error();

        JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
        Map<String, Object> varsInFormula = post.getInnerMap();
        for (Object value : varsInFormula.values()) {
            if (value == null || StringUtils.isBlank(value.toString())) {
                return RespBody.ok();
            }
        }

        EasyField easyField = EasyMetaFactory.valueOf(entity.getField(targetField));
        String formula = easyField.getExtraAttr(EasyFieldConfigProps.DATE_CALCFORMULA);

        boolean canCalc = true;
        Set<String> fieldVars = ContentWithFieldVars.matchsVars(formula);
        for (String field : fieldVars) {
            if (!entity.containsField(field)) {
                canCalc = false;
                break;
            }

            Object fieldValue = varsInFormula.get(field);
            if (fieldValue == null) {
                canCalc = false;
                break;
            }

            String fieldValue2 = fieldValue.toString();
            DisplayType dt = EasyMetaFactory.valueOf(entity.getField(field)).getDisplayType();
            if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                fieldValue = CalendarUtils.parse(fieldValue2, CalendarUtils.UTC_DATETIME_FORMAT.substring(0, fieldValue2.length()));
            } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
                fieldValue = EasyDecimal.clearFlaged(fieldValue2);
                if (StringUtils.isNotBlank((String) fieldValue)) {
                    if (dt == DisplayType.NUMBER) fieldValue = ObjectUtils.toLong(fieldValue);
                    else fieldValue = ObjectUtils.toDouble(fieldValue);
                } else {
                    fieldValue = null;
                }
            }

            if (fieldValue == null) {
                canCalc = false;
                break;
            }
            varsInFormula.put(field, fieldValue);
        }
        if (!canCalc) return RespBody.ok();

        formula = formula
                .replace("{", "").replace("}", "")
                .replace("×", "*").replace("÷", "/");

        Object evalVal = AviatorUtils.eval(formula, varsInFormula, true);
        if (evalVal == null) return RespBody.ok();

        DisplayType dt = easyField.getDisplayType();
        if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
            if (evalVal instanceof Date) {
                evalVal = easyField.wrapValue(evalVal);
                return RespBody.ok(evalVal);
            }
        } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            if (evalVal instanceof Number) {
                evalVal = easyField.wrapValue(evalVal);
                return RespBody.ok(evalVal);
            }
        }

        log.warn("Bad eval value `{}` for field : {}", evalVal, easyField.getRawMeta());
        return RespBody.ok();
    }
}
