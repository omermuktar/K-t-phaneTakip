package com.omermuktar.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omermuktar.library.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class BookTest {

  @Test
  void borrowAndReturnUpdateAvailableCopyCount() {
    Book book = Book.create("978-0132350884", "Clean Code", "Robert C. Martin", "Software", 2);

    book.borrowCopy();
    assertThat(book.getAvailableCopies()).isEqualTo(1);

    book.returnCopy();
    assertThat(book.getAvailableCopies()).isEqualTo(2);
  }

  @Test
  void borrowingUnavailableBookFails() {
    Book book = Book.create("9780132350884", "Clean Code", "Robert C. Martin", "Software", 1);
    book.borrowCopy();

    assertThatThrownBy(book::borrowCopy)
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("kullanılabilir kopya");
  }

  @Test
  void totalCopyCountCannotDropBelowBorrowedCopies() {
    Book book = Book.create("9780132350884", "Clean Code", "Robert C. Martin", "Software", 2);
    book.borrowCopy();
    book.borrowCopy();

    assertThatThrownBy(
            () ->
                book.updateDetails(
                    "9780132350884", "Clean Code", "Robert C. Martin", "Software", 1))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ödünçteki");
  }
}
