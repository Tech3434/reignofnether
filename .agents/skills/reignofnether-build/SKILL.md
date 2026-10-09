---
name: reignofnether-build
description: Как собрать мод Reign of Nether и прогнать гейты (compileJava, validateMixins, runData, test). Использовать при любой правке кода перед коммитом.
---

# Сборка и гейты

Проект — git-репозиторий `___temp/` (NeoForge 21.1 / MC 1.21.1). **`gradlew.bat` не работает**: он
передаёт java одновременно `-classpath ""` и `-jar`. Запускать java напрямую:

```bash
cd ___temp
JAVA="/c/Program Files/Java/jdk-21/bin/java.exe"      # Windows, git-bash
"$JAVA" -Dorg.gradle.appname=gradlew -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

`--offline` обязателен (нет сети). `--rerun` заставляет повторить задачу, даже если Gradle считает её
up-to-date (полезно, когда «зелено» только потому, что ничего не перекомпилировалось):

```bash
... compileJava --rerun --offline --console=plain
```

## Задачи-гейты

| Задача | Что проверяет |
|---|---|
| `compileJava` | типы и синтаксис |
| `validateMixins` | что каждый `@Inject` резолвится в `neoforge-21.1.250.jar` (печатает «N injection point(s) … / M mixin class(es)») |
| `runData` | генерация данных/ресурсов, `Missing:` не должно быть |
| `test` | `DataCodecTest` — декодирует **все** поставляемые `unit`/`building`/`faction` JSON их кодеками (+ точечные проверки `worker`/`menu`); `ContentValidationTest` — валит сборку при неизвестном поле в тех же файлах (имена сверяются по компонентам record'ов). Единственный автоматический гейт против кодек-/схемных регрессий |

## Правила

* Полный прогон перед коммитом: `compileJava` → `validateMixins` → `runData` → `test` (можно одной
  командой, перечислив задачи).
* Правки миксинов, событий или пакетов — самое рискованное: `validateMixins` пропускает часть ошибок
  (`@Shadow`/`@Redirect`, `Invalid LVT row`, `InvalidInjectionException`), а `runServer` в этой среде
  **запускать нельзя** (dev-сервер печатает `Done (...)`, но сам не завершается — процесс висит до
  ручного вмешательства владельца, см. `AGENTS.md`). Если сервер всё же поднимали, достижение
  `Done (...)` в логе уже доказывает рантайм-инициализацию.
* Ни один гейт не проверяет клиент, геометрию на экране и поведение в игре — это ручной прогон
  `runClient` по `docs/RUNCLIENT_CHECKLIST.md` (один сеанс на все фичи).
* Опечатка в JSON определения ловится гейтом `test`; в рантайме то же сообщение (`[content-validation] …
  unknown field 'x'`) пишется в лог при загрузке мира и на `/reload`.
* **CRLF:** большинство `.java` хранит CRLF. Правку делать так: нормализовать в LF → редактировать →
  вернуть CRLF, иначе дифф «весь файл» (см. `reignofnether-line-endings`).
