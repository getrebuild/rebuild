/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

/**
 * @author devezhao
 * @since 2021/7/22
 */
public class EasyEntityConfigProps {

    /**
     * 快速查询字段
     */
    public static final String QUICK_FIELDS = "quickFields";
    /**
     * 实体分类
     */
    public static final String TAGS = "tags";
    /**
     * 明细不允许为空
     */
    public static final String DETAILS_NOTEMPTY = "detailsNotEmpty";
    /**
     * 明细重复判断模式为全部数据（否则为主记录下的）
     */
    public static final String DETAILS_GLOBALREPEAT = "detailsGlobalRepeat";
    /**
     * 明细显示位置
     */
    public static final String DETAILS_SHOWAT2 = "detailsShowAt2";
    /**
     * 明细复制按钮
     */
    public static final String DETAILS_COPIABLE = "detailsCopiable";

    /**
     * 隐藏常用查询面板
     */
    public static final String ADVLIST_HIDE_FILTERS = "advListHideFilters";
    /**
     * 隐藏图表面板
     */
    public static final String ADVLIST_HIDE_CHARTS = "advListHideCharts";
    /**
     * 列表模式
     */
    public static final String ADVLIST_MODE = "advListMode";
    /**
     * 列表分类
     */
    public static final String ADVLIST_SHOWCATEGORY = "advListShowCategory";
    /**
     * 列表查询面板
     */
    public static final String ADVLIST_FILTERPANE = "advListFilterPane";
    /**
     * 列表查询页签
     */
    public static final String ADVLIST_FILTERTABS = "advListFilterTabs";
    /**
     * 重复字段检查模式 (AND/OR)
     */
    public static final String REPEAT_FIELDS_CHECK_MODE = "repeatFieldsCheckMode";
    /**
     * 禁用视图单字段编辑
     */
    public static final String DISABLED_VIEW_EDITABLE = "disabledViewEditable";

    /**
     * 启用记录合并（还需配合权限）
     */
    public static final String ENABLE_RECORD_MERGER = "enableRecordMerger";

    /**
     * 详情模式:字段
     */
    public static final String ADVLIST_MODE2_SHOWFIELDS = "mode2ShowFields";
    /**
     * 卡片模式:字段
     */
    public static final String ADVLIST_MODE3_SHOWFIELDS = "mode3ShowFields";
    /**
     * @see #ADVLIST_HIDE_FILTERS
     */
    public static final String ADVLIST_MODE3_SHOWFILTERS = "mode3ShowFilters";
    /**
     * @see #ADVLIST_SHOWCATEGORY
     */
    public static final String ADVLIST_MODE3_SHOWCATEGORY = "mode3ShowCategory";
}
