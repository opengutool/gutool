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

import lombok.Data;
import org.junit.Test;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 * test args：--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED
 */
public class GutoolDDDFactoryTest {

    @Test
    public void testAll() {
        GutoolDDDFactory.init();
        GutoolDDDFactory.listen(GutoolVersion.class, (event) -> {
            switch (event.getAroundType()) {
                case BEFORE -> {
                    event.getSource().setCurrentVersion("v1.0.0");
                }
                case AFTER -> {
                    event.getSource().setCurrentVersion("v1.0.3");
                    // event.setEventResult("v1.0.2");
                }
            }
        }, GutoolVersion::getCurrentVersion);
        GutoolVersion source = new GutoolVersion();
        GutoolVersion gutoolVersionProxy = GutoolDDDFactory.create(source);
        System.out.println(gutoolVersionProxy.getCurrentVersion());
        System.out.println(source.getCurrentVersion());
    }


    @Test
    public void testAfter() {
        GutoolDDDFactory.init();
        GutoolDDDFactory.listen(GutoolVersion.class, GutoolDDDMethodAroundType.AFTER, (event) -> {
            switch (event.getAroundType()) {
                case BEFORE -> {
                    event.getSource().setCurrentVersion("v1.0.0");
                }
                case AFTER -> {
                    event.getSource().setCurrentVersion("v1.0.3");
                    // event.setEventResult("v1.0.2");
                }
            }
        }, GutoolVersion::getCurrentVersion);
        GutoolVersion source = new GutoolVersion();
        GutoolVersion gutoolVersionProxy = GutoolDDDFactory.create(source);
        System.out.println(gutoolVersionProxy.getCurrentVersion());
        System.out.println(source.getCurrentVersion());
    }

    @Data
    public static class GutoolVersion {
        private String currentVersion;
    }
}
