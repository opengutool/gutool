package io.github.opengutool.domain.script;

import lombok.Data;

import java.io.Serializable;

/**
 * @author <a href="https://github.com/opengutool">gutool</a>
 * @since 2025/9/30
 */
@Data
public class GutoolScriptEvent implements Serializable {
    /**
     * default("button"), "cron", "http"
     */
    private String type;
    private Object params;
    private Long timestamp;
    private Long historyId;
    private Long funcId;
    private Long tabPanelId;
}
