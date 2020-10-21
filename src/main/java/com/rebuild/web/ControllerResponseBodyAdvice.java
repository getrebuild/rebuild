/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
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

    private static final Logger LOG = LoggerFactory.getLogger(ControllerResponseBodyAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 强转为 JSON Herader
        // Controller 方法返回 `null` `String` 时 mediaType=TEXT_PLAIN
        if (MediaType.TEXT_PLAIN.equals(mediaType)) {
            serverHttpResponse.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }

        if (o instanceof RespBody) {
            return ((RespBody) o).toJSON();

        } else {
            // Controller send status of error (xhr)
            int statusCode = ((ServletServerHttpResponse) serverHttpResponse).getServletResponse().getStatus();

            if (statusCode == HttpStatus.OK.value()) {
                return RespBody.ok(o).toJSON();
            } else {
                LOG.error("Response Error Status : " + o);
                return RespBody.error(statusCode);
            }
        }
    }
}
