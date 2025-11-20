package vn.uth.edufinai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OutputGuard {

    private static final Pattern CC_PATTERN = Pattern.compile("\\b\\d{13,19}\\b");
    private static final Pattern NINE_DIGIT_PATTERN = Pattern.compile("\\b\\d{9}\\b");
    private static final List<String> PROFANE_WORDS = List.of("đm", "dm", "cc", "shit", "fuck");
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
            "(?i)" + String.join("|", PROFANE_WORDS.stream()
                    .map(Pattern::quote)
                    .toList())
    );
    private static final String MASKED_CC = "####-MASKED";
    private static final String MASKED_SSN = "###-MASKED";
    private static final String MASKED_PROFANITY = "***";

    public SanitizeResult filterViolations(String text) {
        if (text == null) {
            return new SanitizeResult("", false, List.of());
        }
        String working = text;
        List<String> flags = new ArrayList<>();

        if (CC_PATTERN.matcher(working).find()) {
            working = CC_PATTERN.matcher(working).replaceAll(MASKED_CC);
            flags.add("possible_cc_masked");
        }
        if (NINE_DIGIT_PATTERN.matcher(working).find()) {
            working = NINE_DIGIT_PATTERN.matcher(working).replaceAll(MASKED_SSN);
            flags.add("possible_ssn_masked");
        }
        if (PROFANITY_PATTERN.matcher(working).find()) {
            working = PROFANITY_PATTERN.matcher(working).replaceAll(MASKED_PROFANITY);
            flags.add("profanity_masked");
        }
        boolean violated = !flags.isEmpty();
        return new SanitizeResult(working, violated, flags);
    }

    public static class SanitizeResult {
        public final String sanitizedText;
        public final boolean violated;
        public final List<String> flags;

        public SanitizeResult(String sanitizedText, boolean violated, List<String> flags) {
            this.sanitizedText = sanitizedText;
            this.violated = violated;
            this.flags = flags;
        }
    }
}
