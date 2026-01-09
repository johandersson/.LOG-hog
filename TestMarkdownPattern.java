import java.util.regex.Pattern;

public class TestMarkdownPattern {
    public static void main(String[] args) {
        Pattern HAS_MARKDOWN_PATTERN = Pattern.compile("[\\[*`<#]");
        
        String[] testStrings = {
            "Plain text with no markdown",
            "Text with [link](http://example.com)",
            "Text with **bold**",
            "Text with *italic*",
            "Text with `code`",
            "Text with <span>html</span>",
            "Text with # heading"
        };
        
        for (String test : testStrings) {
            boolean hasMarkdown = HAS_MARKDOWN_PATTERN.matcher(test).find();
            System.out.println(hasMarkdown + " : " + test);
        }
    }
}
