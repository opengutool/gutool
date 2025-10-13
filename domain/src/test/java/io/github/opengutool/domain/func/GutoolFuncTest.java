/*
 * Copyright © 2025/9/3 gutool (gutool@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.opengutool.domain.func;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/13
 */
public class GutoolFuncTest {

    private GutoolFunc gutoolFunc;

    @Before
    public void setUp() {
        gutoolFunc = new GutoolFunc();
        gutoolFunc.setId(1L);
        gutoolFunc.setName("testFunc");
        gutoolFunc.setContent("println 'Hello World'");
    }

    @Test
    public void testThreadLocalIsolation() throws InterruptedException {
        final int threadCount = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // 每个线程初始化同一个GutoolFunc实例
                    gutoolFunc.initRunner(
                        null,
                        "thread-" + threadIndex,
                        output -> System.out.println("Thread-" + threadIndex + ": " + output),
                        () -> System.out.println("Thread-" + threadIndex + " finished")
                    );

                    // 验证运行历史是独立的
                    assertEquals("Thread " + threadIndex + " should have correct funcIn",
                                "thread-" + threadIndex,
                                gutoolFunc.getFuncRunHistory().getFuncIn());

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread " + threadIndex + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals("All threads should complete successfully", threadCount, successCount.get());
    }

}
