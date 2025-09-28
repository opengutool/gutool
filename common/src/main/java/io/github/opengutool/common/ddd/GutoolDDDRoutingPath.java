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
package io.github.opengutool.common.ddd;

import org.noear.dami.bus.Payload;
import org.noear.dami.bus.TopicListener;
import org.noear.dami.bus.impl.Routing;

import java.util.regex.Pattern;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolDDDRoutingPath<C, R> extends Routing<C, R> {

    private final Pattern pattern;

    /**
     * @param expr     表达式（* 表示一段，** 表示不限段）
     * @param index    顺序位
     * @param listener 监听器
     */
    public GutoolDDDRoutingPath(String expr, int index, TopicListener<Payload<C, R>> listener) {
        super(expr, index, listener);

        if (expr.contains("*")) {
            expr = expr.replace(".", "\\."); //支持 . 或 / 做为隔断

            //替换中间的**值
            expr = expr.replace("**", ".[]");

            //替换*值
            expr = expr.replace("*", "[^/\\.]*");

            //替换**值
            expr = expr.replace(".[]", ".*");

            // 替换$ 内部类
            expr = expr.replace("$", "\\$");

            //加头尾
            expr = "^" + expr + "$";

            this.pattern = Pattern.compile(expr);
        } else {
            this.pattern = null;
        }
    }

    /**
     * 匹配
     *
     * @param sentTopic 发送的主题
     */
    public boolean matches(String sentTopic) {
        if (super.matches(sentTopic)) {
            return true;
        }

        if (pattern != null) {
            return pattern.matcher(sentTopic).find();
        }

        return false;
    }
}
