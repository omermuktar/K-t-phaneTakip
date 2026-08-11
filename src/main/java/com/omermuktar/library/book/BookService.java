package com.omermuktar.library.book;

import com.omermuktar.library.book.dto.BookCreateRequest;
import com.omermuktar.library.book.dto.BookResponse;
import com.omermuktar.library.book.dto.BookUpdateRequest;
import com.omermuktar.library.common.exception.ConflictException;
import com.omermuktar.library.common.exception.ResourceNotFoundException;
import com.omermuktar.library.loan.LoanRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

  private final BookRepository bookRepository;
  private final LoanRepository loanRepository;

  public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
    this.bookRepository = bookRepository;
    this.loanRepository = loanRepository;
  }

  @Transactional
  public BookResponse create(BookCreateRequest request) {
    String isbn = normalizeIsbn(request.isbn());
    if (bookRepository.existsByIsbnIgnoreCase(isbn)) {
      throw new ConflictException(
          "ISBN_ALREADY_EXISTS", "Bu ISBN ile kayıtlı bir kitap zaten var.");
    }
    Book book =
        Book.create(
            isbn, request.title(), request.author(), request.category(), request.totalCopies());
    return BookResponse.from(bookRepository.saveAndFlush(book));
  }

  public BookResponse getById(Long id) {
    return BookResponse.from(findBook(id));
  }

  public Page<BookResponse> search(
      String query, String category, Boolean available, Pageable pageable) {
    return bookRepository
        .search(normalizeFilter(query), normalizeFilter(category), available, pageable)
        .map(BookResponse::from);
  }

  public List<String> listCategories() {
    return bookRepository.findDistinctCategories();
  }

  @Transactional
  public BookResponse update(Long id, BookUpdateRequest request) {
    Book book = findBookForUpdate(id);
    String isbn = normalizeIsbn(request.isbn());
    if (bookRepository.existsByIsbnIgnoreCaseAndIdNot(isbn, id)) {
      throw new ConflictException(
          "ISBN_ALREADY_EXISTS", "Bu ISBN ile kayıtlı başka bir kitap var.");
    }
    book.updateDetails(
        isbn, request.title(), request.author(), request.category(), request.totalCopies());
    return BookResponse.from(bookRepository.saveAndFlush(book));
  }

  @Transactional
  public void delete(Long id) {
    Book book = findBookForUpdate(id);
    if (loanRepository.existsByBookId(id)) {
      throw new ConflictException(
          "BOOK_HAS_LOAN_HISTORY", "Ödünç geçmişi bulunan kitap silinemez.");
    }
    bookRepository.delete(book);
  }

  private Book findBook(Long id) {
    return bookRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Kitap", id));
  }

  private Book findBookForUpdate(Long id) {
    return bookRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("Kitap", id));
  }

  private static String normalizeIsbn(String isbn) {
    return isbn.replace("-", "").trim().toUpperCase();
  }

  private static String normalizeFilter(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
