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

import io.github.opengutool.common.ddd.lambda.GutoolDDDLambda;
import net.sf.cglib.proxy.Enhancer;
import org.noear.dami.Dami;
import org.noear.dami.DamiConfig;
import org.noear.dami.bus.impl.TopicRouterPatterned;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class GutoolDDDFactory {

    public static void init() {
        DamiConfig.configure(new TopicRouterPatterned<>(GutoolDDDRoutingPath::new));
    }

    public static <D> D create(D d) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(d.getClass());
        enhancer.setCallback(new GutoolDDDMethodInterceptor<>(d));
        return (D) enhancer.create();
    }

    public static <D> void listen(Class<D> dClass, Consumer<GutoolDDDMethodEvent<D>> consumer) {
        Dami.<GutoolDDDMethodEvent<D>, GutoolDDDMethodEvent<D>>bus().listen("gutool." + dClass.getName() + ".**", (payload) -> {
            consumer.accept(payload.getContent());
            payload.reply(payload.getContent());
        });
    }

    public static <D, T> void listen(
            Class<D> dClass,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SFunction<D, T>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                Dami.<GutoolDDDMethodEvent<D>, GutoolDDDMethodEvent<D>>bus().listen("gutool." + dClass.getName() + "." +
                        GutoolDDDLambda.getMethodName(methodFunctions[i]) + ".**", (payload) -> {
                    consumer.accept(payload.getContent());
                    payload.reply(payload.getContent());
                });
            }
        }
    }

    public static <D> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer) {
        Dami.<GutoolDDDMethodEvent<D>, GutoolDDDMethodEvent<D>>bus().listen("gutool." + dClass.getName() + "." + aroundType.name().toLowerCase(), (payload) -> {
            consumer.accept(payload.getContent());
            payload.reply(payload.getContent());
        });
    }

    public static <D, T> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SFunction<D, T>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, aroundType, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                _listen(dClass, aroundType, consumer, methodFunctions[i]);
            }
        }
    }

    public static <D, P2> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SConsumer<D, P2>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, aroundType, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                _listen(dClass, aroundType, consumer, methodFunctions[i]);
            }
        }
    }

    public static <D, P2, P3> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SConsumer3<D, P2, P3>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, aroundType, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                _listen(dClass, aroundType, consumer, methodFunctions[i]);
            }
        }
    }

    public static <D, P2, P3, P4> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SConsumer4<D, P2, P3, P4>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, aroundType, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                _listen(dClass, aroundType, consumer, methodFunctions[i]);
            }
        }
    }

    public static <D, P2, P3, P4, P5> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SConsumer5<D, P2, P3, P4, P5>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, aroundType, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                _listen(dClass, aroundType, consumer, methodFunctions[i]);
            }
        }
    }

    public static <D, P2, P3, P4, P5, P6> void listen(
            Class<D> dClass,
            GutoolDDDMethodAroundType aroundType,
            Consumer<GutoolDDDMethodEvent<D>> consumer,
            GutoolDDDLambda.SConsumer6<D, P2, P3, P4, P5, P6>... methodFunctions) {
        if (Objects.isNull(methodFunctions) || methodFunctions.length == 0) {
            listen(dClass, aroundType, consumer);
        } else {
            for (int i = 0; i < methodFunctions.length; i++) {
                _listen(dClass, aroundType, consumer, methodFunctions[i]);
            }
        }
    }

    private static <D> void _listen(Class<D> dClass,
                                    GutoolDDDMethodAroundType aroundType,
                                    Consumer<GutoolDDDMethodEvent<D>> consumer,
                                    Serializable fn) {
        Dami.<GutoolDDDMethodEvent<D>, GutoolDDDMethodEvent<D>>bus().listen("gutool." + dClass.getName() + "." +
                GutoolDDDLambda.getMethodName(fn) + "." + aroundType.name().toLowerCase(), (payload) -> {
            consumer.accept(payload.getContent());
            payload.reply(payload.getContent());
        });
    }

}
