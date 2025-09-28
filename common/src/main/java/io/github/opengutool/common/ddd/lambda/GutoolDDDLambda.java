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
package io.github.opengutool.common.ddd.lambda;

import cn.hutool.core.map.WeakConcurrentMap;
import cn.hutool.core.util.ReflectUtil;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolDDDLambda {
    private static final WeakConcurrentMap<String, SerializedLambda> cache = new WeakConcurrentMap<>();

    private static SerializedLambda _resolve(Serializable func) {
        return cache.computeIfAbsent(func.getClass().getName(), (key) -> ReflectUtil.invoke(func, "writeReplace"));
    }

    public static String getMethodName(Serializable fn) {
        return _resolve(fn).getImplMethodName();
    }

    @FunctionalInterface
    public interface SFunction<D, R> extends Serializable {
        R apply(D d);
    }

    @FunctionalInterface
    public interface SConsumer<P1, P2> extends Serializable {
        void accept(P1 p1, P2 p2);
    }

    @FunctionalInterface
    public interface SConsumer3<P1, P2, P3> extends Serializable {
        void accept(P1 p1, P2 p2, P3 p3);
    }

    @FunctionalInterface
    public interface SConsumer4<P1, P2, P3, P4> extends Serializable {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    @FunctionalInterface
    public interface SConsumer5<P1, P2, P3, P4, P5> extends Serializable {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    @FunctionalInterface
    public interface SConsumer6<P1, P2, P3, P4, P5, P6> extends Serializable {
        void accept(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }
}
