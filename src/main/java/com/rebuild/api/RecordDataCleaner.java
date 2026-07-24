/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.EasyN2NReference;
import com.rebuild.core.metadata.easymeta.EasyTag;
import com.rebuild.core.service.dataimport.RecordCheckout;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录数据清洗
 *
 * @author RB
 * @since 2026/7/24
 */
@Slf4j
public class RecordDataCleaner extends RecordCheckout {

    private RecordDataCleaner() {
        super(Collections.emptyMap());
    }

    /**
     * @param entity
     * @param data
     * @return
     */
    public static JSONObject cleanPostData(Entity entity, JSONObject data) {
        return cleanPostData(entity, data, true);
    }

    /**
     * @param entity
     * @param data
     * @param refAutoParse 是否自动解析引用字段值为 ID
     * @return
     */
    public static JSONObject cleanPostData(Entity entity, JSONObject data, boolean refAutoParse) {
        final RecordDataCleaner cleaner = new RecordDataCleaner();

        for (final String fieldName : data.keySet().toArray(new String[0])) {
            if (!entity.containsField(fieldName)) continue;

            final String fieldValue = data.getString(fieldName);
            if (StringUtils.isBlank(fieldValue)) continue;

            Field fieldMeta = entity.getField(fieldName);
            DisplayType dt = EasyMetaFactory.getDisplayType(fieldMeta);

            // 多值字段格式处理
            List<Object> fieldValue4N2N = null;

            if (dt == DisplayType.TAG) {
                String mv;
                if (JSONUtils.wellFormat(fieldValue))
                    mv = StringUtils.join(JSON.parseArray(fieldValue), EasyTag.VALUE_SPLIT);
                else mv = StringUtils.join(cleaner.checkoutTagValue(fieldValue), EasyTag.VALUE_SPLIT);

                if (StringUtils.isBlank(mv)) data.remove(fieldName);
                else data.put(fieldName, mv);
            } else if (dt == DisplayType.N2NREFERENCE) {
                if (JSONUtils.wellFormat(fieldValue)) fieldValue4N2N = JSON.parseArray(fieldValue);
                else {
                    fieldValue4N2N = new ArrayList<>();
                    for (String s : fieldValue.split(MVAL_SPLIT)) fieldValue4N2N.add(s.trim());
                }

                if (fieldValue4N2N == null || fieldValue4N2N.isEmpty()) data.remove(fieldName);
                else data.put(fieldName, StringUtils.join(fieldValue4N2N, EasyN2NReference.MV_SPLIT.trim()));
            } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
                if (JSONUtils.wellFormat(fieldValue)) {
                    JSONArray cleanArray = JSON.parseArray(fieldValue);
                    if (CollectionUtils.isEmpty(cleanArray)) {
                        data.remove(fieldName);
                    } else {
                        JSONArray cleanArray2 = new JSONArray();
                        for (Object o : cleanArray) {
                            if (o instanceof JSONObject) {
                                String oPath = uploadBase64File((JSONObject) o);
                                if (oPath != null) cleanArray2.add(oPath);
                            } else {
                                cleanArray2.add(o.toString());
                            }
                        }
                        data.put(fieldName, cleanArray2.toJSONString());
                    }
                } else {
                    String mv = cleaner.checkoutFileOrImage(fieldValue);
                    if (mv == null) data.remove(fieldName);
                    else data.put(fieldName, mv);
                }
            } else if (dt == DisplayType.BOOL) {
                if ("Y".equalsIgnoreCase(fieldValue) || "是".equals(fieldValue)) data.put(fieldName, true);
                else if ("N".equalsIgnoreCase(fieldValue) || "否".equals(fieldValue)) data.put(fieldName, false);
            }

            if (!refAutoParse) continue;

            // 自动解析引用字段值为 ID
            Object x;

            if (dt == DisplayType.PICKLIST) {
                x = cleaner.checkoutPickListValue(fieldMeta, fieldValue);
            } else if (dt == DisplayType.STATE) {
                x = cleaner.checkoutStateValue(fieldMeta, fieldValue);
            } else if (dt == DisplayType.MULTISELECT) {
                x = cleaner.checkoutMultiSelectValue(fieldMeta, fieldValue);
            } else if (dt == DisplayType.CLASSIFICATION) {
                x = cleaner.checkoutClassificationValue(fieldMeta, fieldValue);
            } else if (dt == DisplayType.REFERENCE) {
                x = cleaner.checkoutReferenceValue(fieldMeta, fieldValue);
            } else if (dt == DisplayType.N2NREFERENCE) {
                List<ID> xArray = new ArrayList<>();
                if (fieldValue4N2N != null) {
                    for (Object v : fieldValue4N2N) {
                        ID found = cleaner.checkoutReferenceValue(fieldMeta, v.toString());
                        if (found != null) xArray.add(found);
                    }
                }
                x = xArray.isEmpty() ? null : StringUtils.join(xArray, EasyN2NReference.MV_SPLIT.trim());
            } else {
                continue;
            }

            if (x != null) data.put(fieldName, x);
            else data.put(fieldName, StringUtils.EMPTY);
        }

        return data;
    }

    // 通过 BASE64 直接上传文件
    private static String uploadBase64File(JSONObject o) {
        String filename = o.getString("filename");
        String base64 = o.getString("base64");
        if (StringUtils.isBlank(filename) || StringUtils.isBlank(base64)) {
            log.warn("Invalid base64 file: {}", o);
            return null;
        }

        File tmp = RebuildConfiguration.getFileOfTemp(CommonsUtils.randomHex() + "-" + filename);
        try {
            CommonsUtils.base64ToFile(base64, tmp);
            long fileSize = FileUtils.sizeOf(tmp);
            String fileKey = QiniuCloud.uploadFile(tmp, filename);

            FilesHelper.storeFileSize(fileKey, fileSize);
            return fileKey;

        } catch (Exception e) {
            if (e instanceof RebuildException) throw (RebuildException) e;
            throw new RebuildException(e);
        }
    }

    private ID checkoutPickListValue(Field field, String value) {
        return super.checkoutPickListValue(field, new Cell(value));
    }

    private Integer checkoutStateValue(Field field, String value) {
        return super.checkoutStateValue(field, new Cell(value));
    }

    private Long checkoutMultiSelectValue(Field field, String value) {
        return super.checkoutMultiSelectValue(field, new Cell(value));
    }

    private ID checkoutClassificationValue(Field field, String value) {
        return super.checkoutClassificationValue(field, new Cell(value));
    }

    private ID checkoutReferenceValue(Field field, String value) {
        return super.checkoutReferenceValue(field, new Cell(value));
    }

    private String[] checkoutTagValue(String value) {
        return super.checkoutTagValue(new Cell(value));
    }

    private String checkoutFileOrImage(String value) {
        return super.checkoutFileOrImage(new Cell(value));
    }
}
