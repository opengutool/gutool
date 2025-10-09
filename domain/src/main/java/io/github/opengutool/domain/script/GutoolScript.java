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
package io.github.opengutool.domain.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import groovy.lang.GroovyShell;
import io.github.opengutool.domain.func.GutoolFuncContainer;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public abstract class GutoolScript extends groovy.lang.Script {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static String TMP_FILE_PATH = System.getProperty("java.io.tmpdir") + File.separator;

    public Object run(String funcName, String[] args) throws CompilationFailedException, IOException {
        Object params = this.getBinding().getProperty("params");
        if (ObjectUtil.isEmpty(params)) {
            params = "";
            this.getBinding().setProperty("params", params);
        }
        // 设置 event
        Object event = this.getBinding().getVariable("event");
        if (ObjectUtil.isEmpty(params)) {
            throw new RuntimeException("Gutool script event not found");
        }
        if (event instanceof GutoolScriptEvent) {
            ((GutoolScriptEvent) event).setParams(params);
        }


        String funcContent = GutoolFuncContainer.getFuncContentByName(funcName);
        if (StrUtil.isBlankOrUndefined(funcContent)) {
            return null;
        }
        String tmpFile = TMP_FILE_PATH + "ScriptTemp" + counter.incrementAndGet() + ".groovy";
        final File file = FileUtil.touch(tmpFile);
        FileUtil.writeString(funcContent, file, Charset.defaultCharset());
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(GutoolScript.class.getName());
        GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), this.getBinding(), config);
        return shell.run(file, args);
    }

    public Object run(String funcName, Object params) throws CompilationFailedException, IOException {
        this.getBinding().setProperty("params", ObjectUtil.isEmpty(params) ? "" : params);
        return this.run(funcName, new String[]{});
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        try {
            // 首先尝试调用父类方法
            return super.invokeMethod(name, args);
        } catch (Exception e) {
            // 如果方法不存在，尝试作为脚本名称执行
            String funcContent = GutoolFuncContainer.getFuncContentByName(name);
            if (StrUtil.isBlankOrUndefined(funcContent)) {
                throw new RuntimeException("Method '" + name + "' not found and script '" + name + "' not found");
            }
            try {
                return run(name, args);
            } catch (CompilationFailedException | IOException ex) {
                throw new RuntimeException("Failed to execute script '" + name + "':", ex);
            }
        }
    }
}
