# AGENTS.md - Project Context

## Project Overview
**NextTranslate** - Android native app (Java) for translation with Clean Architecture + MVVM pattern.

- Package: `com.igoy86.nexttranslate`
- Build: Gradle + Android Gradle Plugin 8.12.0
- Min SDK: 29, Target SDK: 35, Compile SDK: 36

## Session: 12 June 2026

### Task
Analisa dan perbaiki bugs pada fitur **Collection** + buat unit test.

### Bugs Fixed (9 total)

#### HIGH
1. **CollectionAdapter.java:99-100** - `areContentsTheSame` mengabaikan `wordCount` → badge tidak update
   - Fix: Tambahkan `oldItem.getWordCount() == newItem.getWordCount()`
2. **CollectionDetailViewModel.java:91-93** - Reassign `wordsLiveData` memutus observer
   - Fix: Gunakan `MediatorLiveData.addSource()` untuk switch source
3. **CollectionViewModel.java:273** - `observeForever` leak
   - Fix: Simpan reference observer, remove sebelum buat baru, cleanup di `onCleared()`

#### MEDIUM
4. **CollectionDetailViewModel.java:103-107** - Snackbar muncul sebelum DB delete selesai
   - Fix: Jalankan delete di `diskIO`, post snackbar ke `mainThread` setelah selesai
5. **CollectionFragment.java:541** - `getAdapterPosition()` bisa return -1 → crash
   - Fix: Cek `NO_POSITION` sebelum akses list
6. **CollectionRepositoryImpl.java:91-93** - Callback tidak dipanggil saat gagal
   - Fix: Tambah `onError()` default method di `InsertCallback`, panggil saat exception
7. **CollectionFragment.java:29-35** - Duplicate imports
   - Fix: Hapus duplikat

#### LOW
8. **item_collection_card.xml:82** - `android:tint` deprecated
   - Fix: Ganti ke `app:tint`
9. **CollectionWordItem.java** - Missing `equals()`/`hashCode()`/`toString()`
   - Fix: Tambahkan implementasi lengkap

### Unit Tests Created (64 tests)

| File | Tests |
|---|---|
| `CollectionItemTest.java` | 9 |
| `CollectionWordItemTest.java` | 11 |
| `CollectionAdapterDiffTest.java` | 7 |
| `CollectionWordAdapterDiffTest.java` | 7 |
| `CollectionRepositoryImplTest.java` | 7 |
| `CollectionViewModelTest.java` | 12 |
| `CollectionDetailViewModelTest.java` | 6 |

### Dependencies Added
- `mockito-core:5.14.2`
- `mockito-inline:5.2.0`
- `arch-core-testing:2.2.0`
- `testOptions { unitTests.returnDefaultValues = true }` di build.gradle

### Files Modified
- `app/src/main/java/.../presentation/collection/CollectionAdapter.java`
- `app/src/main/java/.../presentation/collection/CollectionDetailViewModel.java`
- `app/src/main/java/.../presentation/collection/CollectionViewModel.java`
- `app/src/main/java/.../presentation/collection/CollectionFragment.java`
- `app/src/main/java/.../data/repository/CollectionRepositoryImpl.java`
- `app/src/main/java/.../domain/repository/CollectionRepository.java`
- `app/src/main/java/.../domain/model/CollectionWordItem.java`
- `app/src/main/res/layout/item_collection_card.xml`
- `gradle/libs.versions.toml`
- `app/build.gradle`

### Known Remaining Issues
- Bottom sheet code (`showSaveToCollectionSheet`) masih duplikat antara `CollectionFragment` dan `TranslateFragment`
- Tidak ada `DeleteWordUseCase` - delete langsung lewat repository
- Tidak ada pengecekan duplicate word saat add ke collection
- Beberapa method DAO tidak terpakai (dead code)

### Build Commands
```bash
./gradlew compileDebugJavaWithJavac    # Compile
./gradlew testDebugUnitTest            # Run unit tests
```
