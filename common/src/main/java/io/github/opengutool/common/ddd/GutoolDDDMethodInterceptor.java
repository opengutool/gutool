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

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.noear.dami.Dami;
import org.noear.dami.bus.DamiBus;

import java.lang.reflect.Method;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolDDDMethodInterceptor<D> implements MethodInterceptor {

    protected final D d;
    private final String topicClassName;
    private final DamiBus<GutoolDDDMethodEvent<D>, GutoolDDDMethodEvent<D>> eventBus;

    public GutoolDDDMethodInterceptor(D d) {
        this.d = d;
        this.topicClassName = d.getClass().getName();
        this.eventBus = Dami.bus();
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

        // 前置
        GutoolDDDMethodEvent<D> beforeMethodEvent = new GutoolDDDMethodEvent<>(this, d, method, args);
        GutoolDDDMethodEvent<D> beforeEvent = eventBus.sendAndRequest(
                this.buildMethodBeforeTopic(method), beforeMethodEvent, 60000, () -> beforeMethodEvent);
        // 如果使用，直接返回
        // 即事件可以阻断方法执行
        if (beforeEvent.getUseEventResult()) {
            return beforeEvent.getEventResult();
        }
        // 执行
        Object proceed = methodProxy.invoke(d, args);

        // 后置
        final GutoolDDDMethodEvent<D> afterMethodEvent = new GutoolDDDMethodEvent<>(this, d, method, args, proceed);
        GutoolDDDMethodEvent<D> afterEvent = eventBus.sendAndRequest(
                this.buildMethodAfterTopic(method), afterMethodEvent, 60000, () -> afterMethodEvent);

        // 如果使用，直接返回
        // 即事件可以重置方法返回结果
        if (afterEvent.getUseEventResult()) {
            return afterEvent.getEventResult();
        } else {
            return proceed;
        }
    }

    public String buildMethodAfterTopic(Method method) {
        return this.buildMethodTopic(method) + ".after";
    }

    public String buildMethodBeforeTopic(Method method) {
        return this.buildMethodTopic(method) + ".before";
    }

    public String buildMethodTopic(Method method) {
        return "gutool." + topicClassName + "." + method.getName();
    }
}
