package com.omermuktar.library.book;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private BookRepository bookRepository;

  @BeforeEach
  void clearBooks() {
    bookRepository.deleteAll();
  }

  @Test
  void createBookReturnsCreatedResource() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "isbn": "9780132350884",
                      "title": "Clean Code",
                      "author": "Robert C. Martin",
                      "category": "Software Engineering",
                      "totalCopies": 3
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.isbn").value("9780132350884"))
        .andExpect(jsonPath("$.availableCopies").value(3));
  }

  @Test
  void invalidBookReturnsStandardProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "isbn": "bad",
                      "title": "",
                      "author": "Author",
                      "category": "Backend",
                      "totalCopies": 0
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.violations").isArray());
  }

  @Test
  void unknownBookReturnsNotFoundProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/v1/books/999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void duplicateNormalizedIsbnReturnsConflict() throws Exception {
    createBook("978-0132350884", "Clean Code", "Software Engineering", 3);

    mockMvc
        .perform(
            post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "isbn": "9780132350884",
                      "title": "Aynı ISBN",
                      "author": "Başka Yazar",
                      "category": "Software Engineering",
                      "totalCopies": 1
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ISBN_ALREADY_EXISTS"));
  }

  @Test
  void updateSearchCategoriesAndDeleteFlowWorks() throws Exception {
    createBook("9780132350884", "Clean Code", "Software Engineering", 3);
    Long id = bookRepository.findAll().get(0).getId();

    mockMvc
        .perform(
            put("/api/v1/books/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "isbn": "978-0132350884",
                      "title": "Clean Code - Updated",
                      "author": "Robert C. Martin",
                      "category": "Backend",
                      "totalCopies": 5
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isbn").value("9780132350884"))
        .andExpect(jsonPath("$.title").value("Clean Code - Updated"))
        .andExpect(jsonPath("$.availableCopies").value(5));

    mockMvc
        .perform(
            get("/api/v1/books")
                .param("query", "updated")
                .param("category", "backend")
                .param("available", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(true));

    mockMvc
        .perform(get("/api/v1/books/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("Backend"));

    mockMvc.perform(delete("/api/v1/books/{id}", id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/books/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void updateCannotUseAnotherBooksIsbn() throws Exception {
    createBook("9780132350884", "Clean Code", "Software", 1);
    createBook("9780201633610", "Design Patterns", "Software", 1);
    Long secondId =
        bookRepository.findAll().stream()
            .filter(book -> book.getTitle().equals("Design Patterns"))
            .findFirst()
            .orElseThrow()
            .getId();

    mockMvc
        .perform(
            put("/api/v1/books/{id}", secondId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "isbn": "9780132350884",
                      "title": "Design Patterns",
                      "author": "Erich Gamma",
                      "category": "Software",
                      "totalCopies": 1
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ISBN_ALREADY_EXISTS"));
  }

  @Test
  void malformedJsonAndInvalidSortReturnStandardErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/books").contentType(MediaType.APPLICATION_JSON).content("{not-json}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

    mockMvc
        .perform(get("/api/v1/books").param("sort", "doesNotExist"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
  }

  private void createBook(String isbn, String title, String category, int totalCopies)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "isbn": "%s",
                      "title": "%s",
                      "author": "Robert C. Martin",
                      "category": "%s",
                      "totalCopies": %d
                    }
                    """
                        .formatted(isbn, title, category, totalCopies)))
        .andExpect(status().isCreated());
  }
}
