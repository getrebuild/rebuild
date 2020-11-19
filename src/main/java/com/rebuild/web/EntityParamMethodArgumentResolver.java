/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link cn.devezhao.persist4j.Entity} 参数解析器
 *
 * @author devezhao
 * @since 2020/11/3
 * @see EntityParam
 */
public class EntityParamMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(EntityParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        EntityParam param = parameter.getParameterAnnotation(EntityParam.class);
        Assert.notNull(param, "[EntityParam] cannot be null");

        String value = webRequest.getParameter(param.name());
        if (StringUtils.isBlank(value)) {
            if (param.required()) {
                throw new InvalidParameterException(
                        Language.L("BadRequestParams") + String.format(" [ %s=%s ]", param.name(), value));
            } else {
                return null;
            }
        }

        if (MetadataHelper.containsEntity(value)) {
            return MetadataHelper.getEntity(value);
        }
        throw new InvalidParameterException("");
    }
}
