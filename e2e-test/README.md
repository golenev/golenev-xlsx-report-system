# MyRunner и задачи Gradle

## Сводка изменений (русский перевод)
- Задача `allureReport` теперь зависит только от тестов, без циклических зависимостей.
- Добавлена обёртка `runMyKotlinFunctionWithReport`, которая последовательно запускает тесты, собирает Allure-отчёт и вызывает `MyRunner`.
- Путь к test-cases Allure передаётся в `MyRunner` во время исполнения через системное свойство `allure.testCasesPath`.

## Как пользоваться
1. Соберите и запустите последовательность тесты → отчёт → MyRunner одной командой:
   ```bash
   gradle -p e2e-test runMyKotlinFunctionWithReport
   ```
2. Если нужно только выполнить `MyRunner` (например, когда отчёт уже собран), запустите:
   ```bash
   gradle -p e2e-test runMyKotlinFunction
   ```
   Задача сама подставит путь к `test-cases` из `build/reports/allure-report/allureReport/data/test-cases`.
