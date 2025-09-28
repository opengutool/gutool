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

import io.github.opengutool.domain.formatter.GroovyCodeFormatter;
import io.github.opengutool.domain.script.GutoolScriptRunner;
import lombok.Data;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Data
public class GutoolFunc {

    private Long id;

    private String name;

    private String define;

    private String content;

    private String remark;

    private String createTime;

    private String modifiedTime;


    private GutoolFuncRunHistory funcRunHistory;
    private GutoolScriptRunner scriptRunner;
    private boolean initialize = false;
    private boolean running = false;

    public boolean setName(String name) {
        if (!GutoolFuncContainer.existName(name)) {
            this.name = name;
            return true;
        }
        return false;
    }

    /**
     * 初始化
     */
    public GutoolFunc initRunner(
            GutoolFuncTabPanel funcTabPanel,
            String funcIn,
            Consumer<String> printStreamConsumer,
            Runnable endCallback) {
        this.funcRunHistory = new GutoolFuncRunHistory(this, funcTabPanel, funcIn);
        this.scriptRunner = new GutoolScriptRunner(this.getFuncRunHistory(), printStreamConsumer, endCallback);
        this.initialize = true;
        return this;
    }

    /**
     * 异步运行
     */
    public <T> boolean asyncRun(Consumer<T> resultHandler) {
        if (this.initialize && Objects.nonNull(this.scriptRunner)) {
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

    /**
     * 更新脚本
     */
    public boolean updateScript(String content) {
        this.content = content;
        return true;
    }

    /**
     * 格式化更新脚本
     */
    public boolean formatUpdateScript(String content) {
        this.content = GroovyCodeFormatter.format(content);
        return true;
    }
}
