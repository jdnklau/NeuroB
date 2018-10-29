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

    @Override
    public boolean equals(Object o) {
        if (o instanceof TimedAnswer) {
            TimedAnswer that = (TimedAnswer) o;

            boolean answersEqual = this.answer != null
                    ? this.answer.equals(that.answer)
                    : that.answer == null;

            boolean timingsEqual = this.time != null
                    ? this.time.equals(that.time)
                    : that.time == null;

            // Messages are just an extra information and should not contribute to equality.

            return answersEqual && timingsEqual;
        }

        return false;
    }

    @Override
    public String toString() {
        return "["
               + "answer=" + answer + ", "
               + "time=" + time
                + (message != null ? ", message=" + message : "")
                + "]";
    }
}
