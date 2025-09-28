package io.github.opengutool.views.util;

import io.github.opengutool.repository.GutoolDbRepository;

/**
 * <pre>
 * 配置管理
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class ConfigUtil extends ConfigBaseUtil {

    private static ConfigUtil configUtil = new ConfigUtil();

    public static ConfigUtil getInstance() {
        return configUtil;
    }

    private ConfigUtil() {
        super();
    }

    private boolean autoCheckUpdate;

    private boolean defaultMaxWindow;

    private boolean unifiedBackground;

    /**
     * 主题颜色跟随系统
     */
    private boolean themeColorFollowSystem;

    private String beforeVersion;

    private String theme;

    private String font;

    private int fontSize;

    private boolean httpUseProxy;

    private String httpProxyHost;

    private String httpProxyPort;

    private String httpProxyUserName;

    private String httpProxyPassword;

    /**
     * 菜单栏位置
     */
    private String menuBarPosition;

    /**
     * 功能Tab位置
     */
    private String funcTabPosition;

    private boolean tabCompact;

    private boolean tabHideTitle;

    private boolean tabSeparator;

    private boolean tabCard;

    /**
     * sql dialect
     */
    private String sqlDialect;

    private String accentColor;

    /**
     * 上次关闭前所在的tab
     */
    private int recentTabIndex;

    private String quickNoteFontName;

    private int quickNoteFontSize;

    private String jsonBeautyFontName;

    private int jsonBeautyFontSize;

    private String currentHostName;

    private int qrCodeSize;

    private String qrCodeErrorCorrectionLevel;

    private String qrCodeLogoPath;

    private String qrCodeSaveAsPath;

    private String qrCodeRecognitionImagePath;

    private String digestFilePath;

    private int randomNumDigit;

    private int randomStringDigit;

    private int randomPasswordDigit;

    private String calculatorInputExpress;

    private String dbFilePath;

    private String dbFilePathBefore;

    private String quickNoteExportPath;

    private String jsonBeautyExportPath;

    private String imageExportPath;

    private String hostExportPath;

    private String lastSelectedColor;

    private String colorTheme;

    private String colorCodeType;

    private String regexText;

    public boolean isAutoCheckUpdate() {
        return setting.getBool("autoCheckUpdate", "setting.common", true);
    }

    public void setAutoCheckUpdate(boolean autoCheckUpdate) {
        setting.putByGroup("autoCheckUpdate", "setting.common", String.valueOf(autoCheckUpdate));
    }

    public boolean isDefaultMaxWindow() {
        return setting.getBool("defaultMaxWindow", "setting.normal", false);
    }

    public void setDefaultMaxWindow(boolean defaultMaxWindow) {
        setting.putByGroup("defaultMaxWindow", "setting.normal", String.valueOf(defaultMaxWindow));
    }

    public boolean isThemeColorFollowSystem() {
        return setting.getBool("themeColorFollowSystem", "setting.normal", true);
    }


    public String getBeforeVersion() {
        return setting.getStr("beforeVersion", "setting.common", "v1.0.0");
    }

    public void setBeforeVersion(String beforeVersion) {
        setting.putByGroup("beforeVersion", "setting.common", beforeVersion);
    }

    public String getTheme() {
        if (SystemUtil.isMacOs()) {
            return setting.getStr("theme", "setting.appearance", "Flat macOS Dark");
        } else {
            return setting.getStr("theme", "setting.appearance", "Flat Darcula");
        }
    }

    public void setTheme(String theme) {
        setting.putByGroup("theme", "setting.appearance", theme);
    }

    public String getFont() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else if (SystemUtil.isMacOs()) {
            return setting.getStr("font", "setting.appearance", "PingFang SC");
        } else {
            return setting.getStr("font", "setting.appearance", "微软雅黑");
        }
    }

    public void setFont(String font) {
        setting.putByGroup("font", "setting.appearance", font);
    }

    public int getFontSize() {
        return setting.getInt("fontSize", "setting.appearance", 12);
    }

    public void setFontSize(int fontSize) {
        setting.putByGroup("fontSize", "setting.appearance", String.valueOf(fontSize));
    }

    public String getJsonBeautyFontName() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("jsonBeautyFontName", "func.jsonBeauty", "等线");
        }
    }

    public void setJsonBeautyFontName(String jsonBeautyFontName) {
        setting.putByGroup("jsonBeautyFontName", "func.jsonBeauty", jsonBeautyFontName);
    }

    public int getJsonBeautyFontSize() {
        return setting.getInt("jsonBeautyFontSize", "func.jsonBeauty", 0);
    }


    public String getAccentColor() {
        return setting.getStr("accentColor", "setting.quickNote", "");
    }

    public void setAccentColor(String accentColor) {
        setting.putByGroup("accentColor", "setting.quickNote", accentColor);
    }

    public String getDbFilePath() {
        return setting.getStr("dbFilePath", "func.advanced", GutoolDbRepository.getDbFileParentPath());
    }

    public void setDbFilePath(String dbFilePath) {
        setting.putByGroup("dbFilePath", "func.advanced", dbFilePath);
    }

    public String getDbFilePathBefore() {
        return setting.getStr("dbFilePathBefore", "func.advanced", "");
    }

    public void setDbFilePathBefore(String dbFilePathBefore) {
        setting.putByGroup("dbFilePathBefore", "func.advanced", dbFilePathBefore);
    }

    public String getQuickNoteExportPath() {
        return setting.getStr("quickNoteExportPath", "func.quickNote", "");
    }
}
