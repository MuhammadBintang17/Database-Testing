package com.praktikum.database.testing.library.integration;

// Import classes untuk testing
import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.dao.BookDAO;
import com.praktikum.database.testing.library.dao.BorrowingDAO;
import com.praktikum.database.testing.library.dao.UserDAO;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.Borrowing;
import com.praktikum.database.testing.library.model.User;
import com.praktikum.database.testing.library.service.BorrowingService;
import com.praktikum.database.testing.library.utils.IndonesianFakerHelper;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Import static assertions
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive Integration Test Suite
 * Menguji integrasi antara User, Book, Borrowing, dan Service layer
 * Focus pada complete workflows dan business processes
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Borrowing Integration Test Suite")
public class BorrowingIntegrationTest extends BaseDatabaseTest {

    // Test dependencies
    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static BorrowingDAO borrowingDAO;
    private static BorrowingService borrowingService;
    private static Faker faker;

    // Test data
    private static User testUser;
    private static Book testBook;

    @BeforeAll
    static void setUpAll() {
        logger.info("Starting Integration Tests");

        // Initialize semua dependencies
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        borrowingDAO = new BorrowingDAO();
        borrowingService = new BorrowingService(userDAO, bookDAO, borrowingDAO);
        faker = IndonesianFakerHelper.getFaker();
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Setup test data untuk setiap test (test isolation)
        setupTestData();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Cleanup test data setelah setiap test
        cleanupTestData();
    }

    @AfterAll
    static void tearDownAll() {
        logger.info("Integration Tests Completed");
    }

    /**
     * Setup test data untuk setiap test
     */
    private void setupTestData() throws SQLException {
        // Create test user
        testUser = User.builder()
                .username("integ_user_" + System.currentTimeMillis())
                .email(IndonesianFakerHelper.generateIndonesianEmail())
                .fullName(IndonesianFakerHelper.generateIndonesianName())
                .phone(IndonesianFakerHelper.generateIndonesianPhone())
                .role("member")
                .status("active")
                .build();
        testUser = userDAO.create(testUser);

        // Create test book
        testBook = Book.builder()
                .isbn("978integ" + System.currentTimeMillis())
                .title("Buku Integration Test - " + faker.book().title())
                .authorId(1)
                .publisherId(1)
                .categoryId(1)
                .publicationYear(2023)
                .pages(300)
                .language("Indonesia")
                .description("Test book description")
                .totalCopies(5)
                .availableCopies(5)
                .price(new BigDecimal("85000.00"))
                .location("Rak Integ")
                .status("available")
                .build();
        testBook = bookDAO.create(testBook);

        logger.info("Test data created - User: " + testUser.getFullName() + ", Book: " + testBook.getTitle());
    }

    /**
     * Cleanup test data setelah setiap test.
     * Menghapus seluruh borrowings yang terkait user atau buku test.
     */
    private void cleanupTestData() throws SQLException {
        // List untuk menampung semua bookId yang perlu dihapus
        Set<Integer> booksToDelete = new HashSet<>();

        // 1. Hapus borrowings milik testUser
        if (testUser != null && testUser.getUserId() != null) {
            List<Borrowing> userBorrowings = borrowingDAO.findByUserId(testUser.getUserId());
            for (Borrowing b : userBorrowings) {
                booksToDelete.add(b.getBookId()); // tandai buku yg terlibat
                try {
                    borrowingDAO.delete(b.getBorrowingId());
                } catch (SQLException e) {
                    logger.warning("Gagal hapus borrowing user: " + e.getMessage());
                }
            }
        }

        // 2. Hapus borrowings milik testBook (kalau ada)
        if (testBook != null && testBook.getBookId() != null) {
            List<Borrowing> bookBorrowings = borrowingDAO.findByBookId(testBook.getBookId());
            for (Borrowing b : bookBorrowings) {
                booksToDelete.add(b.getBookId());
                try {
                    borrowingDAO.delete(b.getBorrowingId());
                } catch (SQLException e) {
                    logger.warning("Gagal hapus borrowing testBook: " + e.getMessage());
                }
            }
            // pastikan testBook terdaftar untuk dihapus
            booksToDelete.add(testBook.getBookId());
        }

        // 3. Hapus semua books yang berhasil kita kumpulkan
        for (Integer bookId : booksToDelete) {
            if (bookId == null) continue;
            try {
                bookDAO.delete(bookId);
            } catch (SQLException e) {
                logger.warning("Gagal hapus buku (ID: " + bookId + "): " + e.getMessage());
            }
        }

        // 4. Hapus user
        if (testUser != null && testUser.getUserId() != null) {
            try {
                userDAO.delete(testUser.getUserId());
            } catch (SQLException e) {
                logger.warning("Gagal hapus user: " + e.getMessage());
            }
        }

        logger.info("Test data cleaned up (User, Books, Borrowings)");
    }

    // =========================================
    // COMPLETE WORKFLOW INTEGRATION TESTS
    // =========================================

    @Test
    @Order(1)
    @DisplayName("TC401: Complete borrowing workflow - Success scenario")
    void testCompleteBorrowingWorkflow_SuccessScenario() throws SQLException {
        // ARRANGE - Get initial state
        int originalAvailableCopies = testBook.getAvailableCopies();
        int originalActiveBorrowings = borrowingDAO.countActiveBorrowingsByUser(testUser.getUserId());

        // ACT - Borrow book menggunakan service layer
        Borrowing borrowing = borrowingService.borrowBook(
                testUser.getUserId(),
                testBook.getBookId(),
                14 // Borrow for 14 days
        );

        // ASSERT - Borrowing created successfully
        assertThat(borrowing).isNotNull();
        assertThat(borrowing.getBorrowingId()).isNotNull();
        assertThat(borrowing.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(borrowing.getBookId()).isEqualTo(testBook.getBookId());
        assertThat(borrowing.getStatus()).isEqualTo("borrowed");
        assertThat(borrowing.getReturnDate()).isNull();

        // VERIFY - Book available copies decreased
        Optional<Book> updatedBook = bookDAO.findById(testBook.getBookId());
        assertThat(updatedBook)
                .isPresent()
                .get()
                .satisfies(book -> {
                    assertThat(book.getAvailableCopies()).isEqualTo(originalAvailableCopies - 1);
                });

        // VERIFY - User active borrowings increased
        int newActiveBorrowings = borrowingDAO.countActiveBorrowingsByUser(testUser.getUserId());
        assertThat(newActiveBorrowings).isEqualTo(originalActiveBorrowings + 1);

        logger.info("TC401 PASSED: Complete borrowing workflow successful");
    }

    @Test
    @Order(2)
    @DisplayName("TC402: Complete return workflow - Success scenario")
    void testCompleteReturnWorkflow_SuccessScenario() throws SQLException {
        // ARRANGE - Borrow book first
        Borrowing borrowing = borrowingService.borrowBook(
                testUser.getUserId(),
                testBook.getBookId(),
                14
        );

        Optional<Book> bookAfterBorrow = bookDAO.findById(testBook.getBookId());
        int copiesAfterBorrow = bookAfterBorrow.get().getAvailableCopies();

        // ACT - Return book menggunakan service layer
        boolean returned = borrowingService.returnBook(borrowing.getBorrowingId());

        // ASSERT - Return successful
        assertThat(returned).isTrue();

        // VERIFY - Borrowing updated
        Optional<Borrowing> returnedBorrowing = borrowingDAO.findById(borrowing.getBorrowingId());
        assertThat(returnedBorrowing)
                .isPresent()
                .get()
                .satisfies(b -> {
                    assertThat(b.getReturnDate()).isNotNull();
                    assertThat(b.getStatus()).isEqualTo("returned");
                });

        // VERIFY - Available copies increased
        Optional<Book> bookAfterReturn = bookDAO.findById(testBook.getBookId());
        assertThat(bookAfterReturn)
                .isPresent()
                .get()
                .satisfies(book -> {
                    assertThat(book.getAvailableCopies()).isEqualTo(copiesAfterBorrow + 1);
                });

        logger.info("TC402 PASSED: Complete return workflow successful");
        logger.info("Return date: " + returnedBorrowing.get().getReturnDate());
        logger.info("Available copies: " + copiesAfterBorrow + " -> " + bookAfterReturn.get().getAvailableCopies());
    }

    @Test
    @Order(3)
    @DisplayName("TC403: Borrow book dengan inactive user - Should Fail")
    void testBorrowBook_WithInactiveUser_ShouldFail() throws SQLException {
        // ARRANGE - Set user to inactive
        testUser.setStatus("inactive");
        userDAO.update(testUser);

        // ACT & ASSERT
        assertThatThrownBy(() ->
                borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active")
                .hasMessageContaining("inactive");

        logger.info("TC403 PASSED: Inactive user cannot borrow books");

        // RESTORE - Set user back to active untuk tests berikutnya
        testUser.setStatus("active");
        userDAO.update(testUser);
    }

    @Test
    @Order(4)
    @DisplayName("TC404: Borrow unavailable book - Should Fail")
    void testBorrowBook_UnavailableBook_ShouldFail() throws SQLException {
        // ARRANGE - Set available copies to 0
        bookDAO.updateAvailableCopies(testBook.getBookId(), 0);

        // ACT & ASSERT
        assertThatThrownBy(() ->
                borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No copies available");

        logger.info("TC404 PASSED: Cannot borrow unavailable book");

        // RESTORE
        bookDAO.updateAvailableCopies(testBook.getBookId(), 5);
    }

    @Test
    @Order(5)
    @DisplayName("TC405: Return already returned book - Should Fail")
    void testReturnBook_AlreadyReturned_ShouldFail() throws SQLException {
        // ARRANGE - Borrow and return book
        Borrowing borrowing = borrowingService.borrowBook(
                testUser.getUserId(),
                testBook.getBookId(),
                14
        );
        borrowingService.returnBook(borrowing.getBorrowingId());

        // ACT & ASSERT - Try to return again
        assertThatThrownBy(() ->
                borrowingService.returnBook(borrowing.getBorrowingId())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already returned");

        logger.info("TC405 PASSED: Cannot return already returned book");
    }

    @Test
    @Order(6)
    @DisplayName("TC406: Multiple borrowings by same user - Should Success")
    void testMultipleBorrowings_BySameUser_ShouldSuccess() throws SQLException {
        // ARRANGE - Create second book
        Book book2 = createTestBook();
        book2 = bookDAO.create(book2);

        // ACT - Borrow multiple books
        Borrowing borrowing1 = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14);
        Borrowing borrowing2 = borrowingService.borrowBook(testUser.getUserId(), book2.getBookId(), 7);

        // ASSERT
        List<Borrowing> userBorrowings = borrowingDAO.findByUserId(testUser.getUserId());
        assertThat(userBorrowings).hasSizeGreaterThanOrEqualTo(2);

        // VERIFY - Both books should have decreased available copies
        Optional<Book> updatedBook1 = bookDAO.findById(testBook.getBookId());
        Optional<Book> updatedBook2 = bookDAO.findById(book2.getBookId());

        assertThat(updatedBook1).isPresent().get().satisfies(b ->
                assertThat(b.getAvailableCopies()).isEqualTo(4));
        assertThat(updatedBook2).isPresent().get().satisfies(b ->
                assertThat(b.getAvailableCopies()).isEqualTo(4));

        logger.info("TC406 PASSED: Multiple borrowings by same user successful");

        // CLEANUP - Return books dan delete book2
        borrowingService.returnBook(borrowing1.getBorrowingId());
        borrowingService.returnBook(borrowing2.getBorrowingId());
        bookDAO.delete(book2.getBookId());
    }

    @Test
    @Order(7)
    @DisplayName("TC407: Borrowing limit enforcement - Maximum 5 books per user")
    void testBorrowingLimitEnforcement_MaximumFiveBooks() throws SQLException {
        // ARRANGE - Create multiple books
        List<Book> testBooks = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) { // Create 6 books
            Book book = createTestBook();
            book.setTitle("Limit Test Book " + (i + 1));
            book = bookDAO.create(book);
            testBooks.add(book);
        }

        // ACT - Borrow 5 books (should succeed)
        List<Borrowing> successfulBorrowings = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Borrowing borrowing = borrowingService.borrowBook(
                    testUser.getUserId(),
                    testBooks.get(i).getBookId(),
                    14
            );
            successfulBorrowings.add(borrowing);
        }

        // ASSERT - 6th borrowing should fail
        assertThatThrownBy(() ->
                borrowingService.borrowBook(testUser.getUserId(), testBooks.get(5).getBookId(), 14)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("batas peminjaman");

        // VERIFY - User has exactly 5 active borrowings
        int activeBorrowings = borrowingDAO.countActiveBorrowingsByUser(testUser.getUserId());
        assertThat(activeBorrowings).isEqualTo(5);

        logger.info("TC407 PASSED: Borrowing limit enforced correctly");

        // CLEANUP - Return all books dan delete test books
        for (Borrowing borrowing : successfulBorrowings) {
            borrowingService.returnBook(borrowing.getBorrowingId());
        }
        for (Book book : testBooks) {
            bookDAO.delete(book.getBookId());
        }
    }

    @Test
    @Order(8)
    @DisplayName("TC408: Concurrent borrowing simulation - Race condition handling")
    void testConcurrentBorrowingSimulation_RaceConditionHandling() throws SQLException {
        // ARRANGE - Set only 1 available copy
        bookDAO.updateAvailableCopies(testBook.getBookId(), 1);

        // ACT - First borrowing should succeed
        Borrowing borrowing1 = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14);
        assertThat(borrowing1).isNotNull();

        // Second borrowing should fail (no copies available)
        assertThatThrownBy(() ->
                borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No copies available");

        // VERIFY - Only one borrowing created and available copies = 0
        int activeBorrowings = borrowingDAO.countActiveBorrowingsByUser(testUser.getUserId());
        Optional<Book> updatedBook = bookDAO.findById(testBook.getBookId());

        assertThat(activeBorrowings).isEqualTo(1);
        assertThat(updatedBook).isPresent().get().satisfies(b ->
                assertThat(b.getAvailableCopies()).isEqualTo(0)
        );

        logger.info("TC408 PASSED: Concurrent borrowing handled correctly");

        // CLEANUP
        borrowingService.returnBook(borrowing1.getBorrowingId());
        bookDAO.updateAvailableCopies(testBook.getBookId(), 5);
    }

    @Test
    @Order(9)
    @DisplayName("TC409: Data consistency after multiple operations")
    void testDataConsistency_AfterMultipleOperations() throws SQLException {
        // ARRANGE
        int initialAvailableCopies = testBook.getAvailableCopies();

        // ACT - Perform multiple operations: borrow, borrow, return
        Borrowing b1 = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 14);
        Borrowing b2 = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 7);
        borrowingService.returnBook(b1.getBorrowingId());

        // ASSERT - Check consistency
        Optional<Book> finalBook = bookDAO.findById(testBook.getBookId());
        assertThat(finalBook).isPresent();

        // Initial: 5, after 2 borrows: 3, after 1 return: 4
        int expectedCopies = initialAvailableCopies - 1; // 5 - 2 + 1 = 4
        assertThat(finalBook.get().getAvailableCopies()).isEqualTo(expectedCopies);

        // VERIFY - Borrowing states
        Optional<Borrowing> borrowing1 = borrowingDAO.findById(b1.getBorrowingId());
        Optional<Borrowing> borrowing2 = borrowingDAO.findById(b2.getBorrowingId());

        assertThat(borrowing1).isPresent().get().satisfies(b ->
                assertThat(b.getStatus()).isEqualTo("returned")
        );
        assertThat(borrowing2).isPresent().get().satisfies(b ->
                assertThat(b.getStatus()).isEqualTo("borrowed")
        );

        logger.info("TC409 PASSED: Data consistency maintained after multiple operations");

        // CLEANUP
        borrowingService.returnBook(b2.getBorrowingId());
    }

    @Test
    @Order(10)
    @DisplayName("TC410: Fine calculation for overdue books")
    void testFineCalculation_ForOverdueBooks() throws SQLException {
        // ARRANGE - Create borrowing dengan due date di masa lalu
        // Borrowed 2 days ago, Due 1 day ago (Overdue by 1 day)
        Timestamp borrowDate = Timestamp.valueOf(LocalDateTime.now().minusDays(2));
        Timestamp pastDueDate = Timestamp.valueOf(LocalDateTime.now().minusDays(1));

        Borrowing borrowing = Borrowing.builder()
                .userId(testUser.getUserId())
                .bookId(testBook.getBookId())
                .borrowDate(borrowDate) // Set borrow date
                .dueDate(pastDueDate)   // Set overdue due date
                .status("borrowed")
                .build();

        borrowing = borrowingDAO.create(borrowing);

        // ACT - Calculate fine
        double fine = borrowingService.calculateFine(borrowing.getBorrowingId());

        // ASSERT - Fine should be calculated (5000 per day)
        assertThat(fine).isGreaterThan(0);
        // 1 day overdue = 5000
        assertThat(fine).isEqualTo(5000.0);

        logger.info("TC410 PASSED: Fine calculation working correctly");
        logger.info("Overdue days: 1, Fine: " + fine);

        // CLEANUP
        borrowingDAO.delete(borrowing.getBorrowingId());
    }

    @Test
    @Order(11)
    @DisplayName("TC411: Transaction integrity - All or nothing principle")
    void testTransactionIntegrity_AllOrNothingPrinciple() throws SQLException {
        int initialCopies = testBook.getAvailableCopies();

        try {
            // ACT - Try to borrow dengan invalid data (should fail completely)
            borrowingService.borrowBook(999999, testBook.getBookId(), 14); // Invalid user ID
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }

        // ASSERT - Book copies should remain unchanged (transaction rolled back or not executed)
        Optional<Book> bookAfterFailedBorrow = bookDAO.findById(testBook.getBookId());
        assertThat(bookAfterFailedBorrow.get().getAvailableCopies()).isEqualTo(initialCopies);

        logger.info("TC411 PASSED: Transaction integrity maintained after failed operation");
        logger.info("Copies unchanged: " + initialCopies);
    }

    @Test
    @Order(12)
    @DisplayName("TC412: Service layer validation - Invalid parameters")
    void testServiceLayerValidation_InvalidParameters() {
        // ACT & ASSERT - Various invalid parameters
        assertThatThrownBy(() -> borrowingService.borrowBook(null, testBook.getBookId(), 14))
                .isInstanceOf(IllegalArgumentException.class); // Or NullPointerException depending on impl

        assertThatThrownBy(() -> borrowingService.borrowBook(testUser.getUserId(), null, 14))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 0))
                .isInstanceOf(IllegalArgumentException.class); // Assuming 0 days is invalid or similar constraint

        logger.info("TC412 PASSED: Service layer validation working correctly");
    }

    // =========================================
    // HELPER METHODS
    // =========================================

    /**
     * Helper method untuk membuat test book
     */
    private Book createTestBook() {
        return Book.builder()
                .isbn("978integ" + System.currentTimeMillis())
                .title("Integration Test Book " + faker.book().title())
                .authorId(1)
                .publisherId(1)
                .categoryId(1)
                .publicationYear(2023)
                .pages(300)
                .language("Indonesia")
                .description("Integration test book")
                .totalCopies(5)
                .availableCopies(5)
                .price(new BigDecimal("75000.00"))
                .location("Rak Integ")
                .status("available")
                .build();
    }
}