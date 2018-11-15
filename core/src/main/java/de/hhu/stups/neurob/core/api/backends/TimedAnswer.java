package de.hhu.stups.neurob.core.api.backends;

/**
 * Encapsulates an {@link Answer} with the corresponding nanoSeconds in nanoseconds
 * it took to determine it.
 */
public class TimedAnswer {
    private final Answer answer;
    private final Long nanoSeconds;
    private final String message;

    public TimedAnswer(Answer answer, Long nanoseconds) {
        this(answer, nanoseconds, "empty message received");
    }

    public TimedAnswer(Answer answer, Long nanoseconds, String message) {
        this.answer = answer;
        this.nanoSeconds = nanoseconds;
        this.message = message;
    }

    public Answer getAnswer() {
        return answer;
    }

    /**
     * @return Time needed to determine answer in nanoseconds.
     */
    public Long getNanoSeconds() {
        return nanoSeconds;
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

            boolean timingsEqual = this.nanoSeconds != null
                    ? this.nanoSeconds.equals(that.nanoSeconds)
                    : that.nanoSeconds == null;

            // Messages are just an extra information and should not contribute to equality.

            return answersEqual && timingsEqual;
        }

        return false;
    }

    @Override
    public String toString() {
        return "["
               + "answer=" + answer + ", "
               + "nanoSeconds=" + nanoSeconds
               + (message != null ? ", message=" + message : "")
               + "]";
    }
}
