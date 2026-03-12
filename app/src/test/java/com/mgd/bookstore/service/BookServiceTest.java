package com.mgd.bookstore.service;

import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Category;
import com.mgd.bookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book techBook;
    private Book fantasyBook;

    @BeforeEach
    void setUp() {
        techBook = new Book();
        techBook.setId(1L);
        techBook.setTitle("Clean Code");
        techBook.setAuthor("Robert Martin");
        techBook.setPrice(new BigDecimal("39.99"));
        techBook.setDescription("A handbook of agile software craftsmanship");
        techBook.setCategories(List.of(Category.TECHNOLOGY));
        techBook.setActive(true);
        techBook.setStock(10);

        fantasyBook = new Book();
        fantasyBook.setId(2L);
        fantasyBook.setTitle("The Hobbit");
        fantasyBook.setAuthor("J.R.R. Tolkien");
        fantasyBook.setPrice(new BigDecimal("14.99"));
        fantasyBook.setDescription("A fantasy adventure");
        fantasyBook.setCategories(List.of(Category.FANTASY));
        fantasyBook.setActive(true);
        fantasyBook.setStock(5);
    }

    // -----------------------------------------------------------------------
    // getAllBooks - only active books
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllBooks - returns only active books")
    void getAllBooks_returnsOnlyActiveBooks() {
        when(bookRepository.findByActive(true)).thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.getAllBooks();

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(techBook, fantasyBook);
        verify(bookRepository).findByActive(true);
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllBooks - returns empty list when no active books")
    void getAllBooks_returnsEmptyList_whenNoActiveBooks() {
        when(bookRepository.findByActive(true)).thenReturn(List.of());

        List<Book> result = bookService.getAllBooks();

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getAllBooksAdmin - all books regardless of status
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllBooksAdmin - returns all books including inactive")
    void getAllBooksAdmin_returnsAllBooks() {
        Book inactiveBook = new Book();
        inactiveBook.setId(3L);
        inactiveBook.setTitle("Delisted Book");
        inactiveBook.setActive(false);

        when(bookRepository.findAll()).thenReturn(List.of(techBook, fantasyBook, inactiveBook));

        List<Book> result = bookService.getAllBooksAdmin();

        assertThat(result).hasSize(3);
        verify(bookRepository).findAll();
    }

    // -----------------------------------------------------------------------
    // saveBook
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("saveBook - saves and returns the book")
    void saveBook_savesAndReturnsBook() {
        when(bookRepository.save(techBook)).thenReturn(techBook);

        Book result = bookService.saveBook(techBook);

        assertThat(result).isEqualTo(techBook);
        verify(bookRepository).save(techBook);
    }

    @Test
    @DisplayName("saveBook - sets stock to 0 when stock is null")
    void saveBook_setsStockToZero_whenStockIsNull() {
        techBook.setStock(null);
        when(bookRepository.save(techBook)).thenReturn(techBook);

        bookService.saveBook(techBook);

        assertThat(techBook.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("saveBook - preserves existing stock when not null")
    void saveBook_preservesStock_whenNotNull() {
        techBook.setStock(42);
        when(bookRepository.save(techBook)).thenReturn(techBook);

        bookService.saveBook(techBook);

        assertThat(techBook.getStock()).isEqualTo(42);
    }

    // -----------------------------------------------------------------------
    // getBookById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getBookById - returns book when found")
    void getBookById_returnsBook_whenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(techBook));

        Optional<Book> result = bookService.getBookById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Clean Code");
        assertThat(result.get().getAuthor()).isEqualTo("Robert Martin");
    }

    @Test
    @DisplayName("getBookById - returns empty Optional when not found")
    void getBookById_returnsEmpty_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.getBookById(99L);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // updateBook
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateBook - updates all fields and returns updated book")
    void updateBook_updatesAndReturnsBook() {
        Book updated = new Book();
        updated.setTitle("Clean Architecture");
        updated.setAuthor("Robert Martin");
        updated.setPrice(new BigDecimal("44.99"));
        updated.setDescription("New description");
        updated.setCategories(List.of(Category.TECHNOLOGY));
        updated.setStock(20);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(techBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Book> result = bookService.updateBook(1L, updated);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Clean Architecture");
        assertThat(result.get().getPrice()).isEqualByComparingTo("44.99");
        assertThat(result.get().getStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("updateBook - returns empty Optional when book not found")
    void updateBook_returnsEmpty_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.updateBook(99L, new Book());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateBook - sets stock to 0 when updated stock is null")
    void updateBook_setsStockToZero_whenUpdatedStockIsNull() {
        Book updated = new Book();
        updated.setTitle("Title");
        updated.setAuthor("Author");
        updated.setPrice(BigDecimal.ONE);
        updated.setStock(null);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(techBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Book> result = bookService.updateBook(1L, updated);

        assertThat(result).isPresent();
        assertThat(result.get().getStock()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // deleteBook
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteBook - calls repository deleteById")
    void deleteBook_callsRepositoryDeleteById() {
        doNothing().when(bookRepository).deleteById(1L);

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    // -----------------------------------------------------------------------
    // delistBook
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("delistBook - sets active to false and saves")
    void delistBook_setsActiveToFalse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(techBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Book> result = bookService.delistBook(1L);

        assertThat(result).isPresent();
        assertThat(result.get().isActive()).isFalse();
        verify(bookRepository).save(techBook);
    }

    @Test
    @DisplayName("delistBook - returns empty Optional when book not found")
    void delistBook_returnsEmpty_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.delistBook(99L);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // relistBook
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("relistBook - sets active to true and saves")
    void relistBook_setsActiveToTrue() {
        techBook.setActive(false);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(techBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Book> result = bookService.relistBook(1L);

        assertThat(result).isPresent();
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("relistBook - returns empty Optional when book not found")
    void relistBook_returnsEmpty_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.relistBook(99L);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // searchBooks - title only
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchBooks - by title only returns matching books")
    void searchBooks_byTitle_returnsMatches() {
        when(bookRepository.findByTitleContainingIgnoreCase("Clean")).thenReturn(List.of(techBook));

        List<Book> result = bookService.searchBooks("Clean", null, null);

        assertThat(result).containsExactly(techBook);
        verify(bookRepository).findByTitleContainingIgnoreCase("Clean");
        verify(bookRepository, never()).findByAuthorContainingIgnoreCase(any());
        verify(bookRepository, never()).findAll();
    }

    @Test
    @DisplayName("searchBooks - title takes priority over author when both provided")
    void searchBooks_titleTakesPriorityOverAuthor() {
        when(bookRepository.findByTitleContainingIgnoreCase("Clean")).thenReturn(List.of(techBook));

        List<Book> result = bookService.searchBooks("Clean", "Tolkien", null);

        assertThat(result).containsExactly(techBook);
        verify(bookRepository).findByTitleContainingIgnoreCase("Clean");
        verify(bookRepository, never()).findByAuthorContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("searchBooks - by title with category filters down results")
    void searchBooks_byTitleAndCategory_filtersResults() {
        when(bookRepository.findByTitleContainingIgnoreCase("The"))
                .thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.searchBooks("The", null, Category.FANTASY);

        assertThat(result).containsExactly(fantasyBook);
    }

    @Test
    @DisplayName("searchBooks - by title with non-matching category returns empty list")
    void searchBooks_byTitleAndCategory_returnsEmpty_whenNoCategoryMatch() {
        when(bookRepository.findByTitleContainingIgnoreCase("Clean")).thenReturn(List.of(techBook));

        List<Book> result = bookService.searchBooks("Clean", null, Category.ROMANCE);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // searchBooks - author only
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchBooks - by author only returns matching books")
    void searchBooks_byAuthor_returnsMatches() {
        when(bookRepository.findByAuthorContainingIgnoreCase("Tolkien")).thenReturn(List.of(fantasyBook));

        List<Book> result = bookService.searchBooks(null, "Tolkien", null);

        assertThat(result).containsExactly(fantasyBook);
        verify(bookRepository).findByAuthorContainingIgnoreCase("Tolkien");
        verify(bookRepository, never()).findByTitleContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("searchBooks - by author with matching category returns results")
    void searchBooks_byAuthorAndCategory_matchingCategory_returnsResults() {
        when(bookRepository.findByAuthorContainingIgnoreCase("Tolkien"))
                .thenReturn(List.of(fantasyBook));

        List<Book> result = bookService.searchBooks(null, "Tolkien", Category.FANTASY);

        assertThat(result).containsExactly(fantasyBook);
    }

    // -----------------------------------------------------------------------
    // searchBooks - category only
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchBooks - by category only filters from all books")
    void searchBooks_byCategoryOnly_filtersFromAll() {
        when(bookRepository.findAll()).thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.searchBooks(null, null, Category.TECHNOLOGY);

        assertThat(result).containsExactly(techBook);
    }

    @Test
    @DisplayName("searchBooks - by category with no matches returns empty list")
    void searchBooks_byCategoryOnly_returnsEmpty_whenNoMatch() {
        when(bookRepository.findAll()).thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.searchBooks(null, null, Category.SCIENCE);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // searchBooks - no filters
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchBooks - no filters returns all books")
    void searchBooks_noFilters_returnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.searchBooks(null, null, null);

        assertThat(result).hasSize(2);
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("searchBooks - blank title treated as no title filter")
    void searchBooks_blankTitle_treatedAsNoFilter() {
        when(bookRepository.findAll()).thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.searchBooks("   ", null, null);

        assertThat(result).hasSize(2);
        verify(bookRepository).findAll();
        verify(bookRepository, never()).findByTitleContainingIgnoreCase(any());
    }

    // -----------------------------------------------------------------------
    // Legacy methods
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchByTitle - delegates to repository")
    void searchByTitle_delegatesToRepository() {
        when(bookRepository.findByTitleContainingIgnoreCase("code")).thenReturn(List.of(techBook));

        List<Book> result = bookService.searchByTitle("code");

        assertThat(result).containsExactly(techBook);
        verify(bookRepository).findByTitleContainingIgnoreCase("code");
    }

    @Test
    @DisplayName("searchByAuthor - delegates to repository")
    void searchByAuthor_delegatesToRepository() {
        when(bookRepository.findByAuthorContainingIgnoreCase("Martin")).thenReturn(List.of(techBook));

        List<Book> result = bookService.searchByAuthor("Martin");

        assertThat(result).containsExactly(techBook);
        verify(bookRepository).findByAuthorContainingIgnoreCase("Martin");
    }
}