package com.omermuktar.library.member;

import com.omermuktar.library.common.api.PageResponse;
import com.omermuktar.library.member.dto.MemberCreateRequest;
import com.omermuktar.library.member.dto.MemberResponse;
import com.omermuktar.library.member.dto.MemberUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/members")
@Validated
@Tag(name = "Üyeler", description = "Kütüphane üyesi yönetimi")
public class MemberController {

  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @PostMapping
  @Operation(summary = "Yeni üye oluştur")
  public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberCreateRequest request) {
    MemberResponse response = memberService.create(request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Üye ayrıntısını getir")
  public MemberResponse getById(
      @PathVariable @Positive(message = "Üye kimliği pozitif bir sayı olmalıdır.") Long id) {
    return memberService.getById(id);
  }

  @GetMapping
  @Operation(summary = "Üyeleri ara ve filtrele")
  public PageResponse<MemberResponse> search(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Boolean active,
      @ParameterObject @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
    return PageResponse.from(memberService.search(query, active, pageable));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Üye bilgilerini güncelle")
  public MemberResponse update(
      @PathVariable @Positive(message = "Üye kimliği pozitif bir sayı olmalıdır.") Long id,
      @Valid @RequestBody MemberUpdateRequest request) {
    return memberService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Üyeyi sil")
  public ResponseEntity<Void> delete(
      @PathVariable @Positive(message = "Üye kimliği pozitif bir sayı olmalıdır.") Long id) {
    memberService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
