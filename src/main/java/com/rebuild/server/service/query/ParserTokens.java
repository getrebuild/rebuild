/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.query;

/**
 * @author devezhao
 * @since 2019/9/29
 */
public class ParserTokens {

    // Operational

    public static final String EQ = "EQ";
    public static final String NEQ = "NEQ";
    public static final String GT = "GT";
    public static final String LT = "LT";
    public static final String GE = "GE";
    public static final String LE = "LE";
    public static final String NL = "NL";
    public static final String NT = "NT";
    public static final String LK = "LK";
    public static final String NLK = "NLK";
    public static final String IN = "IN";
    public static final String NIN = "NIN";
    public static final String BW = "BW";
    public static final String BFD = "BFD";
    public static final String BFM = "BFM";
    public static final String AFD = "AFD";
    public static final String AFM = "AFM";
    public static final String RED = "RED";
    public static final String REM = "REM";
    public static final String SFU = "SFU";
    public static final String SFB = "SFB";
    public static final String SFD = "SFD";
    public static final String YTA = "YTA";
    public static final String TDA = "TDA";
    public static final String TTA = "TTA";
    public static final String BAND = "BAND";
    public static final String NBAND = "NBAND";

    /**
     * @param token
     * @return
     */
    protected static String convetOperator(String token) {
        if (EQ.equalsIgnoreCase(token)) return "=";
        else if (NEQ.equalsIgnoreCase(token)) return "<>";
        else if (GT.equalsIgnoreCase(token)) return ">";
        else if (LT.equalsIgnoreCase(token)) return "<";
        else if (GE.equalsIgnoreCase(token)) return ">=";
        else if (LE.equalsIgnoreCase(token)) return "<=";
        else if (NL.equalsIgnoreCase(token)) return "is null";
        else if (NT.equalsIgnoreCase(token)) return "is not null";
        else if (LK.equalsIgnoreCase(token)) return "like";
        else if (NLK.equalsIgnoreCase(token)) return "not like";
        else if (IN.equalsIgnoreCase(token)) return "in";
        else if (NIN.equalsIgnoreCase(token)) return "not in";
        else if (BW.equalsIgnoreCase(token)) return "between";
        else if (BFD.equalsIgnoreCase(token)) return "<=";  // "$before_day(%d)";
        else if (BFM.equalsIgnoreCase(token)) return "<=";  // "$before_month(%d)";
        else if (AFD.equalsIgnoreCase(token)) return ">=";  // "$after_day(%d)";
        else if (AFM.equalsIgnoreCase(token)) return ">=";  // "$after_month(%d)";
        else if (RED.equalsIgnoreCase(token)) return ">";   // "$recent_day(%d)";
        else if (REM.equalsIgnoreCase(token)) return ">";   // "$recent_month(%d)";
        else if (SFU.equalsIgnoreCase(token)) return "=";
        else if (SFB.equalsIgnoreCase(token)) return "=";
        else if (SFD.equalsIgnoreCase(token)) return "in";
        else if (YTA.equalsIgnoreCase(token)) return "=";
        else if (TDA.equalsIgnoreCase(token)) return "=";
        else if (TTA.equalsIgnoreCase(token)) return "=";
        else if (BAND.equalsIgnoreCase(token)) return "&&";
        else if (NBAND.equalsIgnoreCase(token)) return "!&";
        throw new UnsupportedOperationException("Unsupported token of operator : " + token);
    }

    // --

    // 日期时间

    public static final String ZERO_TIME = " 00:00:00";
    public static final String FULL_TIME = " 23:59:59";

}
