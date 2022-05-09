/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import cn.devezhao.commons.ThreadPool;
import com.rebuild.TestSupport;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class HeavyTaskTest extends TestSupport {

    @Test
    public void testTask() {
        String taskid1 = TaskExecutors.submit(new TestTask("testTask1", 5), UserService.SYSTEM_USER);
        System.out.println("Submit Task1 : " + taskid1);

        String taskid2 = TaskExecutors.submit(new TestTask("testTask2", 5), UserService.SYSTEM_USER);
        System.out.println("Submit Task2 : " + taskid2);

        ThreadPool.waitFor(1000);

        HeavyTask<?> task = TaskExecutors.get(taskid1);
        System.out.println("getElapsedTime " + task.getElapsedTime());
        System.out.println("getCompletedPercent " + task.getCompletedPercent());
        System.out.println("getErrorMessage " + task.getErrorMessage());
        System.out.println("isInterrupted " + task.isInterrupted());

        new TaskExecutors().executeJob();
    }

    @Test
    public void testRejected() {
        for (int i = 0; i < 500; i++) {
            try {
                TaskExecutors.submit(new TestTask("testRejected", 2), UserService.SYSTEM_USER);
            } catch (RejectedExecutionException ex) {
                break;
            }
        }
    }

    @Test
    public void testCancel() {
        ThreadPool.waitFor(1000);  // Wait testRejected

        String taskid = TaskExecutors.submit(new TestTask("testCancel", 100), UserService.SYSTEM_USER);
        System.out.println("Submit Task : " + taskid);

        ThreadPool.waitFor(1000);
        boolean cancel = TaskExecutors.cancel(taskid);
        System.out.println("Cancel Task : " + taskid + " > " + cancel);

        ThreadPool.waitFor(1000);
    }

    // --

    static class TestTask extends HeavyTask<Void> {

        private String name;
        private int number;

        protected TestTask(String name, int number) {
            this.name = "[ " + name.toUpperCase() + " ] ";
            this.number = number;
        }

        @Override
        public Void exec() {
            this.setTotal(this.number);
            for (int i = 0; i < this.number; i++) {
                if (this.isInterrupted()) {
                    System.err.println(this.name + "Interrupted : " + this.getCompleted() + " / " + this.getTotal());
                    break;
                }

                ThreadPool.waitFor(50);  // Mock time
                System.out.println(this.name + "Mock ... " + i);
                this.addCompleted();
            }
            return null;
        }
    }
}
