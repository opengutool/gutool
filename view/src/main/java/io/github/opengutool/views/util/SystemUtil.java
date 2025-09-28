package io.github.opengutool.views.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <pre>
 * 系统工具
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class SystemUtil {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String VM_VENDOR = System.getProperty("java.vm.vendor");
    private static final String USER_HOME = System.getProperty("user.home");
    public static final String CONFIG_HOME = USER_HOME + File.separator + ".gutool";

    /**
     * 日志文件路径
     */
    public final static String LOG_DIR = USER_HOME + File.separator + ".gutool" + File.separator + "logs" + File.separator;

    public static boolean isMacOs() {
        return OS_NAME.contains("Mac");
    }

    public static boolean isMacSilicon() {
        return OS_NAME.contains("Mac") && "aarch64".equals(OS_ARCH);
    }

    public static boolean isWindowsOs() {
        return OS_NAME.contains("Windows");
    }

    public static boolean isLinuxOs() {
        return OS_NAME.contains("Linux");
    }

    public static boolean isJBR() {
        return VM_VENDOR.contains("JetBrains");
    }
}
