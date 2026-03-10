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

        fantasyBook = new Book();
        fantasyBook.setId(2L);
        fantasyBook.setTitle("The Hobbit");
        fantasyBook.setAuthor("J.R.R. Tolkien");
        fantasyBook.setPrice(new BigDecimal("14.99"));
        fantasyBook.setDescription("A fantasy adventure");
        fantasyBook.setCategories(List.of(Category.FANTASY));
    }

    // -----------------------------------------------------------------------
    // getAllBooks
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllBooks - returns all books from repository")
    void getAllBooks_returnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(techBook, fantasyBook));

        List<Book> result = bookService.getAllBooks();

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(techBook, fantasyBook);
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("getAllBooks - returns empty list when no books exist")
    void getAllBooks_returnsEmptyList_whenNoBooksExist() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<Book> result = bookService.getAllBooks();

        assertThat(result).isEmpty();
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