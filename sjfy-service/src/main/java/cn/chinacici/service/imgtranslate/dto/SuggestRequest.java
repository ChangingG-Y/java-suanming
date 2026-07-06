package cn.chinacici.service.imgtranslate.dto;

/** 复核阶段"AI 翻译"按钮的请求体：用户手动挑一行要 AI 给个翻译建议。 */
public class SuggestRequest {
    private int index;
    private String model;
    private String instruction;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}
