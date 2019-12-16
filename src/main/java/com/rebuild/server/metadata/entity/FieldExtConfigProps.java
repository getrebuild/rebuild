/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.metadata.entity;

/**
 * 字段扩展属性常量
 *
 * @author ZHAO
 * @since 2019/12/3
 */
public class FieldExtConfigProps {

    /**
     * 是否允许负数
     */
    public static final String NUMBER_NOTNEGATIVE = "notNegative";
    /**
     * 是否允许负数
     */
    public static final String DECIMAL_NOTNEGATIVE = NUMBER_NOTNEGATIVE;

    /**
     * 日期格式
     */
    public static final String DATE_DATEFORMAT = "dateFormat";
    /**
     * 日期格式
     */
    public static final String DATETIME_DATEFORMAT = DATE_DATEFORMAT;

    /**
     * 允许上传数量
     */
    public static final String FILE_UPLOADNUMBER = "uploadNumber";
    /**
     * 允许上传数量
     */
    public static final String IMAGE_UPLOADNUMBER = FILE_UPLOADNUMBER;

    /**
     * 自动编号规则
     */
    public static final String SERIES_SERIESFORMAT = "seriesFormat";
    /**
     * 自动编号归零方式
     */
    public static final String SERIES_SERIESZERO = "seriesZero";

    /**
     * 使用哪个分类数据
     */
    public static final String CLASSIFICATION_USECLASSIFICATION = "useClassification";

    /**
     * 使用哪个状态类
     */
    public static final String STATE_STATECLASS = "stateClass";
}
