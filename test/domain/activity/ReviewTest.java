///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/activity/Review.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for Review domain entity.
 */
public class ReviewTest {

    @Test
    @DisplayName("Review record stores all fields correctly")
    void reviewStoresAllFields() {
        Review review = new Review(
            42,
            "APPROVED",
            "Looks good to me!",
            "https://github.com/owner/repo/pull/42#pullrequestreview-123",
            "owner/repo"
        );

        assertEquals(42, review.prNumber());
        assertEquals("APPROVED", review.state());
        assertEquals("Looks good to me!", review.body());
        assertEquals("https://github.com/owner/repo/pull/42#pullrequestreview-123", review.url());
        assertEquals("owner/repo", review.repository());
    }

    @Test
    @DisplayName("Review with null optional fields is valid")
    void reviewWithNullOptionalFields() {
        Review review = new Review(1, "COMMENTED", null, null, null);

        assertNull(review.body());
        assertNull(review.url());
        assertNull(review.repository());
    }

    @Test
    @DisplayName("Review with empty body is valid")
    void reviewWithEmptyBody() {
        Review review = new Review(1, "APPROVED", "", null, null);

        assertEquals("", review.body());
    }

    @Test
    @DisplayName("Two reviews with same data are equal")
    void reviewsWithSameDataAreEqual() {
        Review review1 = new Review(1, "APPROVED", "LGTM", "url", "repo");
        Review review2 = new Review(1, "APPROVED", "LGTM", "url", "repo");

        assertEquals(review1, review2);
        assertEquals(review1.hashCode(), review2.hashCode());
    }

    @Test
    @DisplayName("Two reviews with different PR numbers are not equal")
    void reviewsWithDifferentPrNumbersAreNotEqual() {
        Review review1 = new Review(1, "APPROVED", "LGTM", "url", "repo");
        Review review2 = new Review(2, "APPROVED", "LGTM", "url", "repo");

        assertNotEquals(review1, review2);
    }

    @Test
    @DisplayName("Review state values are valid")
    void reviewStateValues() {
        Review approved = new Review(1, "APPROVED", null, null, null);
        Review changesRequested = new Review(2, "CHANGES_REQUESTED", null, null, null);
        Review commented = new Review(3, "COMMENTED", null, null, null);
        Review pending = new Review(4, "PENDING", null, null, null);

        assertEquals("APPROVED", approved.state());
        assertEquals("CHANGES_REQUESTED", changesRequested.state());
        assertEquals("COMMENTED", commented.state());
        assertEquals("PENDING", pending.state());
    }

    @Test
    @DisplayName("Review toString contains useful info")
    void reviewToStringContainsInfo() {
        Review review = new Review(42, "APPROVED", "LGTM", null, "owner/repo");

        String str = review.toString();
        assertTrue(str.contains("42"), "toString should contain PR number");
        assertTrue(str.contains("APPROVED"), "toString should contain state");
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(ReviewTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
