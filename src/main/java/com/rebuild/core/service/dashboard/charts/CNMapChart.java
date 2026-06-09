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
        if (nums.length > 0) this.calcFormula43(array, nums);

        List<Object[]> datas = new ArrayList<>();
        for (Object[] o : array) {
            String map = (String) o[0];
            if (StringUtils.isBlank(map)) continue;

            String[] locSplit = map.split(CommonsUtils.COMM_SPLITER_RE);
            if (locSplit.length != 2 || StringUtils.isBlank(locSplit[1])) {
                // fix: 数据格式不对?
                if (JSONUtils.wellFormat(map)) {
                    JSONObject fix43 = JSON.parseObject(map);
                    locSplit = new String[]{
                            fix43.getString("text"),
                            fix43.getString("lng") + "," + fix43.getString("lat"),
                    };
                } else {
                    continue;
                }
            }

            List<Object> item = new ArrayList<>();
            item.add(locSplit[0]);
            item.add(locSplit[1]);
            for (int i = 0; i < nums.length; i++) {
                item.add(wrapAxisValue(nums[i], o[i + 1]));
            }
            datas.add(item.toArray(new Object[0]));
        }

        List<String> name = new ArrayList<>();
        name.add(dim1.getLabel());
        for (Numerical num : nums) {
            name.add(num.getLabel());
        }

        JSONObject renderOption = config.getJSONObject("option");

        return JSONUtils.toJSONObject(
                new String[]{"data", "name", "_renderOption"},
                new Object[]{datas, name, renderOption});
    }

    private String buildSql() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        if (nums.length > 0) {
            return buildSql(dims[0], nums, true);
        }

        String sql = "select {0} from {1} where {2}";
        sql = MessageFormat.format(sql,
                dims[0].getSqlName(), getSourceEntity().getName(), getFilterSql());

        return appendSqlSort(sql);
    }
}
