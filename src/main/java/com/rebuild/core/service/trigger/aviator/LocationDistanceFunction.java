/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.ObjectUtils;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.rebuild.core.metadata.MetadataHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Usage: LOCATIONDISTANCE(location1, location2)
 * Return: Number (米)
 *
 * @author devezhao
 * @since 2022/3/1
 */
@Slf4j
public class LocationDistanceFunction extends AbstractFunction {
    private static final long serialVersionUID = -6435900808414828331L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        final double[] L1s = parseLngLat(arg1.getValue(env).toString());
        if (L1s == null) {
            log.warn("Bad lnglat(1) format : {}", arg1.getValue(env));
            return AviatorDouble.valueOf(0);
        }

        final double[] L2s = parseLngLat(arg2.getValue(env).toString());
        if (L2s == null) {
            log.warn("Bad lnglat(2) format : {}", arg2.getValue(env));
            return AviatorDouble.valueOf(0);
        }

        double LNG1 = L1s[0];
        double LAT1 = L1s[1];

        double LNG2 = L2s[0];
        double LAT2 = L2s[1];

        double LAT1p = (Math.PI / 180) * LAT1;
        double LAT2p = (Math.PI / 180) * LAT2;
        double LNG1p = (Math.PI / 180) * LNG1;
        double LNG2p = (Math.PI / 180) * LNG2;

        double R = 6371;
        double scale = 1000;

        double d = Math.acos(Math.sin(LAT1p) * Math.sin(LAT2p) + Math.cos(LAT1p) * Math.cos(LAT2p) * Math.cos(LNG2p - LNG1p)) * R * scale;
        return AviatorDouble.valueOf(d);
    }

    // 123.456789,123.456789
    // 地址$$$$123.456789,123.456789
    private double[] parseLngLat(String lnglat) {
        String[] lnglats = lnglat.split(MetadataHelper.SPLITER_RE);
        if (lnglats.length == 2) {
            lnglats = lnglats[1].split(",");
        } else {
            lnglats = lnglat.split(",");
        }

        if (lnglats.length != 2) return null;

        double lng = ObjectUtils.toDouble(lnglats[0], -1);
        double lat = ObjectUtils.toDouble(lnglats[1], -1);

        if (lng < 0 || lat < 0) return null;

        return new double[] { lng, lat };
    }

    @Override
    public String getName() {
        return "LOCATIONDISTANCE";
    }
}
