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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.github.opengutool.domain.func.GutoolFuncRunHistory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 * 脚本运行器
 */
public class GutoolScriptRunner {
    private static final Logger logger = LoggerFactory.getLogger(GutoolScriptRunner.class);
    private static String TMP_FILE_PATH = System.getProperty("java.io.tmpdir") + File.separator;
    private static final AtomicInteger counter = new AtomicInteger(0);

    private final Consumer<String> runPrintStreamConsumer;
    private final Runnable runEndCallback;
    private final GutoolFuncRunHistory funcRunHistory;

    public GutoolScriptRunner(
            GutoolFuncRunHistory funcRunHistory,
            Consumer<String> printStreamConsumer,
            Runnable endCallback) {
        this.runPrintStreamConsumer = printStreamConsumer;
        this.funcRunHistory = funcRunHistory;
        this.runEndCallback = endCallback;
    }

    public <T> void compileAndExecuteInBackground(Consumer<T> resultHandler) {
        GroovyShell shell;
        String tmpFile = TMP_FILE_PATH + "ScriptTemp" + counter.incrementAndGet() + ".groovy";
        try {
            printStr("-----start-----");
            final File file = FileUtil.touch(tmpFile);
            FileUtil.writeString(funcRunHistory.getFuncMirror(), file, Charset.defaultCharset());
            Binding binding = new Binding();
            binding.setProperty("args", new String[0]);
            Object params;
            // 如果是 json 自动成对象
            if (JSONUtil.isTypeJSONObject(funcRunHistory.getFuncIn())) {
                params = JSONUtil.parseObj(funcRunHistory.getFuncIn());
            } else if (JSONUtil.isTypeJSONArray(funcRunHistory.getFuncIn())) {
                params = JSONUtil.parseArray(funcRunHistory.getFuncIn());
            } else {
                params = funcRunHistory.getFuncIn();
            }
            GutoolScriptEvent event = funcRunHistory.getEvent();
            event.setParams(params);

            binding.setProperty("params", params);
            // 新增 event 对象（主要包含类型和 xxx id）
            binding.setProperty("event", event);

            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(GutoolScript.class.getName());
            shell = new GroovyShell(binding, config);
            groovy.lang.Script script = shell.parse(new FileReader(file));
            // 异步执行脚本
            changeOutInBackground(binding, script::run, (status, obj) -> {
                this.resultToFuncRunHistory(status, (T) obj);
                if (Objects.nonNull(resultHandler)) {
                    resultHandler.accept((T) obj);
                }
            });
        } catch (Exception e) {
            printStrOrigin(ExceptionUtil.stacktraceToString(e, Integer.MAX_VALUE));
            printStr("-----error-----");
            funcRunHistory.update(ExceptionUtil.stacktraceToString(e, 1000), "error");
            runEndCallback.run();
        }
    }

    private <T> void resultToFuncRunHistory(String status, T result) {
        String funcOut = "";
        if (ObjectUtil.isNotEmpty(result)) {
            try {
                if (result instanceof CharSequence) {
                    funcOut = result.toString();
                } else {
                    funcOut = JSONUtil.toJsonStr(result);
                }
            } catch (Exception e) {
                status = "error";
                funcOut = ExceptionUtil.stacktraceToString(e, 1000);
            }
        }
        funcRunHistory.update(funcOut, status);
    }

    private void printStr(String str) {
        if (StrUtil.isNotBlank(str)) {
            runPrintStreamConsumer.accept(DateUtil.format(new Date(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + " " + str.trim() + "\r\n");
        }
    }

    private void printStrOrigin(String str) {
        runPrintStreamConsumer.accept(str);
    }

    private void changeOutInBackground(Binding binding, Supplier<Object> supplier, BiConsumer<String, Object> resultHandler) {
        new SwingWorker<Void, String>() {  // Changed Void to String to publish intermediate output
            @Override
            protected Void doInBackground() throws Exception {
                try (final RealTimeStream realTimeStream = new RealTimeStream(this::publish);
                     PrintStream cacheStream = new PrintStream(realTimeStream, true)) {
                    // 绑定自定义的 PrintStream
                    binding.setProperty("out", cacheStream);
                    resultHandler.accept("success", supplier.get());
                    cacheStream.flush();
                    realTimeStream.flush();
                    return null;
                } catch (Exception e) {
                    resultHandler.accept("error", ExceptionUtil.stacktraceToString(e, 1000));
                    printStrOrigin(ExceptionUtil.stacktraceToString(e, Integer.MAX_VALUE));
                    printStr("-----error-----");
                    return null;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    printStrOrigin(chunk);
                }
            }

            @Override
            protected void done() {
                printStr("-----end-----");
                runEndCallback.run();
            }
        }.execute();
    }

    private static class RealTimeStream extends OutputStream {
        private final Consumer<String> outputConsumer;
        private final StringBuffer buffer;

        public RealTimeStream(Consumer<String> outputConsumer) {
            this.outputConsumer = outputConsumer;
            this.buffer = new StringBuffer();
        }


        @Override
        public void write(int b) throws IOException {
            if (b == '\n') {
                buffer.append((char) b);
                String line = buffer.toString();
                outputConsumer.accept(line);
                buffer.setLength(0);
            } else {
                buffer.append((char) b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            String chunk = new String(b, off, len);
            String[] lines = chunk.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    buffer.append("\n");
                    outputConsumer.accept(buffer.toString());
                    buffer.setLength(0);
                }
                buffer.append(lines[i]);
            }
        }

        @Override
        public void flush() throws IOException {
            if (!buffer.isEmpty()) {
                outputConsumer.accept(buffer.toString());
                buffer.setLength(0);
            }
        }

        @Override
        public void close() throws IOException {
            buffer.setLength(0);
        }
    }

}
