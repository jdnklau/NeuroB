package de.hhu.stups.neurob.core.api.backends;

/**
 * Encapsulates an {@link Answer} with the corresponding time it took
 * to determine it.
 */
public class TimedAnswer {
    private final Answer answer;
    private final Long time;
    private final String message;

    public TimedAnswer(Answer answer, Long time) {
        this(answer, time, "empty message received");
    }

    public TimedAnswer(Answer answer, Long time, String message) {

        this.answer = answer;
        this.time = time;
        this.message = message;
    }

    public Answer getAnswer() {
        return answer;
    }

    /**
     * @return Time needed to determine answer in nanoseconds.
     */
    public Long getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }
}
