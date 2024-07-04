/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.spring.FastJsonJsonView;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.Initialization;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.MarkdownUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.exceptions.TemplateInputException;
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
@Slf4j
@Component
public class RebuildWebConfigurer implements WebMvcConfigurer, ErrorViewResolver, Initialization {

    @Resource(name = "thymeleafViewResolver")
    private ThymeleafViewResolver thymeleafViewResolver;

    @Getter
    private static String pageFooterHtml;

    @Override
    public void init() {
        Assert.notNull(thymeleafViewResolver, "[thymeleafViewResolver] is null");

        // ServletContext 共享变量
        thymeleafViewResolver.addStaticVariable(WebConstants.ENV, Application.devMode() ? "dev" : "production");
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

        String pageFooter = RebuildConfiguration.get(ConfigurationItem.PageFooter);
        if (StringUtils.isBlank(pageFooter)) {
            pageFooterHtml = null;
        } else {
            pageFooterHtml = MarkdownUtils.render(pageFooter, true, false);
        }
        thymeleafViewResolver.addStaticVariable(WebConstants.PAGE_FOOTER, pageFooterHtml);

        setStaticVariable(ConfigurationItem.PortalOfficePreviewUrl);
        setStaticVariable(ConfigurationItem.PortalBaiduMapAk);
        setStaticVariable(ConfigurationItem.PortalUploadMaxSize);
        setStaticVariable(ConfigurationItem.AppBuild);
        setStaticVariable(ConfigurationItem.PageMourningMode);

        // 清理缓存
        thymeleafViewResolver.clearCache();
    }

    private void setStaticVariable(ConfigurationItem item) {
        String value = RebuildConfiguration.get(item);
        if (StringUtils.isBlank(value)) {
            thymeleafViewResolver.addStaticVariable(item.name(), null);
        } else {
            thymeleafViewResolver.addStaticVariable(item.name(), value);
        }
    }

    @Override
    public void configureViewResolvers(@NotNull ViewResolverRegistry registry) {
        WebMvcConfigurer.super.configureViewResolvers(registry);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        converters.add(0, new FastJsonHttpMessageConverter4());
        converters.add(0, new FastJsonHttpMessageConverter());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RebuildWebInterceptor())
                .excludePathPatterns("/gw/api/**")
                .excludePathPatterns("/language/**")
                .excludePathPatterns("/assets/**")
                .excludePathPatterns("/h5app/**")
                .excludePathPatterns("/*.txt");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/h5app/**")
                .addResourceLocations("classpath:/public/h5app/");
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
     * @see ControllerRespBodyAdvice
     */
    private ModelAndView createError(HttpServletRequest request, Exception ex, HttpStatus status, Map<String, Object> model) {
        // IGNORED
        if (request.getRequestURI().contains("/assets/")) return null;

        ModelAndView error;
        if (ServletUtils.isAjaxRequest(request) || request.getRequestURI().contains("/filex/upload")) {
            error = new ModelAndView(new FastJsonJsonView());
        } else {
            error = new ModelAndView("/error/error");
            error.getModelMap().put(WebConstants.$BUNDLE, AppUtils.getReuqestBundle(request));
        }

        int errorCode = status.value();
        String errorMsg = ex == null ? null : KnownExceptionConverter.convert2ErrorMsg(ex);
        if (errorMsg == null) errorMsg = getErrorMessage(request, ex);

        String errorLog = "\n++ EXECUTE REQUEST ERROR(s) TRACE +++++++++++++++++++++++++++++++++++++++++++++" +
                "\nUser    : " + ObjectUtils.defaultIfNull(AppUtils.getRequestUser(request), "-") +
                "\nIP      : " + ServletUtils.getRemoteAddr(request) +
                "\nUA      : " + StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-") +
                "\nURL(s)  : " + getRequestUrls(request) +
                "\nMessage : " + errorMsg + (model != null ? (" " + model) : "") +
                "\n";

        if (ex instanceof DefinedException) {
            errorCode = ((DefinedException) ex).getErrorCode();
            log.warn(errorLog, Application.devMode() ? ex : null);
        } else if (ex != null && ex.getClass().getSimpleName().equals("ClientAbortException")) {
            log.warn("ClientAbortException : " + errorMsg, Application.devMode() ? ex : null);
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            log.warn("HttpRequestMethodNotSupportedException : " + getRequestUrls(request), Application.devMode() ? ex : null);
        } else {
            log.error(errorLog, ex);

            if (ex != null && ThrowableUtils.getRootCause(ex) instanceof TemplateInputException
                    && errorMsg.contains("Error resolving template")) {
                errorMsg = Language.L("访问的页面/资源不存在");
            }
        }

        if (StringUtils.isBlank(errorMsg)) errorMsg = Language.L("系统繁忙，请稍后重试");

        error.getModel().put("error_code", errorCode);
        error.getModel().put("error_msg", CommonsUtils.sanitizeHtml(errorMsg));

        if (ex != null && Application.devMode()) {
            error.getModel().put("error_stack", ThrowableUtils.extractStackTrace(ex));
        }

        return error;
    }

    /**
     * 获取请求+引用地址
     *
     * @param request
     * @return
     */
    protected static String getRequestUrls(HttpServletRequest request) {
        String reqUrl = request.getRequestURL().toString();
        if (StringUtils.isNotBlank(request.getQueryString())) reqUrl += "?" + request.getQueryString();
        String refUrl = ServletUtils.getReferer(request);

        if (refUrl == null) return reqUrl;
        else if (reqUrl.endsWith("/error")) return refUrl;
        else return reqUrl + " via " + refUrl;
    }

    /**
     * 获取后台抛出的错误消息
     *
     * @param request
     * @param exception
     * @return
     * @see KnownExceptionConverter
     */
    protected static String getErrorMessage(HttpServletRequest request, Throwable exception) {
        if (exception == null && request != null) {
            String errorMsg = (String) request.getAttribute(ServletUtils.ERROR_MESSAGE);
            if (StringUtils.isNotBlank(errorMsg)) {
                return errorMsg;
            }

            Integer code = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
            if (code != null && code == 404) {
                return Language.L("访问的页面/资源不存在");
            } else if (code != null && code == 403) {
                return Language.L("权限不足，访问被阻止");
            } else if (code != null && code == 401) {
                return Language.L("未授权访问");
            }

            exception = (Throwable) request.getAttribute(ServletUtils.ERROR_EXCEPTION);
        }

        // 已知异常
        if (exception != null) {
            Throwable known = ThrowableUtils.getRootCause(exception);
            if (known instanceof AccessDeniedException) {
                return Language.L("权限不足，访问被阻止");
            }
        }

        if (exception == null) {
            return Language.L("系统繁忙，请稍后重试");
        } else {
            exception = ThrowableUtils.getRootCause(exception);
            String errorMsg = exception.getLocalizedMessage();
            if (StringUtils.isBlank(errorMsg)) errorMsg = Language.L("系统繁忙，请稍后重试");
            return errorMsg;
        }
    }
}
