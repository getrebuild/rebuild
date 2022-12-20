/*!
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link ID} 数组参数解析器
 *
 * @author devezhao
 * @since 2022/12/20
 * @see IdArrayParam
 */
public class IdArrayParamMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(IdParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        IdArrayParam param = parameter.getParameterAnnotation(IdArrayParam.class);
        Assert.notNull(param, "[IdArrayParam] cannot be null");

        String value = webRequest.getParameter(param.name());

        Set<ID> idArray = new LinkedHashSet<>();
        if (value != null) {
            for (String s : value.split("[,;|]")) {
                if (ID.isId(s)) idArray.add(ID.valueOf(s));
            }
        }

        if (param.required() && idArray.isEmpty()) {
            throw new InvalidParameterException(Language.L("无效请求参数 (%s=%s)", param.name(), value));
        }

        return idArray.toArray(new ID[0]);
    }
}
