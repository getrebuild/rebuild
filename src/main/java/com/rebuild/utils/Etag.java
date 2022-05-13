/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HTTP ETAG
 *
 * @author devezhao
 * @since 2020/12/29
 * @see org.springframework.web.filter.ShallowEtagHeaderFilter
 */
public class Etag {

    private static final String DIRECTIVE_NO_STORE = "no-store";

    private String responseETag;
    transient private HttpServletResponse response;

    /**
     * @param etag
     * @param response
     */
    public Etag(String etag, HttpServletResponse response) {
        // whether the generated ETag should be weak
        // SPEC: length of W/ + " + 0 + 32bits md5 hash + "
        String responseETag = String.format("W/\"0%s\"", etag);
        response.setHeader(HttpHeaders.ETAG, responseETag);

        this.responseETag = responseETag;
        this.response = response;
    }

    /**
     * 是否无缓存
     *
     * @return
     */
    protected boolean isForceNoCache() {
        String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
        return cacheControl != null && cacheControl.contains(DIRECTIVE_NO_STORE);
    }

    /**
     * 资源是否已修改
     *
     * @param request
     * @param writeStatusIfMatch
     * @return
     */
    protected boolean isMatchEtag(HttpServletRequest request, boolean writeStatusIfMatch) {
        String requestETag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (requestETag != null &&
                ("*".equals(requestETag) || responseETag.equals(requestETag) ||
                        responseETag.replaceFirst("^W/", "").equals(requestETag.replaceFirst("^W/", "")))) {
            if (writeStatusIfMatch) {
                response.setStatus(HttpStatus.NOT_MODIFIED.value());
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否需要写数据（通过 etag 值判断）
     *
     * @param request
     * @return
     */
    public boolean isNeedWrite(HttpServletRequest request) {
        if (isForceNoCache()) return true;
        return !isMatchEtag(request, true);
    }
}
