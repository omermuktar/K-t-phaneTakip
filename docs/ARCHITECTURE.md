# Mimari Kararlar

Bu belgede, eski masaüstü uygulamasını REST API olarak yeniden tasarlarken aldığım temel teknik
kararları ve bu kararların nedenlerini özetledim.

## 1. Özelliğe göre paketleme

Kod yalnızca teknik katmanlara (`controller`, `service`, `repository`) ayrılmak yerine `book`, `member` ve `loan` özelliklerinin altında tutulur. Böylece bir iş alanına ait sınıflar birlikte bulunur ve proje büyüdüğünde değişiklik kapsamı daha kolay anlaşılır.

## 2. Controller–Service–Repository sınırları

- Controller yalnızca HTTP isteğini alır, doğrular ve uygun HTTP cevabını üretir.
- Service iş kurallarını ve transaction sınırlarını yönetir.
- Repository kalıcı veri erişimini soyutlar.
- Entity'ler kopya azaltma ve iade gibi kendi durumlarını koruyan davranışları içerir.

## 3. Eşzamanlı ödünç verme

İki istek son kalan kopyayı aynı anda almaya çalışabilir. `findByIdForUpdate` üzerinde `PESSIMISTIC_WRITE` kilidi kullanılarak kitap satırı transaction boyunca kilitlenir. Böylece kullanılabilir kopya sayısı sıfırın altına düşmez.

Entity'lerde ayrıca `@Version` bulunur. Bu, diğer güncelleme senaryolarında optimistic locking desteği sağlar.

## 4. Transaction yönetimi

Ödünç verme tek transaction içinde:

1. Kitap ve üye doğrulanır.
2. Aktif mükerrer kayıt kontrol edilir.
3. Kitabın kullanılabilir kopyası azaltılır.
4. Ödünç kaydı oluşturulur.

Adımlardan biri başarısız olursa bütün işlem geri alınır. İade işleminde de ödünç durumu ile kopya sayısı aynı transaction içinde güncellenir.

## 5. Gecikme durumu

`OVERDUE` veritabanında kalıcı durum olarak tutulmaz. Aktif bir kaydın teslim tarihi güncel zamandan önceyse gecikmiş kabul edilir. Bu yaklaşım, her gece durum güncelleyen ayrı bir zamanlanmış görev ihtiyacını ortadan kaldırır.

Zaman hesabı doğrudan `Instant.now()` yerine enjekte edilen `Clock` üzerinden yapılır. Testlerde sabit zaman kullanılabilir.

## 6. Veri tabanı profilleri

- `dev`: Dosya tabanlı H2, otomatik örnek veri ve hızlı başlangıç.
- `test`: Bellek içi H2, her test bağlamında temiz şema.
- `prod`: PostgreSQL, Flyway migration ve ortam değişkenleri.

## 7. Hata sözleşmesi

API hataları Spring `ProblemDetail` biçiminde döner. Her cevapta makine tarafından okunabilir `code`, zaman damgası ve gerektiğinde alan doğrulama hataları bulunur. Controller'larda tekrar eden `try/catch` blokları yerine tek bir `@RestControllerAdvice` kullanılır.

## 8. Silme politikası

Ödünç geçmişi bulunan kitap ve üyeler fiziksel olarak silinmez. Bu kural geçmiş kayıtların ve yabancı anahtar bütünlüğünün korunmasını sağlar. Aktif ödüncü bulunan üye pasif duruma da alınamaz.
