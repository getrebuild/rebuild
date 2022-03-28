/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

/**
 * 字段值计算方式
 *
 * @author devezhao
 * @since 12/15/2018
 */
public enum FormatCalc {

    // 数字字段
    SUM("求和"), AVG("平均值"), MAX("最大数"), MIN("最小数"), COUNT("计数"),
    COUNT2("去重计数"),

    // 日期时间字段
    Y("年"), M("月"), D("日"), H("时"),
    Q("季"),
    I("时分"),

    // 分类字段
    L1("一级"), L2("二级"), L3("三级"), L4("四级"),

    NONE("无"),

    ;

    private final String label;

    FormatCalc(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
