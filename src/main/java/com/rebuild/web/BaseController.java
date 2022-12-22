/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.Controller;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base Controller.
 * 请求方法可返回 {@link com.rebuild.api.RespBody} 或 {@link JSON}
 *
 * @author Zixin (RB)
 * @since 05/21/2017
 *
 * @see com.rebuild.api.RespBody
 * @see ControllerRespBodyAdvice
 */
public abstract class BaseController extends Controller {

    /**
     * @param request
     * @return
     * @see AppUtils#getRequestUser(HttpServletRequest)
     */
    protected ID getRequestUser(HttpServletRequest request) {
        ID user = AppUtils.getRequestUser(request);
        if (user == null) {
            throw new InvalidParameterException(Language.L("无效请求用户"));
        }
        return user;
    }

    /**
     * @param response
     * @param data
     */
    protected void writeSuccess(HttpServletResponse response, Object data) {
        writeJSON(response, formatSuccess(data));
    }

    /**
     * @param response
     * @param message
     */
    protected void writeFailure(HttpServletResponse response, String message) {
        writeJSON(response, formatFailure(message));
    }

    /**
     * @see com.rebuild.api.RespBody
     * @see ControllerRespBodyAdvice
     */
    private void writeJSON(HttpServletResponse response, Object aJson) {
        String aJsonString;
        if (aJson instanceof String) {
            aJsonString = (String) aJson;
        } else {
            aJsonString = JSON.toJSONString(aJson);
        }
        ServletUtils.writeJson(response, aJsonString);
    }

    /**
     * @param request
     * @param name
     * @return
     */
    protected String getParameter(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    /**
     * @param request
     * @param name
     * @param defaultValue
     * @return
     */
    protected String getParameter(HttpServletRequest request, String name, String defaultValue) {
        return StringUtils.defaultIfBlank(getParameter(request, name), defaultValue);
    }

    /**
     * @param request
     * @param name
     * @return
     */
    protected String getParameterNotNull(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (StringUtils.isEmpty(v)) {
            throw new InvalidParameterException(Language.L("无效请求参数 (%s=%s)", name, v));
        }
        return v;
    }

    /**
     * @param request
     * @param name
     * @return
     */
    protected Integer getIntParameter(HttpServletRequest request, String name) {
        return getIntParameter(request, name, null);
    }

    /**
     * @param request
     * @param name
     * @param defaultValue
     * @return
     */
    protected Integer getIntParameter(HttpServletRequest request, String name, Integer defaultValue) {
        String v = request.getParameter(name);
        if (StringUtils.isBlank(v)) return defaultValue;

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * @param request
     * @param name
     * @return
     * @see BooleanUtils#toBoolean(String)
     */
    protected boolean getBoolParameter(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        return v != null && BooleanUtils.toBoolean(v);
    }

    /**
     * @param request
     * @param name
     * @param defaultValue
     * @return
     */
    protected boolean getBoolParameter(HttpServletRequest request, String name, boolean defaultValue) {
        String v = request.getParameter(name);
        return v == null ? defaultValue : BooleanUtils.toBoolean(v);
    }

    /**
     * @param request
     * @param name
     * @return
     */
    protected ID getIdParameter(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        return ID.isId(v) ? ID.valueOf(v) : null;
    }

    /**
     * @param request
     * @param name
     * @return
     */
    protected ID getIdParameterNotNull(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (ID.isId(v)) return ID.valueOf(v);
        throw new InvalidParameterException(Language.L("无效请求参数 (%s=%s)", name, v));
    }

    /**
     * @param request
     * @param name
     * @return
     */
    protected ID[] getIdArrayParameter(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (v == null) return ID.EMPTY_ID_ARRAY;

        Set<ID> set = new LinkedHashSet<>();
        for (String id : v.split("[,;|]")) {
            if (ID.isId(id)) set.add(ID.valueOf(id));
        }
        return set.toArray(new ID[0]);
    }

    /**
     * @param view
     * @return
     */
    protected ModelAndView createModelAndView(String view) {
        return new ModelAndView(view);
    }

    /**
     * @param view
     * @param modelMap
     * @return
     */
    protected ModelAndView createModelAndView(String view, Map<String, Object> modelMap) {
        ModelAndView mv = createModelAndView(view);
        if (modelMap != null && !modelMap.isEmpty()) {
            mv.getModelMap().putAll(modelMap);
        }
        return mv;
    }
}
