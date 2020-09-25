/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.support.spring.FastJsonJsonView;
import com.rebuild.core.Application;
import com.rebuild.core.Initialization;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
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
    public void configureViewResolvers(ViewResolverRegistry registry) {
        WebMvcConfigurer.super.configureViewResolvers(registry);
    }

    @Override
    public void init() {
        Assert.notNull(thymeleafViewResolver, "[thymeleafViewResolver] is null");

        // ServletContext 共享变量
        thymeleafViewResolver.addStaticVariable(WebConstants.ENV, Application.devMode() ? "dev" : "prodution");
        thymeleafViewResolver.addStaticVariable(WebConstants.COMMERCIAL, License.getCommercialType());
        thymeleafViewResolver.addStaticVariable(WebConstants.BASE_URL, AppUtils.getContextPath());
        thymeleafViewResolver.addStaticVariable(WebConstants.APP_NAME, RebuildConfiguration.get(ConfigurationItem.AppName));
        thymeleafViewResolver.addStaticVariable(WebConstants.STORAGE_URL, RebuildConfiguration.get(ConfigurationItem.StorageURL));
        thymeleafViewResolver.addStaticVariable(WebConstants.FILE_SHARABLE, RebuildConfiguration.get(ConfigurationItem.FileSharable));
        thymeleafViewResolver.addStaticVariable(WebConstants.MARK_WATERMARK, RebuildConfiguration.get(ConfigurationItem.MarkWatermark));
    }

    /**
     * 请求拦截
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RebuildWebInterceptor())
                .excludePathPatterns("/gw/**")
                .excludePathPatterns("/language/**")
                .excludePathPatterns("/assets/**")
                .excludePathPatterns("/*.txt");
    }

    /**
     * 异常处理
     *
     * @param resolvers
     */
    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add((request, response, handler, ex)
                -> createError(request, ex, HttpStatus.INTERNAL_SERVER_ERROR, null));
    }

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        return createError(request, null, status, model);
    }

    /**
     * @param request
     * @param ex
     * @return
     * @see AppUtils#isHtmlRequest(HttpServletRequest)
     */
    private ModelAndView createError(HttpServletRequest request, Exception ex, HttpStatus status, Map<String, Object> model) {
        // IGNORED
        if (request.getRequestURI().contains("/assets/")) return null;

        ModelAndView error;
        if (AppUtils.isHtmlRequest(request)) {
            error = new ModelAndView("/error/error");
            error.getModelMap().put("bundle", AppUtils.getReuqestBundle(request));
        } else {
            error = new ModelAndView(new FastJsonJsonView());
        }

        int errorCode = status.value();
        String errorMsg = AppUtils.getErrorMessage(request, ex);

        if (ex instanceof DataSpecificationException) {
            errorCode = ((DataSpecificationException) ex).getErrorCode();
        } else {
            String errorLog = "\n++ EXECUTE REQUEST ERROR(s) TRACE +++++++++++++++++++++++++++++++++++++++++++++" +
                    "\nUser    : " + ObjectUtils.defaultIfNull(AppUtils.getRequestUser(request), "-") +
                    "\nIP      : " + ServletUtils.getRemoteAddr(request) +
                    "\nUA      : " + StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-") +
                    "\nURL(s)  : " + request.getRequestURL() + " [ " + StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "-") + " ]" +
                    "\nMessage : " + errorMsg + (model != null ? (" " + model.toString()) : "");
            LOG.error(errorLog, ex);
        }

        error.getModel().put("error_code", errorCode);
        error.getModel().put("error_msg", errorMsg);
        return error;
    }
}
