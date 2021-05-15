/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.i18n.Language;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link cn.devezhao.persist4j.engine.ID} 参数解析器
 *
 * @author devezhao
 * @since 2020/11/3
 * @see IdParam
 */
public class IdParamMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(IdParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        IdParam param = parameter.getParameterAnnotation(IdParam.class);
        Assert.notNull(param, "[IdParam] cannot be null");

        String value = webRequest.getParameter(param.name());
        ID idValue = ID.isId(value) ? ID.valueOf(value) : null;

        if (param.required() && idValue == null) {
            throw new InvalidParameterException(Language.$L("无效请求参数 (%s=%s)", param.name(), value));
        }
        return idValue;
    }
}
