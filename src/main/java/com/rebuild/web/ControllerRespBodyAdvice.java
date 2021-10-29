/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.api.RespBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/9/30
 *
 * @see RespBody
 * @see RebuildWebConfigurer#configureMessageConverters(List)
 */
@Slf4j
@ControllerAdvice
public class ControllerRespBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        HttpServletRequest request = ((ServletServerHttpRequest) serverHttpRequest).getServletRequest();
        HttpServletResponse response = ((ServletServerHttpResponse) serverHttpResponse).getServletResponse();

        // #supports
        // Controller send status of error
        if (response.getStatus() != 200) {
            if (o instanceof Map) {
                String path404 = (String) ((Map<?, ?>) o).get("path");
                if (path404 != null && path404.endsWith(".map")) {
                    return o;
                }
            }

            log.warn("Response Error Status : {}", o);
            return o;
        }

        // 强转为 JSON Herader
        // @ResponseBody 方法返回 `null` `String` 时 mediaType=TEXT_PLAIN
        if (MediaType.TEXT_PLAIN.equals(mediaType)) {
            // @Deprecated
            log.warn("Force conversion TEXT_PLAIN to APPLICATION_JSON : {}", request.getRequestURI());
            response.addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        }

        if (o instanceof RespBody) {
            return ((RespBody) o).toJSON();
        } else {
            return RespBody.ok(o).toJSON();
        }
    }
}
