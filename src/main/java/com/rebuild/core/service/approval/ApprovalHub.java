/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CacheTemplate;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.SetUser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rebuild.core.privileges.bizz.ZeroEntry.AllowRevokeApproval;

/**
 * 审批处理。此类是作为 ApprovalStepService 的辅助，因为有些逻辑放在 Service 中不合适
 *
 * @author devezhao
 * @since 2019/06/24
 */
@Slf4j
public class ApprovalHub {

    /**
     * @param approvalStepId
     */
    public static void create(ID approvalStepId) {
        Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub);
        r.setID("approvalStepId", approvalStepId);
        Application.getCommonsService().create(r);
    }

    /**
     * @param approvalStepId
     */
    public static void update(ID approvalStepId) {
        Object[] id = Application.createQueryNoFilter(
                "select hubId from RobotApprovalHub where approvalStepId = ?")
                .setParameter(1, approvalStepId)
                .unique();
        Record r = EntityHelper.forUpdate((ID) id[0]);

        // TODO 根据节点状态更新

        Application.getCommonsService().update(r);
    }

    /**
     * @param approvalStepId
     */
    public static void delete(ID approvalStepId) {
        Object[] id = Application.createQueryNoFilter(
                "select hubId from RobotApprovalHub where approvalStepId = ?")
                .setParameter(1, approvalStepId)
                .unique();
        Application.getCommonsService().delete((ID) id[0]);
    }
}
