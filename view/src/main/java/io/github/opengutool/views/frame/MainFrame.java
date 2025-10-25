package io.github.opengutool.views.frame;

import io.github.opengutool.views.UiConsts;
import io.github.opengutool.views.component.TopMenuBar;
import io.github.opengutool.views.util.ComponentUtil;
import io.github.opengutool.views.util.MacWindowUtil;

import javax.swing.*;

/**
 * <pre>
 * 主窗口
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -332963894416012132L;

    public static TopMenuBar topMenuBar;

    public void init() {
        this.setName(UiConsts.APP_NAME);
        this.setTitle(UiConsts.APP_NAME);
        FrameUtil.setFrameIcon(this);

        MacWindowUtil.configureMacFullscreenContent(this);

        topMenuBar = TopMenuBar.getInstance();
        topMenuBar.init();
        setJMenuBar(topMenuBar);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.9, 0.88);
    }
}
