package ar.com.rjgodoy.webhook_router.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class QueueDeclTest {

    private static final Directive MOCK_DIRECTIVE_BODY = webhook -> Result.NULL;

    @Test
    void testConstructorInitialization_bothNull() {
        QueueDecl queueDecl = new QueueDecl("testQueue", null, null, null, MOCK_DIRECTIVE_BODY);
        assertEquals("testQueue", queueDecl.getName());
        assertNull(queueDecl.getMaxTasksRetention());
        assertNull(queueDecl.getMaxDaysRetention());
        assertNull(queueDecl.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, queueDecl.getBody());
    }

    @Test
    void testConstructorInitialization_onlyTasks() {
        QueueDecl queueDecl = new QueueDecl("testQueue", 100, null, null, MOCK_DIRECTIVE_BODY);
        assertEquals("testQueue", queueDecl.getName());
        assertEquals(100, queueDecl.getMaxTasksRetention());
        assertNull(queueDecl.getMaxDaysRetention());
        assertNull(queueDecl.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, queueDecl.getBody());
    }

    @Test
    void testConstructorInitialization_onlyDays() {
        QueueDecl queueDecl = new QueueDecl("testQueue", null, 30, null, MOCK_DIRECTIVE_BODY);
        assertEquals("testQueue", queueDecl.getName());
        assertNull(queueDecl.getMaxTasksRetention());
        assertEquals(30, queueDecl.getMaxDaysRetention());
        assertNull(queueDecl.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, queueDecl.getBody());
    }

    @Test
    void testConstructorInitialization_bothTasksAndDays() {
        QueueDecl queueDecl = new QueueDecl("testQueue", 100, 30, null, MOCK_DIRECTIVE_BODY);
        assertEquals("testQueue", queueDecl.getName());
        assertEquals(100, queueDecl.getMaxTasksRetention());
        assertEquals(30, queueDecl.getMaxDaysRetention());
        assertNull(queueDecl.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, queueDecl.getBody());
    }

    @Test
    void testConstructorInitialization_TasksAndDaysWithANDCombinator() {
        QueueDecl queueDecl = new QueueDecl("testQueue", 100, 30, "AND", MOCK_DIRECTIVE_BODY);
        assertEquals("testQueue", queueDecl.getName());
        assertEquals(100, queueDecl.getMaxTasksRetention());
        assertEquals(30, queueDecl.getMaxDaysRetention());
        assertEquals("AND", queueDecl.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, queueDecl.getBody());
    }

    @Test
    void testConstructorInitialization_TasksAndDaysWithORCombinator() {
        QueueDecl queueDecl = new QueueDecl("testQueue", 120, 25, "OR", MOCK_DIRECTIVE_BODY);
        assertEquals("testQueue", queueDecl.getName());
        assertEquals(120, queueDecl.getMaxTasksRetention());
        assertEquals(25, queueDecl.getMaxDaysRetention());
        assertEquals("OR", queueDecl.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, queueDecl.getBody());
    }

    @Test
    void testCopyConstructor_bothNull() {
        QueueDecl original = new QueueDecl("origQueue", null, null, null, MOCK_DIRECTIVE_BODY);
        QueueDecl copy = new QueueDecl(original, MOCK_DIRECTIVE_BODY);

        assertEquals("origQueue", copy.getName());
        assertNull(copy.getMaxTasksRetention());
        assertNull(copy.getMaxDaysRetention());
        assertNull(copy.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, copy.getBody());
    }

    @Test
    void testCopyConstructor_onlyTasks() {
        QueueDecl original = new QueueDecl("origQueue", 200, null, null, MOCK_DIRECTIVE_BODY);
        QueueDecl copy = new QueueDecl(original, MOCK_DIRECTIVE_BODY);

        assertEquals("origQueue", copy.getName());
        assertEquals(200, copy.getMaxTasksRetention());
        assertNull(copy.getMaxDaysRetention());
        assertNull(copy.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, copy.getBody());
    }

    @Test
    void testCopyConstructor_onlyDays() {
        QueueDecl original = new QueueDecl("origQueue", null, 60, null, MOCK_DIRECTIVE_BODY);
        QueueDecl copy = new QueueDecl(original, MOCK_DIRECTIVE_BODY);

        assertEquals("origQueue", copy.getName());
        assertNull(copy.getMaxTasksRetention());
        assertEquals(60, copy.getMaxDaysRetention());
        assertNull(copy.getRetentionPolicyCombinator());
        assertEquals(MOCK_DIRECTIVE_BODY, copy.getBody());
    }

    @Test
    void testCopyConstructor_bothTasksAndDays_NullCombinator() {
        QueueDecl original = new QueueDecl("origQueue", 200, 60, null, MOCK_DIRECTIVE_BODY);
        Directive newBody = webhook -> Result.NULL;
        QueueDecl copy = new QueueDecl(original, newBody);

        assertEquals("origQueue", copy.getName());
        assertEquals(200, copy.getMaxTasksRetention());
        assertEquals(60, copy.getMaxDaysRetention());
        assertNull(copy.getRetentionPolicyCombinator());
        assertEquals(newBody, copy.getBody());
    }

    @Test
    void testCopyConstructor_bothTasksAndDays_ANDCombinator() {
        QueueDecl original = new QueueDecl("origQueue", 250, 70, "AND", MOCK_DIRECTIVE_BODY);
        Directive newBody = webhook -> Result.NULL;
        QueueDecl copy = new QueueDecl(original, newBody);

        assertEquals("origQueue", copy.getName());
        assertEquals(250, copy.getMaxTasksRetention());
        assertEquals(70, copy.getMaxDaysRetention());
        assertEquals("AND", copy.getRetentionPolicyCombinator());
        assertEquals(newBody, copy.getBody());
    }

    @Test
    void testCopyConstructor_bothTasksAndDays_ORCombinator() {
        QueueDecl original = new QueueDecl("origQueue", 300, 80, "OR", MOCK_DIRECTIVE_BODY);
        Directive newBody = webhook -> Result.NULL;
        QueueDecl copy = new QueueDecl(original, newBody);

        assertEquals("origQueue", copy.getName());
        assertEquals(300, copy.getMaxTasksRetention());
        assertEquals(80, copy.getMaxDaysRetention());
        assertEquals("OR", copy.getRetentionPolicyCombinator());
        assertEquals(newBody, copy.getBody());
    }
}
