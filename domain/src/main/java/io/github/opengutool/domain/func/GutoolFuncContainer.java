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

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 * 函数容器
 */
public final class GutoolFuncContainer {
    private static final Map<String, GutoolFunc> GUTOOL_FUNC_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, GutoolFunc> GUTOOL_ID_FUNC_MAP = new ConcurrentHashMap<>();

    public static boolean existName(String funcName) {
        return GUTOOL_FUNC_MAP.containsKey(funcName);
    }


    public static String getFuncContentByName(String funcName) {
        GutoolFunc gutoolFunc = GUTOOL_FUNC_MAP.get(funcName);
        if (Objects.nonNull(gutoolFunc)) {
            return gutoolFunc.getContent();
        }
        return null;
    }
    public static String getFuncContentById(Long funcId) {
        GutoolFunc gutoolFunc = GUTOOL_ID_FUNC_MAP.get(funcId);
        if (Objects.nonNull(gutoolFunc)) {
            return gutoolFunc.getContent();
        }
        return null;
    }


    public static GutoolFunc getFuncById(Long funcId) {
        return GUTOOL_ID_FUNC_MAP.get(funcId);
    }

    public static void putFunc(GutoolFunc gutoolFunc) {
        GUTOOL_FUNC_MAP.put(StrUtil.toString(gutoolFunc.getName()), gutoolFunc);
        GUTOOL_ID_FUNC_MAP.put(gutoolFunc.getId(), gutoolFunc);
    }
}
