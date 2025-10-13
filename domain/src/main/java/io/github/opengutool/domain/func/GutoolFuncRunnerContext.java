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

import io.github.opengutool.domain.script.GutoolScriptRunner;

import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/10/13
 * Thread-local context for GutoolFunc to handle multithreading
 */
public class GutoolFuncRunnerContext {

    private GutoolFuncRunHistory funcRunHistory;
    private GutoolScriptRunner scriptRunner;
    private boolean initialize = false;
    private boolean running = false;

    private static final ThreadLocal<GutoolFuncRunnerContext> THREAD_LOCAL_CONTEXT =
        ThreadLocal.withInitial(GutoolFuncRunnerContext::new);

    public static GutoolFuncRunnerContext getCurrentContext() {
        return THREAD_LOCAL_CONTEXT.get();
    }

    public static void removeCurrentContext() {
        THREAD_LOCAL_CONTEXT.remove();
    }

    public GutoolFuncRunHistory getFuncRunHistory() {
        return funcRunHistory;
    }

    public void setFuncRunHistory(GutoolFuncRunHistory funcRunHistory) {
        this.funcRunHistory = funcRunHistory;
    }

    public GutoolScriptRunner getScriptRunner() {
        return scriptRunner;
    }

    public void setScriptRunner(GutoolScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner;
    }

    public boolean isInitialize() {
        return initialize;
    }

    public void setInitialize(boolean initialize) {
        this.initialize = initialize;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * 初始化
     */
    public GutoolFuncRunnerContext initRunner(
            GutoolFunc gutoolFunc,
            GutoolFuncTabPanel funcTabPanel,
            String funcIn,
            Consumer<String> printStreamConsumer,
            Runnable endCallback) {
        this.funcRunHistory = new GutoolFuncRunHistory(gutoolFunc, funcTabPanel, funcIn);
        this.scriptRunner = new GutoolScriptRunner(this.getFuncRunHistory(), printStreamConsumer, endCallback);
        this.initialize = true;
        return this;
    }

    /**
     * 异步运行
     */
    public <T> boolean asyncRun(Consumer<T> resultHandler) {
        if (this.initialize && this.scriptRunner != null) {
            this.running = true;
            this.scriptRunner.compileAndExecuteInBackground(resultHandler);
            return true;
        }
        return false;
    }

    /**
     * 重置
     */
    public boolean resetRunner() {
        this.scriptRunner = null;
        this.funcRunHistory = null;
        this.initialize = false;
        this.running = false;
        return true;
    }
}