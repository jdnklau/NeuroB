package de.hhu.stups.neurob.core.api.backends;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimedAnswerTest {

    @Test
    void shouldBeEqual() {
        TimedAnswer first = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer second = new TimedAnswer(Answer.VALID, 200L);

        assertTrue(first.equals(second));
    }

    @Test
    void shouldBeEqualWhenMessagesDiffer() {
        TimedAnswer first = new TimedAnswer(Answer.VALID, 200L, "first message");
        TimedAnswer second = new TimedAnswer(Answer.VALID, 200L, "second message");

        assertTrue(first.equals(second));
    }

    @Test
    void shouldBeUnequalWhenAnswersDontMatch() {
        TimedAnswer first = new TimedAnswer(Answer.INVALID, 200L);
        TimedAnswer second = new TimedAnswer(Answer.UNKNOWN, 200L);

        assertFalse(first.equals(second));
    }

    @Test
    void shouldBeUnequalWhenTimingsDontMatch() {
        TimedAnswer first = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer second = new TimedAnswer(Answer.VALID, 400L);

        assertFalse(first.equals(second));
    }

    @Test
    void shouldTranslateToSeconds() {
        TimedAnswer answer = new TimedAnswer(Answer.VALID, 2000000000L);

        Long expected = 2L;
        Long actual = answer.getTime(TimeUnit.SECONDS);

        assertEquals(expected, actual);
    }

    @Test
    void shouldTranslateToMilliSeconds() {
        TimedAnswer answer = new TimedAnswer(Answer.VALID, 2000000000L);

        Long expected = 2000L;
        Long actual = answer.getTime(TimeUnit.MILLISECONDS);

        assertEquals(expected, actual);
    }

}
