package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图
 *
 * @author RB
 * @since 3/16/2024
 */
public class CNMapChart extends ChartData {

    protected CNMapChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        RbAssert.isCommercial(Language.L("免费版不支持此图表"));

        Dimension[] dims = getDimensions();
        Dimension dim1 = dims[0];
        Numerical[] nums = getNumericals();

        Field locationOrDqClazz = dim1.getField();
        EasyField easyField = EasyMetaFactory.valueOf(locationOrDqClazz);
        if (easyField.getDisplayType() != DisplayType.LOCATION) {
            throw new DefinedException("“地图”仅支持位置字段");
        }

        String sql = buildSql();
        Object[][] array = createQuery(sql).setMaxResults(5000).array();

        List<Object[]> datas = new ArrayList<>();
        for (Object[] o : array) {
            String map = (String) o[0];
            if (StringUtils.isBlank(map)) continue;

            String[] mapSplit = map.split(CommonsUtils.COMM_SPLITER_RE);
            if (mapSplit.length != 2 || StringUtils.isBlank(mapSplit[1])) continue;

            Object n = nums.length > 0 ? wrapAxisValue(nums[0], o[1]) : 0;
            datas.add(new Object[]{mapSplit[0], mapSplit[1], n});
        }

        String[] name = new String[]{
                dim1.getLabel(),
                nums.length > 0 ? nums[0].getLabel() : null
        };
        JSONObject renderOption = config.getJSONObject("option");

        return JSONUtils.toJSONObject(
                new String[]{"data", "name", "_renderOption"},
                new Object[]{datas, name, renderOption});
    }

    private String buildSql() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        if (nums.length > 0) {
            return buildSql(dims[0], nums, false);
        }

        String sql = "select {0} from {1} where {2}";
        sql = MessageFormat.format(sql,
                dims[0].getSqlName(), getSourceEntity().getName(), getFilterSql());

        return appendSqlSort(sql);
    }
}
