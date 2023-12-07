/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

/**
 * 字段扩展属性常量
 *
 * @author ZHAO
 * @since 2019/12/3
 */
public class EasyFieldConfigProps {

    /**
     * 信息脱敏
     */
    public static final String ADV_DESENSITIZED = "advDesensitized";

    /**
     * 正则表达式
     */
    public static final String ADV_PATTERN = "advPattern";

    /**
     * 扫码
     */
    public static final String TEXT_SCANCODE = "textScanCode";
    /**
     * 常用值
     */
    public static final String TEXT_COMMON = "textCommon";

    /**
     * 是否允许负数
     */
    public static final String NUMBER_NOTNEGATIVE = "notNegative";
    /**
     * 格式
     */
    public static final String NUMBER_FORMAT = "numberFormat";
    /**
     * 表单公式
     */
    public static final String NUMBER_CALCFORMULA = "calcFormula";

    /**
     * 是否允许负数
     */
    public static final String DECIMAL_NOTNEGATIVE = NUMBER_NOTNEGATIVE;
    /**
     * 格式（小数长度）
     */
    public static final String DECIMAL_FORMAT = "decimalFormat";
    /**
     * 格式
     */
    public static final String DECIMAL_TYPE = "decimalType";
    /**
     * 表单公式
     */
    public static final String DECIMAL_CALCFORMULA = NUMBER_CALCFORMULA;

    /**
     * 日期格式
     */
    public static final String DATE_FORMAT = "dateFormat";
    /**
     * 表单公式
     */
    public static final String DATE_CALCFORMULA = NUMBER_CALCFORMULA;

    /**
     * 日期格式
     */
    public static final String DATETIME_FORMAT = "datetimeFormat";
    /**
     * 表单公式
     */
    public static final String DATETIME_CALCFORMULA = NUMBER_CALCFORMULA;

    /**
     * 时间格式
     */
    public static final String TIME_FORMAT = "timeFormat";

    /**
     * 允许上传数量 1,9
     */
    public static final String FILE_UPLOADNUMBER = "uploadNumber";
    /**
     * 允许上传文件类型
     */
    public static final String FILE_SUFFIX = "fileSuffix";

    /**
     * 允许上传数量
     */
    public static final String IMAGE_UPLOADNUMBER = FILE_UPLOADNUMBER;

    /**
     * 图片获取方式（仅H5）
     */
    public static final String IMAGE_CAPTURE = "imageCapture";

    /**
     * 自动编号规则
     */
    public static final String SERIES_FORMAT = "seriesFormat";
    /**
     * 自动编号归零方式
     */
    public static final String SERIES_ZERO = "seriesZero";

    /**
     * 使用哪个分类数据
     */
    public static final String CLASSIFICATION_USE = "classification";

    /**
     * 分类数据等级
     */
    public static final String CLASSIFICATION_LEVEL = "classificationLevel";

    /**
     * 使用哪个状态类
     */
    public static final String STATE_CLASS = "stateClass";

    /**
     * 引用字段数据过滤
     */
    public static final String REFERENCE_DATAFILTER = "referenceDataFilter";
    /**
     * 父级级联字段
     */
    public static final String REFERENCE_CASCADINGFIELD = "referenceCascadingField";
    /**
     * 快速新建
     */
    public static final String REFERENCE_QUICKNEW = "referenceQuickNew";
    /**
     * 引用字段扫码
     */
    public static final String REFERENCE_SCANCODE = TEXT_SCANCODE;

    /**
     * @see #REFERENCE_DATAFILTER
     */
    public static final String N2NREFERENCE_DATAFILTER = REFERENCE_DATAFILTER;
    /**
     * @see #REFERENCE_CASCADINGFIELD
     */
    public static final String N2NREFERENCE_CASCADINGFIELD = REFERENCE_CASCADINGFIELD;
    /**
     * @see #REFERENCE_QUICKNEW
     */
    public static final String N2NREFERENCE_QUICKNEW = REFERENCE_QUICKNEW;

    /**
     * 任意引用字段可引用实体
     */
    public static final String ANYREFERENCE_ENTITIES = "anyreferenceEntities";

    /**
     * 多行文本使用 MD 编辑器
     */
    public static final String NTEXT_USEMDEDIT = "useMdedit";

    /**
     * 视图直接显示地图
     */
    public static final String LOCATION_MAPONVIEW = "locationMapOnView";
    /**
     * 自动定位
     */
    public static final String LOCATION_AUTOLOCATION = "locationAutoLocation";

    /**
     * 标签列表
     */
    public static final String TAG_LIST = "tagList";
    /**
     * 标签最大数量
     */
    public static final String TAG_MAXSELECT = "tagMaxSelect";

}
