/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.rbv.data.Html5ReportGenerator;
import com.rebuild.rbv.data.WordReportGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 2025/3/8
 */
@ConditionalOnMissingClass("com.rebuild.Rbv")
@Component
@Slf4j
public class RbvFunction {

    protected RbvFunction() {}

    public static RbvFunction call() {
        return Application.getBean(RbvFunction.class);
    }

    // -- TRIGGER

    public void setWeakMode(ID id) {
        log.warn("No RbvFunction : setWeakMode");
    }

    public ID getWeakMode(boolean once) {
        log.warn("No RbvFunction : getWeakMode");
        return null;
    }

    public Set<Object> sendToWxwork(ActionContext actionContext, OperatingContext operatingContext) {
        log.warn("No RbvFunction : sendToWxwork");
        return null;
    }

    public Set<Object> sendToDingtalk(ActionContext actionContext, OperatingContext operatingContext) {
        log.warn("No RbvFunction : sendToDingtalk");
        return null;
    }

    public Set<Object> sendToFeishu(ActionContext actionContext, OperatingContext operatingContext) {
        log.warn("No RbvFunction : sendToFeishu");
        return null;
    }

    // -- SOP

    public void onApproveManual(Record approvalRecord) {
        log.debug("No RbvFunction : onApproveManual");
    }

    // -- CONF

    public File dumpRebuildConf(int a, boolean b) {
        log.debug("No RbvFunction : dumpRebuildConf");
        return null;
    }

    public void checkSchemas() {
        log.debug("No RbvFunction : checkSchemas");
    }

    public boolean checkIp(String ip) {
        log.debug("No RbvFunction : checkIp");
        return true;
    }
    public boolean checkTime(Date date) {
        log.debug("No RbvFunction : checkTime");
        return true;
    }

    // -- TRANS

    public Record restRecord(Record record) {
        log.debug("No RbvFunction : restRecord");
        return record;
    }

    // -- USER

    public boolean unbindExternalUser(ID userId, String appId) {
        log.debug("No RbvFunction : unbindExternalUser");
        return false;
    }

    // -- REPORTS

    public Map<String, String> transformVarsHtml5(String templateContent, String entityName) {
        log.debug("No RbvFunction : transformVarsHtml5");
        return null;
    }

    public Map<String, String> transformVarsWord(File template, String entityName) {
        log.debug("No RbvFunction : transformVarsWord");
        return null;
    }

    public Html5ReportGenerator createHtml5(String templateContent, ID recordId) {
        log.debug("No RbvFunction : createHtml5");
        return null;
    }

    public Html5ReportGenerator createHtml5(ID reportId, ID[] recordIds, Boolean noPagebreak) {
        log.debug("No RbvFunction : createHtml5#2");
        return null;
    }

    public WordReportGenerator createWord(File templateFile, ID recordId) {
        log.debug("No RbvFunction : createWord");
        return null;
    }

    public WordReportGenerator createWord(ID reportId, ID[] recordIds) {
        log.debug("No RbvFunction : createWord#2");
        return null;
    }
}
