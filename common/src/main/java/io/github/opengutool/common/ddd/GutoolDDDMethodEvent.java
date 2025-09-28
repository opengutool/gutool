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

import java.lang.reflect.Method;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolDDDMethodEvent<D> {
    private static final long serialVersionUID = 1L;

    private final GutoolDDDMethodInterceptor<D> methodInterceptor;

    private final Method method;

    private final Object[] args;

    private final GutoolDDDMethodAroundType aroundType;

    private boolean useEventResult = false;

    private final Object proceedResult;

    private Object eventResult;

    private final D source;

    public D getSource() {
        return source;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public GutoolDDDMethodAroundType getAroundType() {
        return aroundType;
    }

    public Object getProceedResult() {
        return proceedResult;
    }

    public boolean getUseEventResult() {
        return useEventResult;
    }

    public Object getEventResult() {
        return eventResult;
    }

    public void setEventResult(Object domainEventResult) {
        this.useEventResult = true;
        this.eventResult = domainEventResult;
    }

    public GutoolDDDMethodEvent(GutoolDDDMethodInterceptor<D> methodInterceptor, D source, Method method, Object[] args) {
        this.source = source;
        this.methodInterceptor = methodInterceptor;
        this.method = method;
        this.aroundType = GutoolDDDMethodAroundType.BEFORE;
        this.args = args;
        this.proceedResult = null;
    }

    public GutoolDDDMethodEvent(GutoolDDDMethodInterceptor<D> methodInterceptor, D source, Method method, Object[] args, Object proceedResult) {
        this.source = source;
        this.methodInterceptor = methodInterceptor;
        this.method = method;
        this.args = args;
        this.proceedResult = proceedResult;
        this.aroundType = GutoolDDDMethodAroundType.AFTER;
    }
}
