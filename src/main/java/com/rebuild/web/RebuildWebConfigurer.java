/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;
import com.alibaba.fastjson.support.spring.FastJsonJsonView;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.Initialization;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * MVC 配置
 *
 * @author devezhao
 * @since 2020/8/26
 */
@Component
public class RebuildWebConfigurer implements WebMvcConfigurer, ErrorViewResolver, Initialization {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildWebConfigurer.class);

    @Resource(name = "thymeleafViewResolver")
    private ThymeleafViewResolver thymeleafViewResolver;

    @Override
    public void init() {
        Assert.notNull(thymeleafViewResolver, "[thymeleafViewResolver] is null");

        // ServletContext 共享变量
        thymeleafViewResolver.addStaticVariable(WebConstants.ENV, Application.devMode() ? "dev" : "prodution");
        thymeleafViewResolver.addStaticVariable(WebConstants.COMMERCIAL, License.getCommercialType());
        thymeleafViewResolver.addStaticVariable(WebConstants.BASE_URL, AppUtils.getContextPath());
        thymeleafViewResolver.addStaticVariable(WebConstants.APP_NAME, RebuildConfiguration.get(ConfigurationItem.AppName));
        if (QiniuCloud.instance().available()) {
            thymeleafViewResolver.addStaticVariable(WebConstants.STORAGE_URL, RebuildConfiguration.get(ConfigurationItem.StorageURL));
        } else {
            thymeleafViewResolver.addStaticVariable(WebConstants.STORAGE_URL, StringUtils.EMPTY);
        }
        thymeleafViewResolver.addStaticVariable(WebConstants.FILE_SHARABLE, RebuildConfiguration.get(ConfigurationItem.FileSharable));
        thymeleafViewResolver.addStaticVariable(WebConstants.MARK_WATERMARK, RebuildConfiguration.get(ConfigurationItem.MarkWatermark));

        // 清理缓存
        thymeleafViewResolver.clearCache();
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        WebMvcConfigurer.super.configureViewResolvers(registry);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new FastJsonHttpMessageConverter4());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RebuildWebInterceptor())
                .excludePathPatterns("/gw/api/**")
                .excludePathPatterns("/language/**")
                .excludePathPatterns("/assets/**")
                .excludePathPatterns("/*.txt");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // URL 参数
        resolvers.add(new IdParamMethodArgumentResolver());
        resolvers.add(new EntityParamMethodArgumentResolver());
    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add((request, response, handler, ex)
                -> createError(request, ex, HttpStatus.INTERNAL_SERVER_ERROR, null));
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        return createError(request, (Exception) request.getAttribute(ServletUtils.ERROR_EXCEPTION), status, model);
    }

    /**
     * @param request
     * @param ex
     * @return
     */
    private ModelAndView createError(HttpServletRequest request, Exception ex, HttpStatus status, Map<String, Object> model) {
        // IGNORED
        if (request.getRequestURI().contains("/assets/")) return null;

        ModelAndView error;
        if (ServletUtils.isAjaxRequest(request)) {
            error = new ModelAndView(new FastJsonJsonView());
        } else {
            error = new ModelAndView("/error/error");
            error.getModelMap().put(WebConstants.$BUNDLE, AppUtils.getReuqestBundle(request));
        }

        int errorCode = status.value();
        String errorMsg = AppUtils.getErrorMessage(request, ex);

        String errorLog = "\n++ EXECUTE REQUEST ERROR(s) TRACE +++++++++++++++++++++++++++++++++++++++++++++" +
                "\nUser    : " + ObjectUtils.defaultIfNull(AppUtils.getRequestUser(request), "-") +
                "\nIP      : " + ServletUtils.getRemoteAddr(request) +
                "\nUA      : " + StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-") +
                "\nURL(s)  : " + getRequestUrls(request) +
                "\nMessage : " + errorMsg + (model != null ? (" " + model.toString()) : "") +
                "\n";

        if (ex instanceof DefinedException) {
            errorCode = ((DefinedException) ex).getErrorCode();
            LOG.warn(errorLog, Application.devMode() ? ex : null);
        } else {
            LOG.error(errorLog, ex);
        }

        error.getModel().put("error_code", errorCode);
        error.getModel().put("error_msg", errorMsg);

        if (ex != null && Application.devMode()) {
            error.getModel().put("error_stack", ThrowableUtils.extractStackTrace(ex));
        }

        return error;
    }

    /**
     * 获取请求/引用地址
     *
     * @param request
     * @return
     */
    static String getRequestUrls(HttpServletRequest request) {
        String reqUrl = request.getRequestURL().toString();
        String refUrl = ServletUtils.getReferer(request);

        if (refUrl == null) return reqUrl;
        else if (reqUrl.endsWith("/error")) return refUrl;
        else return reqUrl + " [ " + refUrl + " ]";
    }
}
