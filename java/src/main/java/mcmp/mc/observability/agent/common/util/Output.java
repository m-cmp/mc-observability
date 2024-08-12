package mcmp.mc.observability.agent.common.util;

public class Output {

    private String text = "";

    public String getText() {
        return text;
    }

    public void setText(String result) {
        this.text = result;
    }

    public void clearText() {
        this.text = "";
    }

}