package com.omermuktar.library.book;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

  boolean existsByIsbnIgnoreCase(String isbn);

  boolean existsByIsbnIgnoreCaseAndIdNot(String isbn, Long id);

  @Query(
      """
      select b from Book b
      where (:query is null
             or lower(b.title) like lower(concat('%', :query, '%'))
             or lower(b.author) like lower(concat('%', :query, '%'))
             or lower(b.isbn) like lower(concat('%', :query, '%')))
        and (:category is null or lower(b.category) = lower(:category))
        and (:available is null
             or (:available = true and b.availableCopies > 0)
             or (:available = false and b.availableCopies = 0))
      """)
  Page<Book> search(
      @Param("query") String query,
      @Param("category") String category,
      @Param("available") Boolean available,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Book b where b.id = :id")
  Optional<Book> findByIdForUpdate(@Param("id") Long id);

  @Query("select distinct b.category from Book b order by b.category")
  List<String> findDistinctCategories();

  long countByAvailableCopiesGreaterThan(int value);

  @Query("select coalesce(sum(b.totalCopies), 0) from Book b")
  long sumTotalCopies();

  @Query("select coalesce(sum(b.availableCopies), 0) from Book b")
  long sumAvailableCopies();
}
