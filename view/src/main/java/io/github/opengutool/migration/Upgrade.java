package io.github.opengutool.migration;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.opengutool.GutoolApp;
import io.github.opengutool.views.UiConsts;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 * 更新升级工具类
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
@Slf4j
public class Upgrade {
    public static void startMigration() {
        // 取得当前版本
        String currentVersion = UiConsts.APP_VERSION;
        // 取得升级前版本
        String beforeVersion = GutoolApp.config.getBeforeVersion();
        if (!StrUtil.equals(currentVersion, beforeVersion)) {
            if (NumberUtil.toBigDecimal(currentVersion.replaceAll("\\D", ""))
                    .compareTo(NumberUtil.toBigDecimal(beforeVersion.replaceAll("\\D", ""))) <= 0) {
                return;
            }
            log.info("升级开始");
            FlywayDb.startMigration();
            log.info("旧版本{}", beforeVersion);
            log.info("当前版本{}", currentVersion);
            // 升级完毕且成功，则赋值升级前版本号为当前版本
            GutoolApp.config.setBeforeVersion(currentVersion);
            GutoolApp.config.save();
            log.info("升级结束");
        }
    }
}
