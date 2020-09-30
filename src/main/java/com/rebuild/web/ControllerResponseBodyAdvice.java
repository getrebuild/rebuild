/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.api.RespBody;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * @author devezhao
 * @since 2020/9/30
 *
 * @see RespBody
 * @see RebuildWebConfigurer#configureHandlerExceptionResolvers(List)
 * @see RebuildWebConfigurer#configureMessageConverters(List)
 */
@ControllerAdvice
public class ControllerResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 强转为 JSON
        // Controller 方法返回 `null` `String` 时 mediaType=TEXT_PLAIN
        if (mediaType == MediaType.TEXT_PLAIN) {
            serverHttpResponse.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }

        if (o instanceof RespBody) {
            return ((RespBody) o).toJSON();
        } else {
            return RespBody.ok(o).toJSON();
        }
    }
}
