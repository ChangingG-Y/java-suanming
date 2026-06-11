package cn.chinacici.service.ai.dto;

/**
 * 前端传来的历史聊天消息。
 */
public class AiHistoryMessage {
    private String role;
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
