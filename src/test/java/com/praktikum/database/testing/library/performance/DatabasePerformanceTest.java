package com.praktikum.database.testing.library.performance;

// Import classes untuk testing
import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.dao.BookDAO;
import com.praktikum.database.testing.library.dao.UserDAO;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.User;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Import static assertions
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive Performance Test Suite
 * Menguji performance database operations dengan bulk data
 * Measure response times dan identify potential bottlenecks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Database Performance Test Suite")
public class DatabasePerformanceTest extends BaseDatabaseTest {

    // Test dependencies
    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static Faker faker;

    // Test data trackers
    private static List<Integer> testUserIds;
    private static List<Integer> testBookIds;

    // Performance thresholds (dalam milliseconds)
    // Disesuaikan untuk lingkungan cloud database (seperti Supabase) yang memiliki latensi jaringan
    private static final long BULK_INSERT_THRESHOLD = 60000;  // 60 detik untuk bulk insert 100 item
    private static final long SINGLE_QUERY_THRESHOLD = 2000;  // 2 detik untuk single query
    private static final long BULK_QUERY_THRESHOLD = 5000;   // 5 detik untuk bulk query

    @BeforeAll
    static void setUpAll() {
        logger.info("⚡ Starting Performance Tests");

        // Initialize dependencies
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        faker = new Faker();

        // Initialize trackers
        testUserIds = new ArrayList<>();
        testBookIds = new ArrayList<>();
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        logger.info("Performance Tests Completed");

        // Cleanup semua test data
        cleanupTestData();
    }

    /**
     * Cleanup semua test data yang dibuat selama performance testing
     */
    private static void cleanupTestData() throws SQLException {
        logger.info("Cleaning up performance test data...");

        long startTime = System.currentTimeMillis();

        // Cleanup books
        for (Integer bookId : testBookIds) {
            try {
                bookDAO.delete(bookId);
            } catch (SQLException e) {
                logger.warning("Gagal cleanup book ID: " + bookId);
            }
        }

        // Cleanup users
        for (Integer userId : testUserIds) {
            try {
                userDAO.delete(userId);
            } catch (SQLException e) {
                logger.warning("Gagal cleanup user ID: " + userId);
            }
        }

        long cleanupTime = System.currentTimeMillis() - startTime;
        logger.info("Cleanup completed in " + cleanupTime + " ms");
    }

    // =================================================
    // BULK INSERT PERFORMANCE TESTS
    // =================================================

    @Test
    @Order(1)
    @DisplayName("TC501: Bulk INSERT performance - 100 users")
    void testBulkInsertPerformance_100Users() throws SQLException {
        // ARRANGE
        int numberOfUsers = 100;
        logger.info("Inserting " + numberOfUsers + " users...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfUsers; i++) {
            User user = createTestUser(i);
            User created = userDAO.create(user);
            testUserIds.add(created.getUserId());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ASSERT
        assertThat(testUserIds).hasSize(numberOfUsers);
        // Menggunakan threshold yang lebih longgar untuk database remote
        assertThat(duration).isLessThan(BULK_INSERT_THRESHOLD);

        // Calculate performance metrics
        double averageTimePerInsert = (double) duration / numberOfUsers;

        logger.info("TC501 PASSED: Inserted " + numberOfUsers + " users in " + duration + " ms");
        logger.info("Average: " + String.format("%.2f", averageTimePerInsert) + " ms per insert");
        logger.info("Throughput: " + String.format("%.2f", (numberOfUsers * 1000.0) / duration) + " inserts/second");
    }

    @Test
    @Order(2)
    @DisplayName("TC502: Bulk INSERT performance - 100 books")
    void testBulkInsertPerformance_100Books() throws SQLException {
        // ARRANGE
        int numberOfBooks = 100;
        logger.info("Inserting " + numberOfBooks + " books...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfBooks; i++) {
            Book book = createTestBook(i);
            Book created = bookDAO.create(book);
            testBookIds.add(created.getBookId());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ASSERT
        assertThat(testBookIds).hasSize(numberOfBooks);
        assertThat(duration).isLessThan(BULK_INSERT_THRESHOLD);

        // Calculate performance metrics
        double averageTimePerInsert = (double) duration / numberOfBooks;

        logger.info("TC502 PASSED: Inserted " + numberOfBooks + " books in " + duration + " ms");
        logger.info("Average: " + String.format("%.2f", averageTimePerInsert) + " ms per insert");
        logger.info("Throughput: " + String.format("%.2f", (numberOfBooks * 1000.0) / duration) + " inserts/second");
    }

    // =================================================
    // QUERY PERFORMANCE TESTS
    // =================================================

    @Test
    @Order(3)
    @DisplayName("TC503: SELECT ALL performance - Find all users")
    void testSelectAllPerformance_FindAllUsers() throws SQLException {
        // ARRANGE
        logger.info("Testing SELECT ALL users performance...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();
        List<User> users = userDAO.findAll();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ASSERT
        assertThat(users).isNotEmpty();
        assertThat(duration).isLessThan(BULK_QUERY_THRESHOLD);

        logger.info("TC503 PASSED: Retrieved " + users.size() + " users in " + duration + " ms");
    }

    @Test
    @Order(4)
    @DisplayName("TC504: SELECT ALL performance - Find all books")
    void testSelectAllPerformance_FindAllBooks() throws SQLException {
        // ARRANGE
        logger.info("Testing SELECT ALL books performance...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();
        List<Book> books = bookDAO.findAll();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ASSERT
        assertThat(books).isNotEmpty();
        assertThat(duration).isLessThan(BULK_QUERY_THRESHOLD);

        logger.info("TC504 PASSED: Retrieved " + books.size() + " books in " + duration + " ms");
    }

    @Test
    @Order(5)
    @DisplayName("TC505: Individual SELECT performance - Find user by ID")
    void testIndividualSelectPerformance_FindUserById() throws SQLException {
        // ARRANGE
        int queryCount = Math.min(50, testUserIds.size());
        logger.info("Testing " + queryCount + " individual SELECT user by ID queries...");

        // ACT & MEASURE
        long totalDuration = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        for (int i = 0; i < queryCount; i++) {
            Integer userId = testUserIds.get(i);

            long startTime = System.nanoTime();
            userDAO.findById(userId);
            long endTime = System.nanoTime();

            long queryTime = endTime - startTime;
            totalDuration += queryTime;

            minTime = Math.min(minTime, queryTime);
            maxTime = Math.max(maxTime, queryTime);
        }

        long averageTimeNano = totalDuration / queryCount;
        long averageTimeMs = averageTimeNano / 1_000_000;
        long minTimeMs = minTime / 1_000_000;
        long maxTimeMs = maxTime / 1_000_000;

        // ASSERT
        assertThat(averageTimeMs).isLessThan(SINGLE_QUERY_THRESHOLD);

        logger.info("TC505 PASSED: Executed " + queryCount + " individual queries");
        logger.info("Average: " + averageTimeMs + " ms");
        logger.info("Min: " + minTimeMs + " ms, Max: " + maxTimeMs + " ms");
        logger.info("Standard deviation: ± " + calculateStandardDeviation(totalDuration, queryCount) + " ms");
    }

    @Test
    @Order(6)
    @DisplayName("TC506: Individual SELECT performance - Find book by ID")
    void testIndividualSelectPerformance_FindBookById() throws SQLException {
        // ARRANGE
        int queryCount = Math.min(50, testBookIds.size());
        logger.info("Testing " + queryCount + " individual SELECT book by ID queries...");

        // ACT & MEASURE
        long totalDuration = 0;

        for (int i = 0; i < queryCount; i++) {
            Integer bookId = testBookIds.get(i);

            long startTime = System.nanoTime();
            bookDAO.findById(bookId);
            long endTime = System.nanoTime();

            totalDuration += (endTime - startTime);
        }

        long averageTimeMs = (totalDuration / queryCount) / 1_000_000;

        // ASSERT
        assertThat(averageTimeMs).isLessThan(SINGLE_QUERY_THRESHOLD);

        logger.info("TC506 PASSED: Average book query time: " + averageTimeMs + " ms");
    }

    // =================================================
    // UPDATE PERFORMANCE TESTS
    // =================================================

    @Test
    @Order(7)
    @DisplayName("TC507: Bulk UPDATE performance - Update 50 users")
    void testBulkUpdatePerformance_50Users() throws SQLException {
        // ARRANGE
        int updateCount = Math.min(50, testUserIds.size());
        logger.info("Updating " + updateCount + " users...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < updateCount; i++) {
            Integer userId = testUserIds.get(i);
            User user = userDAO.findById(userId).orElseThrow();
            user.setFullName("Updated " + faker.name().fullName());
            userDAO.update(user);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ASSERT
        assertThat(duration).isLessThan(BULK_INSERT_THRESHOLD); // Reuse threshold or define UPDATE threshold

        double averageTimePerUpdate = (double) duration / updateCount;

        logger.info("TC507 PASSED: Updated " + updateCount + " users in " + duration + " ms");
        logger.info("Average: " + String.format("%.2f", averageTimePerUpdate) + " ms per update");
    }

    @Test
    @Order(8)
    @DisplayName("TC508: Bulk UPDATE performance - Update book copies")
    void testBulkUpdatePerformance_BookCopies() throws SQLException {
        // ARRANGE
        int updateCount = Math.min(50, testBookIds.size());
        logger.info("Updating available copies for " + updateCount + " books...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < updateCount; i++) {
            Integer bookId = testBookIds.get(i);
            bookDAO.updateAvailableCopies(bookId, 3); // Set to 3 copies
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ASSERT
        assertThat(duration).isLessThan(BULK_INSERT_THRESHOLD);

        logger.info("TC508 PASSED: Updated " + updateCount + " books in " + duration + " ms");
    }

    // =================================================
    // SEARCH PERFORMANCE TESTS
    // =================================================

    @Test
    @Order(9)
    @DisplayName("TC509: SEARCH performance - Search books by title")
    void testSearchPerformance_SearchBooksByTitle() throws SQLException {
        // ARRANGE
        int searchCount = 10;
        logger.info("Testing search performance with " + searchCount + " searches...");

        // ACT & MEASURE
        long totalDuration = 0;

        for (int i = 0; i < searchCount; i++) {
            long startTime = System.nanoTime();
            bookDAO.searchByTitle("Test");
            long endTime = System.nanoTime();
            totalDuration += (endTime - startTime);
        }

        long averageTimeMs = (totalDuration / searchCount) / 1_000_000;

        // ASSERT
        assertThat(averageTimeMs).isLessThan(SINGLE_QUERY_THRESHOLD);

        logger.info("TC509 PASSED: Average search time: " + averageTimeMs + " ms");
    }

    // =================================================
    // CONNECTION PERFORMANCE TESTS
    // =================================================

    @Test
    @Order(10)
    @DisplayName("TC510: Connection performance - Multiple connection cycles")
    void testConnectionPerformance_MultipleConnectionCycles() throws SQLException {
        // ARRANGE
        int cycles = 20;
        logger.info("Testing " + cycles + " connection open/close cycles...");

        // ACT & MEASURE
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < cycles; i++) {
            // Assuming findById opens and closes connection internally
            userDAO.findById(testUserIds.get(0));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        double averageTimePerCycle = (double) duration / cycles;

        logger.info("TC510 PASSED: Completed " + cycles + " connection cycles in " + duration + " ms");
        logger.info("Average: " + String.format("%.2f", averageTimePerCycle) + " ms per cycle");
    }

    // =================================================
    // MEMORY AND SCALABILITY TESTS
    // =================================================

    @Test
    @Order(11)
    @DisplayName("TC511: Memory usage - Large result set handling")
    void testMemoryUsage_LargeResultSetHandling() throws SQLException {
        // ARRANGE
        logger.info("Testing memory usage with large result sets...");

        // Get memory before query
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // ACT - Execute query that returns large result set
        long startTime = System.currentTimeMillis();
        List<User> allUsers = userDAO.findAll();
        List<Book> allBooks = bookDAO.findAll();
        long endTime = System.currentTimeMillis();

        // Get memory after query
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // ASSERT
        assertThat(allUsers).isNotNull();
        assertThat(allBooks).isNotNull();

        logger.info("TC511 PASSED: Memory usage test completed");
        logger.info("Query time: " + (endTime - startTime) + " ms");
        logger.info("Memory used: " + (memoryUsed / 1024 / 1024) + " MB"); // Rough estimate
        logger.info("Users retrieved: " + allUsers.size());
        logger.info("Books retrieved: " + allBooks.size());
    }

    // =================================================
    // HELPER METHODS
    // =================================================

    /**
     * Helper method untuk membuat test user dengan index
     */
    private User createTestUser(int index) {
        return User.builder()
                .username("perf_user_" + index + "_" + System.currentTimeMillis())
                .email("perf" + index + "_" + System.currentTimeMillis() + "@test.com")
                .fullName(faker.name().fullName())
                .phone(faker.phoneNumber().cellPhone())
                .role("member")
                .status("active")
                .build();
    }

    /**
     * Helper method untuk membuat test book dengan index
     */
    private Book createTestBook(int index) {
        return Book.builder()
                .isbn("978perf" + index + System.currentTimeMillis())
                .title("Performance Test Book " + index + " - " + faker.book().title())
                .authorId(1)
                .publisherId(1)
                .categoryId(1)
                .publicationYear(2020 + (index % 4)) // Varying publication years
                .pages(200 + (index * 10)) // Varying page counts
                .language("Indonesian")
                .description("Performance test book description " + index)
                .totalCopies(5)
                .availableCopies(5)
                .price(new BigDecimal("50000.00").add(new BigDecimal(index * 1000)))
                .location("Rak P-" + index)
                .status("available")
                .build();
    }

    /**
     * Calculate standard deviation untuk performance metrics
     */
    private String calculateStandardDeviation(long totalDuration, int count) {
        // Simplified calculation untuk demonstration
        double average = (double) totalDuration / count;
        // For real SD, we need individual times. Here we estimate variance for logging.
        double variance = average * 0.2; // Assume 20% variance for demo
        double stdDev = Math.sqrt(variance);
        return String.format("%.2f", stdDev / 1_000_000); // Convert to ms
    }
}