# Backend unit и contract tests: инструкции для LLM

Аудитория файла — только LLM-агенты. Не использовать его как пользовательскую документацию и не добавлять вводный материал, который не сокращает диагностику или изменение тестов.

## Обязательный маршрут

1. Перед анализом, добавлением или исправлением backend-тестов прочитать этот файл полностью.
2. Прочитать production-класс и его KDoc, затем соседние тесты того же слоя.
3. Определить тип проверки:
   - unit: изолированный JUnit 5 + Mockito без Spring context;
   - contract: реальный Spring context + MockMvc + H2 в пакете `com.example.report.contract`.
4. Не применять к backend-тестам `AllureId`, `step {}`, Page Object, endpoint DAO, Selenide и остальные правила `e2e-test`.
5. Не ослаблять точные assertions ради зелёного запуска. Если production-контракт изменён намеренно, синхронно обновить unit-тест, contract-тест и карту покрытия ниже.

## Среда и команды

- Использовать JDK 17. JDK 21 вызывает несовместимость JVM target Java/Kotlin.
- Запуск из корня репозитория:

```powershell
.\backend\gradlew.bat -p backend compileTestKotlin
.\backend\gradlew.bat -p backend unitTest
.\backend\gradlew.bat -p backend contractTest
```

- Точечный запуск:

```powershell
.\backend\gradlew.bat -p backend unitTest --tests "com.example.report.service.TestReportServiceUpsertBehaviorUnitTest"
.\backend\gradlew.bat -p backend contractTest --tests "com.example.report.contract.TestReportApiContractTest"
```

- Отчёты:
  - `backend/build/reports/tests/unitTest/index.html`;
  - `backend/build/reports/tests/contractTest/index.html`;
  - XML и stack trace: `backend/build/test-results/<task>/`.

## Unit-тесты

- Располагать рядом с production-пакетом под `backend/src/test/kotlin/com/example/report`.
- Использовать традиционный стиль JUnit 5/Mockito, backtick-имена по наблюдаемому поведению и структуру arrange/act/assert.
- Мокать только внешние зависимости тестируемого класса. Не мокать внутренние private-алгоритмы.
- Проверять возвращаемое значение, точные мутации сущностей, порядок, нормализацию, исключения и отсутствие нежелательных вызовов repository/service.
- Для времени использовать фиксированный `Clock`; не зависеть от системной даты.
- Для невалидных DTO допустимы `Map`, raw JSON и намеренно неполные объекты, если это часть проверяемого контракта.

## Contract-тесты

- Все contract-тесты размещать только в `com.example.report.contract` и наследовать от `ContractTestSupport`.
- `ContractTestSupport` поднимает `@SpringBootTest`, подключает MockMvc, H2 и фиксированный Clock `2026-06-28T10:15:30Z` в зоне `Europe/Moscow`.
- Перед каждым тестом очищаются `RegressionRepository`, затем `TestReportRepository`. Не добавлять локальную широкую очистку в классы-наследники.
- Проверять внешний контракт: HTTP status, content type, заголовки, точную JSON-структуру, сообщения ошибок и существенное состояние после запроса.
- Для XLSX проверять headers, MIME type и сигнатуру ZIP `PK`; содержимое workbook детально проверять на unit-уровне `ExcelExportServiceUnitTest`.
- Не вызывать controller/service напрямую: контракт должен проходить через MockMvc.

## Зафиксированные инварианты API

- `POST /api/tests`:
  - обязательны `testId`, `category`, `shortTitle`, структурированный `scenario`;
  - одиночное обновление может брать отсутствующие обязательные поля из существующей записи;
  - `forceUpdate=false` сохраняет ручные поля существующей записи, а для новой применяет серверные defaults;
  - `runStatus` одиночного запроса не меняет статус регресса.
- `POST /api/tests/batch`:
  - не использует fallback обязательных полей из существующей записи;
  - валидирует весь batch до первого сохранения;
  - при `isRegressRunning=true` требует допустимый `runStatus` у каждого элемента и активный регресс текущей даты;
  - принимает JSON alias `run_status` и синхронизирует результаты активного регресса.
- Scenario:
  - хранится структурированным JSON с рекурсивными `subSteps`, `attachments`, `parameters` и duration metadata;
  - legacy text/JSON читается и восстанавливается без потери бизнес-шагов;
  - отрицательные duration/size не сохраняются;
  - `attachments` каждого входного шага обязателен и должен быть массивом.
- Regression:
  - одновременно существует не более одного `RUNNING` запуска;
  - имя релиза trim-ится и не может повторять исторический release;
  - stop требует статус каждого существующего тест-кейса и сохраняет отсортированный snapshot;
  - cancel удаляет пустой запуск, но завершает и сохраняет запуск с накопленными результатами;
  - отсутствие актуального запуска в batch/upload regression mode возвращает `404`.
- `POST /uploadReport`:
  - требует multipart `files` и хотя бы один Allure test JSON;
  - требует label `AS_ID`;
  - восстанавливает текстовые attachments по `source`/`paths`;
  - по умолчанию сохраняет ручные поля существующего теста;
  - в regression mode нормализует Allure status и синхронизирует активный регресс.
- `GET /api/tests/export/excel`:
  - возвращает XLSX даже при значении длиннее жёсткого лимита Excel в 32 767 символов;
  - без потерь разбивает такое значение на несколько расположенных по вертикали ячеек, не разделяя surrogate pair;
  - убирает внутренние границы между частями длинного значения и объединяет остальные колонки тест-кейса по высоте этих строк;
  - применяет разбиение ко всем экспортируемым полям текущего отчёта и regression snapshot.
- Ошибки `ResponseStatusException` возвращают `timestamp`, `status`, `error`, `message`, `path`; сообщение `Required field <name> is missing` дополнительно возвращает `missingField`.

## Карта contract-покрытия

| Контракт | Класс |
|---|---|
| `/api/tests`, delete, config, текущий XLSX | `TestReportApiContractTest` |
| `/api/tests/batch`, атомарность, regression mode | `BatchApiContractTest` |
| lifecycle/history/snapshot/XLSX регресса | `RegressionApiContractTest` |
| multipart Allure import и attachments | `UploadReportApiContractTest` |
| CORS `/api/**` и `/uploadReport` | `CorsApiContractTest` |

## Карта unit-покрытия

| Область | Основные классы |
|---|---|
| чтение, сортировка, базовая валидация отчёта | `TestReportServiceUnitTest` |
| одиночное сохранение structured scenario | `TestReportServicePostUnitTest` |
| defaults, `forceUpdate`, batch, delete | `TestReportServiceUpsertBehaviorUnitTest` |
| normalization и legacy scenario | `TestReportScenarioUnitTest` |
| основной lifecycle регресса | `RegressionServiceUnitTest` |
| state, history, sync, stale run, snapshot | `RegressionServiceStateUnitTest` |
| structured scenario в snapshot | `RegressionServiceScenarioSnapshotTest` |
| формирование текущего и snapshot XLSX | `ExcelExportServiceUnitTest` |
| config, statuses, DTO validation, error body, time | соответствующие тесты в `service`, `model`, `dto`, `handler`, `config` |

## Алгоритм разбора падения

1. Запустить только упавший класс и прочитать XML из `backend/build/test-results/<task>/`.
2. Сопоставить assertion с production KDoc и инвариантами этого файла.
3. Проверить, является ли расхождение намеренным изменением задачи или регрессией production-кода.
4. Если контракт не менялся, исправлять production-код или setup теста; не заменять точное ожидание более общим.
5. Если контракт изменился намеренно, найти все связанные unit/contract-тесты через карту покрытия и `rg`, затем обновить этот файл.
6. Запустить затронутый класс, после него полностью `unitTest` и `contractTest`.
7. Выполнить `git diff --check`. Не добавлять `backend/build`, `.gradle` и test reports в git.
