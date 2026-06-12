package io.obya.api.onboarding.domain.model;

public record Score(int evaluation) {

    public static Score undefined() {
        return new Score(-1);
    }

    public Grade grade() {
        return Grade.from(evaluation);
    }

    public boolean isUndefined() {
        return evaluation < 0;
    }

    public boolean isTooLow() {
        return Grade.D.weak(evaluation);
    }

    public enum Grade {
        A(90, 100, "Highly Ready. The API is robust and ideal for AI agent tool execution"),
        B(75, 89, "Good. The API is generally useable by AI, but requires targeted adjustments for optimal reliability."),
        C(50, 74, "AI Aware. The API has basic documentation but lacks the semantic clarity or structural requirements needed for robust, autonomous agent use."),
        D(0, 49, "Needs Improvement. The API lacks crucial context, validation, or security parameters for safe agentic deployment."),
        NONE(-1, -1, "No Score");

        public final int min;
        public final int max;
        public final String text;

        Grade(int min, int max, String text) {
            this.min = min;
            this.max = max;
            this.text = text;
        }

        public static Grade from(int evaluation) {
            for (Grade grade : Grade.values()) {
                if (evaluation >= grade.min && evaluation <= grade.max) {
                    return grade;
                }
            }
            throw new IllegalArgumentException("Invalid evaluation score: " + evaluation);
        }

        public boolean strong(int evaluation) {
            return evaluation > (min + max) / 2;
        }

        public boolean weak(int evaluation) {
            return evaluation < (min + max) / 2;
        }
    }
}
