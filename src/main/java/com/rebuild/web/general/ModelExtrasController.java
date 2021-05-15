/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.general.transform.RecordTransfomer;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * 表单/视图 功能扩展
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/20
 */
@RestController
@RequestMapping("/app/entity/extras/")
public class ModelExtrasController extends BaseController {

    // 获取表单回填数据
    @RequestMapping("fillin-value")
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

        ConfigBean config = TransformManager.instance.getTransformConfig(transid, null);
        Entity targetEntity = MetadataHelper.getEntity(config.getString("target"));

        RecordTransfomer transfomer = new RecordTransfomer(targetEntity, (JSONObject) config.getJSON("config"));
        if (!transfomer.checkFilter(sourceRecord)) {
            return RespBody.error($L("当前记录不符合转换条件"), 400);
        }

        ID newId = transfomer.transform(sourceRecord);
        return RespBody.ok(newId);
    }

    @GetMapping("record-last-modified")
    public JSONAware fetchRecordLastModified(@IdParam ID id) {
        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        String sql = String.format("select modifiedOn from %s where %s = '%s'",
                entity.getName(), entity.getPrimaryField().getName(), id);
        Object[] recordMeta = Application.createQueryNoFilter(sql).unique();
        if (recordMeta == null) {
            return RespBody.errorl("记录不存在");
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
                "select revisionType,revisionOn,revisionBy from RevisionHistory where recordId = ? order by revisionOn desc")
                .setParameter(1, id)
                .setLimit(100)
                .array();

        for (Object[] o : array) {
            int revType = (int) o[0];
            if (revType == 1) o[0] = $L("新建");
            else if (revType == 2) o[0] = $L("删除");
            else if (revType == 4) o[0] = $L("更新");
            else if (revType == 16) o[0] = $L("分派");
            else if (revType == 32) o[0] = $L("共享");
            else if (revType == 64) o[0] = $L("取消共享");
            else o[0] = $L("未知");

            o[1] = I18nUtils.formatDate((Date) o[1]);
            o[2] = new Object[] { o[2], UserHelper.getName((ID) o[2]) };
        }

        return JSONUtils.toJSONObjectArray(
                new String[] { "revisionType", "revisionOn", "revisionBy" }, array);
    }
}
