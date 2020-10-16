/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

/**
 * 暂不存在的实体
 *
 * @author devezhao
 * @since 2020/9/29
 */
public class GhostEntity extends UnsafeEntity {

    public GhostEntity(String name) {
        super(name, null, null, -1, null);
    }
}
