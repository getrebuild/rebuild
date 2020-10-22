/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rebuild.api.Controller;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Root Controller
 *
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseController extends Controller {

    /**
     * @param request
     * @return
     */
    protected ID getRequestUser(HttpServletRequest request) {
        ID user = AppUtils.getRequestUser(request);
        if (user == null) {
            user = AppUtils.getRequestUserViaRbMobile(request, false);
        }

        if (user == null) {
            throw new InvalidParameterException(getLang(request, "BadRequestUser"));
        }
        return user;
    }

    /**
     * @param request
     * @param key
     * @param phKey
     * @return
     * @see AppUtils#getReuqestBundle(HttpServletRequest)
     */
    protected String getLang(HttpServletRequest request, String key, String... phKey) {
        return AppUtils.getReuqestBundle(request).getLang(key, phKey);
    }

    /**
     * @param request
     * @param key
     * @param phValues
     * @return
     * @see AppUtils#getReuqestBundle(HttpServletRequest)
     */
    protected String formatLang(HttpServletRequest request, String key, Object... phValues) {
        return AppUtils.getReuqestBundle(request).formatLang(key, phValues);
    }

    /**
     * @param response
     */
    protected void writeSuccess(HttpServletResponse response) {
        writeSuccess(response, null);
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
     */
    protected void writeFailure(HttpServletResponse response) {
        writeFailure(response, null);
    }

    /**
     * @param response
     * @param message
     */
    protected void writeFailure(HttpServletResponse response, String message) {
        writeJSON(response, formatFailure(message));
    }

    /**
     * @param response
     * @param aJson
     */
    protected void writeJSON(HttpServletResponse response, Object aJson) {
        if (aJson == null) {
            throw new IllegalArgumentException();
        }

        String aJsonString;
        if (aJson instanceof String) {
            aJsonString = (String) aJson;
        } else {
            // fix: $ref.xxx
            aJsonString = JSON.toJSONString(aJson,
                    SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue);
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
            throw new InvalidParameterException(getLang(request, "BadRequestParams") + " [" + name + "=" + v + "]");
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
            return null;
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
        throw new InvalidParameterException(getLang(request, "BadRequestParams") + " [" + name + "=" + v + "]");
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
