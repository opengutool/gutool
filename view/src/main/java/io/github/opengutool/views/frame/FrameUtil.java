package io.github.opengutool.views.frame;

import com.google.common.collect.Lists;
import io.github.opengutool.views.UiConsts;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * FrameUtil
 * </pre>
 *
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/3
 */
public class FrameUtil {

    public static void setFrameIcon(JFrame jFrame) {
        List<Image> images = Lists.newArrayList();
        images.add(UiConsts.IMAGE_LOGO_64);
        jFrame.setIconImages(images);
    }
}
