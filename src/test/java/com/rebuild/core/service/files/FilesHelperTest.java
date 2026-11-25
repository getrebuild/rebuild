/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.files;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2019/11/15
 */
public class FilesHelperTest extends TestSupport {

    @BeforeEach
    public void setUpPerMethod() {
        UserContextHolder.setUser(UserService.ADMIN_USER);
    }

    @Test
    void getFolders() {
        Record folder = EntityHelper.forNew(EntityHelper.AttachmentFolder, SIMPLE_USER);
        folder.setString("name", "123456");
        folder = Application.getService(EntityHelper.AttachmentFolder).create(folder);

        System.out.println(FilesHelper.getAccessableFolders(SIMPLE_USER, null));
        Application.getService(EntityHelper.AttachmentFolder).delete(folder.getPrimary());
    }

    @Test
    void isPathReadable() {
        final String path = "rb/test/" + System.nanoTime() + ".txt";

        // 非附件文件
        Assertions.assertTrue(FilesHelper.isPathReadable(path, SIMPLE_USER));

        // 附件所属记录不可读
        Record record = FilesHelper.createAttachment(path, UserService.ADMIN_USER);
        record.setInt("belongEntity", MetadataHelper.getEntity(TestAllFields).getEntityCode());
        record.setString("belongField", "text");
        record.setID("relatedRecord", ID.newId(MetadataHelper.getEntity(TestAllFields).getEntityCode()));
        record = Application.getCommonsService().create(record);

        try {
            Assertions.assertFalse(FilesHelper.isPathReadable(path, SIMPLE_USER));
            Assertions.assertFalse(FilesHelper.isPathReadable(CodecUtils.urlEncode(path), SIMPLE_USER));
            Assertions.assertTrue(FilesHelper.isPathReadable(path, UserService.ADMIN_USER));

            // 同一路径下存在公开文件
            Record samePath = FilesHelper.createAttachment(path, UserService.ADMIN_USER);
            samePath = Application.getCommonsService().create(samePath);
            try {
                Assertions.assertTrue(FilesHelper.isPathReadable(path, SIMPLE_USER));
            } finally {
                Application.getService(EntityHelper.Attachment).delete(samePath.getPrimary());
            }
        } finally {
            Application.getService(EntityHelper.Attachment).delete(record.getPrimary());
        }
    }

    @Test
    void isPathReadableIgnoreDeleted() {
        final String path = "rb/test/" + System.nanoTime() + ".txt";

        Record record = FilesHelper.createAttachment(path, UserService.ADMIN_USER);
        record.setInt("belongEntity", MetadataHelper.getEntity(TestAllFields).getEntityCode());
        record.setString("belongField", "text");
        record.setID("relatedRecord", ID.newId(MetadataHelper.getEntity(TestAllFields).getEntityCode()));
        record.setBoolean("isDeleted", true);
        record = Application.getCommonsService().create(record);

        try {
            Assertions.assertTrue(FilesHelper.isPathReadable(path, SIMPLE_USER));
        } finally {
            Application.getService(EntityHelper.Attachment).delete(record.getPrimary());
        }
    }

}
