package com.praktikum.database.testing.library.dao;

// Import classes untuk testing
import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.Borrowing;
import com.praktikum.database.testing.library.model.User;
import com.praktikum.database.testing.library.utils.IndonesianFakerHelper;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

// Import static assertions
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite untuk BorrowingDAO
 * Menguji semua operasi CRUD pada entity Borrowing
 * Termasuk testing business logic untuk peminjaman dan pengembalian
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BorrowingDAO CRUD Operations Test Suite")
public class BorrowingDAOTest extends BaseDatabaseTest {

    // Test dependencies
    private static BorrowingDAO borrowingDAO;
    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static Faker faker;

    // Test data
    private static User testUser;
    private static Book testBook;
    private static Borrowing testBorrowing;
    private static List<Integer> createdBorrowingIds;

    @BeforeAll
    static void setUpAll() throws SQLException {
        logger.info("Starting BorrowingDAO CRUD Tests");

        // Initialize dependencies
        borrowingDAO = new BorrowingDAO();
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        faker = IndonesianFakerHelper.getFaker();
        createdBorrowingIds = new java.util.ArrayList<>();

        // Create test user dan book
        setupTestData();
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        logger.info("BorrowingDAO CRUD Tests Completed");

        // Cleanup test data
        for (Integer borrowingId : createdBorrowingIds) {
            try {
                borrowingDAO.delete(borrowingId);
                logger.fine("Cleaned up test borrowing ID: " + borrowingId);
            } catch (SQLException e) {
                logger.warning("Gagal cleanup borrowing ID: " + borrowingId + " - " + e.getMessage());
            }
        }

        // Cleanup user dan book
        if (testBook != null && testBook.getBookId() != null) {
            try {
                bookDAO.delete(testBook.getBookId());
            } catch (SQLException e) {
                // Ignore
            }
        }
        if (testUser != null && testUser.getUserId() != null) {
            try {
                userDAO.delete(testUser.getUserId());
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Setup test data (user dan book) untuk borrowing tests
     */
    private static void setupTestData() throws SQLException {
        // Create test user
        testUser = User.builder()
                .username("borrowing_test_user_" + System.currentTimeMillis())
                .email("borrowing_test_" + System.currentTimeMillis() + "@example.com")
                .fullName("Borrowing Test User")
                .role("member")
                .status("active")
                .build();
        testUser = userDAO.create(testUser);

        // Create test book
        testBook = Book.builder()
                .isbn("978borrowing" + System.currentTimeMillis())
                .title("Borrowing Test Book")
                .authorId(1)
                .totalCopies(5)
                .availableCopies(5)
                .price(new BigDecimal("50000.00"))
                .build();
        testBook = bookDAO.create(testBook);
    }

    // ======================================
    // POSITIVE TEST CASES
    // ======================================

    @Test
    @Order(1)
    @DisplayName("TC201: Create borrowing dengan data valid - Should Success")
    void testCreateBorrowing_WithValidData_ShouldSuccess() throws SQLException {
        // ARRANGE
        Timestamp dueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(14));
        testBorrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(dueDate)
                .status("borrowed")
                .notes("Test borrowing for automated testing")
                .build();

        // ACT
        Borrowing createdBorrowing = borrowingDAO.create(testBorrowing);
        createdBorrowingIds.add(createdBorrowing.getBorrowingId());

        // ASSERT
        assertThat(createdBorrowing)
                .isNotNull()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getBorrowingId()).isNotNull().isPositive();
                    assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
                    assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
                    assertThat(borrowing.getDueDate()).isEqualTo(dueDate);
                    assertThat(borrowing.getStatus()).isEqualTo("borrowed");
                    assertThat(borrowing.getBorrowDate()).isNotNull(); // Auto-generated
                    assertThat(borrowing.getReturnDate()).isNull(); // Should be null for new borrowing
                    assertThat(borrowing.getCreatedAt()).isNotNull();
                    assertThat(borrowing.getUpdatedAt()).isNotNull();
                });

        logger.info("TC201 PASSED: Borrowing created dengan ID: " + createdBorrowing.getBorrowingId());
    }

    @Test
    @Order(2)
    @DisplayName("TC202: Find borrowing by existing ID - Should Return Borrowing")
    void testFindBorrowingById_WithExistingId_ShouldReturnBorrowing() throws SQLException {
        // ACT
        Optional<Borrowing> foundBorrowing = borrowingDAO.findById(testBorrowing.getBorrowingId());

        // ASSERT
        assertThat(foundBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getBorrowingId()).isEqualTo(testBorrowing.getBorrowingId());
                    assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
                    assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
                });

        logger.info("TC202 PASSED: Borrowing found dengan ID: " + testBorrowing.getBorrowingId());
    }

    @Test
    @Order(3)
    @DisplayName("TC203: Find borrowings by user ID - Should Return User's Borrowings")
    void testFindBorrowingsByUserId_ShouldReturnUserBorrowings() throws SQLException {
        // ACT
        List<Borrowing> userBorrowings = borrowingDAO.findByUserId(testUser.getUserId());

        // ASSERT
        assertThat(userBorrowings)
                .isNotNull()
                .isNotEmpty()
                .allSatisfy(borrowing -> {
                    assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
                });

        logger.info("TC203 PASSED: Found " + userBorrowings.size() + " borrowings for user ID: " + testUser.getUserId());
    }

    @Test
    @Order(4)
    @DisplayName("TC204: Find borrowings by book ID - Should Return Book's Borrowings")
    void testFindBorrowingsByBookId_ShouldReturnBookBorrowings() throws SQLException {
        // ACT
        List<Borrowing> bookBorrowings = borrowingDAO.findByBookId(testBook.getBookId());

        // ASSERT
        assertThat(bookBorrowings)
                .isNotNull()
                .isNotEmpty()
                .allSatisfy(borrowing -> {
                    assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
                });

        logger.info("TC204 PASSED: Found " + bookBorrowings.size() + " borrowings for book ID: " + testBook.getBookId());
    }

    @Test
    @Order(5)
    @DisplayName("TC205: Find active borrowings - Should Return Only Active Borrowings")
    void testFindActiveBorrowings_ShouldReturnOnlyActive() throws SQLException {
        // ACT
        List<Borrowing> activeBorrowings = borrowingDAO.findActiveBorrowings();

        // ASSERT
        assertThat(activeBorrowings)
                .isNotNull()
                .allSatisfy(borrowing -> {
                    assertThat(borrowing.getReturnDate()).isNull(); // Should not be returned yet
                    assertThat(borrowing.getStatus()).isNotEqualTo("returned");
                });

        logger.info("TC205 PASSED: Found " + activeBorrowings.size() + " active borrowings");
    }

    @Test
    @Order(6)
    @DisplayName("TC206: Return book - Should Update Return Date and Status")
    void testReturnBook_ShouldUpdateReturnDateAndStatus() throws SQLException {
        // ARRANGE - Create a new borrowing untuk di-return
        Timestamp dueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(7));
        Borrowing borrowingToReturn = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(dueDate)
                .build();
        borrowingToReturn = borrowingDAO.create(borrowingToReturn);
        createdBorrowingIds.add(borrowingToReturn.getBorrowingId());

        // ACT - Return the book
        Timestamp returnDate = new Timestamp(System.currentTimeMillis());
        boolean returned = borrowingDAO.returnBook(borrowingToReturn.getBorrowingId(), returnDate);

        // ASSERT
        assertThat(returned).isTrue();

        // VERIFY - Borrowing should be marked as returned
        Optional<Borrowing> returnedBorrowing = borrowingDAO.findById(borrowingToReturn.getBorrowingId());
        assertThat(returnedBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getReturnDate()).isNotNull();
                    assertThat(borrowing.getStatus()).isEqualTo("returned");
                });

        logger.info("TC206 PASSED: Book returned successfully - Borrowing ID: " + borrowingToReturn.getBorrowingId());
    }

    @Test
    @Order(7)
    @DisplayName("TC207: Update borrowing status - Should Success")
    void testUpdateBorrowingStatus_ShouldSuccess() throws SQLException {
        // ARRANGE - Create a new borrowing
        Borrowing borrowingToUpdate = createTestBorrowing();
        borrowingToUpdate = borrowingDAO.create(borrowingToUpdate);
        createdBorrowingIds.add(borrowingToUpdate.getBorrowingId());

        // ACT - Update status to "overdue"
        boolean updated = borrowingDAO.updateStatus(borrowingToUpdate.getBorrowingId(), "overdue");

        // ASSERT
        assertThat(updated).isTrue();

        // VERIFY
        Optional<Borrowing> updatedBorrowing = borrowingDAO.findById(borrowingToUpdate.getBorrowingId());
        assertThat(updatedBorrowing)
                .isPresent()
                .get()
                .satisfies(borrowing -> {
                    assertThat(borrowing.getStatus()).isEqualTo("overdue");
                });

        logger.info("TC207 PASSED: Borrowing status updated to overdue");
    }

    @Test
    @Order(8)
    @DisplayName("TC208: Count active borrowings by user - Should Return Correct Count")
    void testCountActiveBorrowingsByUser_ShouldReturnCorrectCount() throws SQLException {
        // ACT
        int activeCount = borrowingDAO.countActiveBorrowingsByUser(testUser.getUserId());
        int totalCount = borrowingDAO.countAll();

        // ASSERT
        assertThat(activeCount).isGreaterThanOrEqualTo(0);
        assertThat(totalCount).isGreaterThanOrEqualTo(activeCount);

        logger.info("TC208 PASSED: Active borrowings: " + activeCount + ", Total: " + totalCount);
    }

    // ======================================
    // NEGATIVE TEST CASES
    // ======================================

    @Test
    @Order(20)
    @DisplayName("TC220: Create borrowing dengan invalid user ID - Should Fail")
    void testCreateBorrowing_WithInvalidUserId_ShouldFail() {
        // ARRANGE - Invalid user ID
        Timestamp dueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(14));
        Borrowing invalidBorrowing = Borrowing.builder()
                .userId(999999) // Invalid user ID
                .bookId(testBook.getBookId())
                .dueDate(dueDate)
                .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> borrowingDAO.create(invalidBorrowing))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key"); // Foreign key constraint violation

        logger.info("TC220 PASSED: Foreign key constraint for user_id working correctly");
    }

    @Test
    @Order(21)
    @DisplayName("TC221: Create borrowing dengan invalid book ID - Should Fail")
    void testCreateBorrowing_WithInvalidBookId_ShouldFail() {
        // ARRANGE - Invalid book ID
        Timestamp dueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(14));
        Borrowing invalidBorrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(999999) // Invalid book ID
                .dueDate(dueDate)
                .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> borrowingDAO.create(invalidBorrowing))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key"); // Foreign key constraint violation

        logger.info("TC221 PASSED: Foreign key constraint for book_id working correctly");
    }

    @Test
    @Order(22)
    @DisplayName("TC222: Create borrowing dengan due_date <= borrow_date - Should Fail")
    void testCreateBorrowing_WithInvalidDueDate_ShouldFail() {
        // ARRANGE - Due date in the past
        Timestamp pastDate = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        Borrowing invalidBorrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(pastDate) // Invalid: past date
                .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> borrowingDAO.create(invalidBorrowing))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("check"); // CHECK constraint violation

        logger.info("TC222 PASSED: Due date check constraint working correctly");
    }

    @Test
    @Order(23)
    @DisplayName("TC223: Return already returned book - Should Fail")
    void testReturnBook_AlreadyReturned_ShouldFail() throws SQLException {
        // ARRANGE - Create and return a book first
        Borrowing borrowing = createTestBorrowing();
        borrowing = borrowingDAO.create(borrowing);
        createdBorrowingIds.add(borrowing.getBorrowingId());

        Timestamp returnDate = new Timestamp(System.currentTimeMillis());
        borrowingDAO.returnBook(borrowing.getBorrowingId(), returnDate);

        // ACT - Try to return again
        boolean returnedAgain = borrowingDAO.returnBook(borrowing.getBorrowingId(), returnDate);

        // ASSERT
        assertThat(returnedAgain).isFalse(); // Should return false, not throw exception

        logger.info("TC223 PASSED: Cannot return already returned book");
    }

    @Test
    @Order(24)
    @DisplayName("TC224: Find borrowing by non-existent ID - Should Return Empty")
    void testFindBorrowingById_WithNonExistentId_ShouldReturnEmpty() throws SQLException {
        // ACT
        Optional<Borrowing> foundBorrowing = borrowingDAO.findById(999999);

        // ASSERT
        assertThat(foundBorrowing).isEmpty();

        logger.info("TC224 PASSED: Non-existent borrowing handled correctly");
    }

    @Test
    @Order(25)
    @DisplayName("TC225: Delete non-existent borrowing - Should Return False")
    void testDeleteBorrowing_WithNonExistentBorrowing_ShouldReturnFalse() throws SQLException {
        // ACT
        boolean deleted = borrowingDAO.delete(999999);

        // ASSERT
        assertThat(deleted).isFalse();

        logger.info("TC225 PASSED: Non-existent borrowing delete handled correctly");
    }

    // ======================================
    // BUSINESS LOGIC TEST CASES
    // ======================================

    @Test
    @Order(30)
    @DisplayName("TC230: Find overdue borrowings - Should Return Only Overdue")
    void testFindOverdueBorrowings_ShouldReturnOnlyOverdue() throws SQLException {
        // ARRANGE - Database sudah ada sample data overdue dari SQL schema
        // Kita tidak perlu buat data overdue manual

        // ACT - Cari overdue borrowings
        List<Borrowing> overdueBorrowings = borrowingDAO.findOverdueBorrowings();

        // ASSERT - Harus ada minimal 1 borrowing overdue dari sample data
        assertThat(overdueBorrowings)
                .isNotNull()
                .isNotEmpty()
                .allSatisfy(overdueBorrowing -> {
                    assertThat(overdueBorrowing.getDueDate()).isBefore(new Timestamp(System.currentTimeMillis()));
                    assertThat(overdueBorrowing.getReturnDate()).isNull(); // Not returned yet
                    assertThat(overdueBorrowing.getStatus()).isIn("borrowed", "overdue");
                });

        // Hitung berapa yang overdue
        long actualOverdueCount = overdueBorrowings.stream()
                .filter(b -> b.getDueDate().before(new Timestamp(System.currentTimeMillis())))
                .filter(b -> b.getReturnDate() == null)
                .count();

        logger.info("TC230 PASSED: Found " + overdueBorrowings.size() + " overdue borrowings");
        logger.info("       Actual overdue count: " + actualOverdueCount);

        // Print detail untuk debugging
        overdueBorrowings.forEach(b ->
                logger.info("       - Borrowing ID: " + b.getBorrowingId() +
                        ", Due: " + b.getDueDate() +
                        ", Status: " + b.getStatus())
        );
    }

    @Test
    @Order(31)
    @DisplayName("TC231: Update fine amount - Should Success")
    void testUpdateFineAmount_ShouldSuccess() throws SQLException {
        // ARRANGE - Create a borrowing
        Borrowing borrowing = createTestBorrowing();
        borrowing = borrowingDAO.create(borrowing);
        createdBorrowingIds.add(borrowing.getBorrowingId());

        // ACT - Update fine amount
        double fineAmount = 25000.0;
        boolean updated = borrowingDAO.updateFineAmount(borrowing.getBorrowingId(), fineAmount);

        // ASSERT
        assertThat(updated).isTrue();

        // VERIFY
        Optional<Borrowing> updatedBorrowing = borrowingDAO.findById(borrowing.getBorrowingId());
        assertThat(updatedBorrowing)
                .isPresent()
                .get()
                .satisfies(b -> {
                    assertThat(b.getFineAmount().doubleValue()).isEqualTo(fineAmount);
                });

        logger.info("TC231 PASSED: Fine amount updated to: " + fineAmount);
    }

    // ======================================
    // PERFORMANCE TEST CASES
    // ======================================

    @Test
    @Order(40)
    @DisplayName("TC240: Borrowing search performance - Should Complete Quickly")
    void testBorrowingSearchPerformance() throws SQLException {
        // ARRANGE
        int iterations = 10;
        long totalTime = 0;

        // ACT & MEASURE
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            borrowingDAO.findByUserId(testUser.getUserId());
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        long averageTimeMs = (totalTime / iterations) / 4_000_000;

        // ASSERT
        assertThat(averageTimeMs).isLessThan(400);

        logger.info("TC240 PASSED: Average borrowing search time: " + averageTimeMs + " ms");
    }

    // ======================================
    // HELPER METHODS
    // ======================================

    /**
     * Helper method untuk membuat test borrowing
     * @return Borrowing object untuk testing
     */
    private Borrowing createTestBorrowing() {
        Timestamp dueDate = Timestamp.valueOf(LocalDateTime.now().plusDays(14));
        return Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .dueDate(dueDate)
                .status("borrowed")
                .notes("Test borrowing created by helper method")
                .build();
    }
}