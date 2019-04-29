/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.helper.task;

import java.util.concurrent.RejectedExecutionException;

import org.junit.Test;

import cn.devezhao.commons.ThreadPool;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class BulkTaskTest {

	@Test
	public void testTask() throws Exception {
		String taskid1 = TaskExecutor.submit(new TestTask("testTask1", 5));
		System.out.println("Submit Task1 : " + taskid1);
		
		String taskid2 = TaskExecutor.submit(new TestTask("testTask2",5));
		System.out.println("Submit Task2 : " + taskid2);
		
		ThreadPool.waitFor(2 * 1000);
	}
	
	@Test(expected=RejectedExecutionException.class)
	public void testRejected() throws Exception {
		for (int i = 0; i < 30; i++) {
			TaskExecutor.submit(new TestTask("testRejected", 2));
		}
	}
	
	@Test
	public void testCancel() throws Exception {
		ThreadPool.waitFor(1 * 1000);  // Wait testRejected
		
		String taskid = TaskExecutor.submit(new TestTask("testCancel", 100));
		System.out.println("Submit Task : " + taskid);
		
		ThreadPool.waitFor(1000 * 2);
		boolean cancel = TaskExecutor.cancel(taskid);
		System.out.println("Cancel Task : " + taskid + " > " + cancel);
		
		ThreadPool.waitFor(1 * 1000);
	}
	
	static class TestTask extends HeavyTask {
		private String name;
		private int number;
		protected TestTask(String name, int number) {
			this.name = "[ " + name.toUpperCase() + " ] ";
			this.number = number;
		}
		@Override
		public void run() {
			this.setTotal(this.number);
			for (int i = 0; i < this.number; i++) {
				if (this.isInterrupted()) {
					System.err.println(this.name + "Interrupted : " + this.getComplete() + " / " + this.getTotal());
					break;
				}
				
				ThreadPool.waitFor(200);  // Mock time
				System.out.println(this.name + "Mock ... " + i);
				this.setCompleteOne();
			}
			this.completedAfter();
		}
	}
}
