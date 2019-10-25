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

package com.rebuild.utils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求频率计数器。非线程安全
 *
 * @author devezhao
 * @since 2019/10/25
 */
public class RequestFrequencyCounter implements Serializable {
    private static final long serialVersionUID = -5330361946689461083L;

    private Map<String, RequestFrequencyCounter> muiltCounters;

    private LinkedList<Long> requestCounter = new LinkedList<>();

    private int seconds = 1;

    public RequestFrequencyCounter() {
        super();
    }

    /**
     * @param name
     * @return
     */
    public RequestFrequencyCounter counter(String name) {
        if (muiltCounters == null) {
            muiltCounters = new ConcurrentHashMap<>();
        }

        RequestFrequencyCounter c = muiltCounters.get(name);
        if (c == null) {
            c = new RequestFrequencyCounter();
            muiltCounters.put(name, c);
        }
        return c;
    }

    /**
     * @return
     */
    public RequestFrequencyCounter add() {
        requestCounter.addFirst(System.currentTimeMillis());
        if (requestCounter.size() > Short.MAX_VALUE) {
            requestCounter.removeLast();
        }
        return this;
    }

    /**
     * @return
     */
    public RequestFrequencyCounter seconds() {
        return seconds(1);
    }

    /**
     * @param seconds
     * @return
     */
    public RequestFrequencyCounter seconds(int seconds) {
        this.seconds = seconds;
        return this;
    }

    /**
     * @return
     */
    public RequestFrequencyCounter minutes() {
        return minutes(1);
    }

    /**
     * @param minutes
     * @return
     */
    public RequestFrequencyCounter minutes(int minutes) {
        this.seconds = minutes * 60;
        return this;
    }

    /**
     * @return
     */
    public int times() {
        int c = 0;
        long ctm = System.currentTimeMillis() - (this.seconds * 1000);
        for (Long t : requestCounter) {
            if (t < ctm) break;
            c++;
        }
        return c;
    }

    /**
     * 大于
     *
     * @param times
     * @return
     */
    public boolean than(int times) {
        return times() > times;
    }

    /**
     */
    public void clear() {
        this.requestCounter.clear();
    }
}
