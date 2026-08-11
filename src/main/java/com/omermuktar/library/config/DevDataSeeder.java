package com.omermuktar.library.config;

import com.omermuktar.library.book.Book;
import com.omermuktar.library.book.BookRepository;
import com.omermuktar.library.member.Member;
import com.omermuktar.library.member.MemberRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;

  public DevDataSeeder(BookRepository bookRepository, MemberRepository memberRepository) {
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (bookRepository.count() == 0) {
      bookRepository.save(
          Book.create(
              "9780132350884", "Clean Code", "Robert C. Martin", "Yazılım Mühendisliği", 3));
      bookRepository.save(
          Book.create("9781617294945", "Spring in Action", "Craig Walls", "Backend", 2));
      bookRepository.save(
          Book.create(
              "9781491950357",
              "Designing Data-Intensive Applications",
              "Martin Kleppmann",
              "Veri Sistemleri",
              2));
    }

    if (memberRepository.count() == 0) {
      memberRepository.save(
          Member.create("MEM-001", "Ömer Şamil Muktar", "omer.samil@example.com"));
      memberRepository.save(Member.create("MEM-002", "Zeynep Kaya", "zeynep.kaya@example.com"));
    }
  }
}
