---
name: reignofnether-build
description: Как собрать мод Reign of Nether и прогнать гейты (compileJava, validateMixins, runData, test, runServer через харнесс, runClient). Использовать при любой правке кода перед коммитом.
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
| `runServer` | применение main-миксинов **в рантайме**; запускать **только через харнесс** (см. ниже), а не сырым gradle-таском |
| `runClient` | клиентские миксины, HUD и рендер; **не** завершается сам, останавливать вручную |

## Правила

* Полный прогон перед коммитом: `compileJava` → `validateMixins` → `runData` → `test` (можно одной
  командой, перечислив задачи).
* Правки миксинов, событий или пакетов — самое рискованное: `validateMixins` пропускает часть ошибок
  (`@Shadow`/`@Redirect`, `Invalid LVT row`, `InvalidInjectionException`), а сырой dev-сервер в этой
  среде печатает `Done (...)` и может не завершиться после `stop` (процесс висит до ручного
  вмешательства владельца, см. `AGENTS.md`). Поэтому такой прогон — **только через харнесс** (ниже);
  достижение `Done (...)` в логе доказывает рантайм-инициализацию миксинов/реестров.
* `runServer`: **только `bash run/port_srv15.sh`**. Он ждёт `Done (` до 85 с, гоняет проверки по RCON,
  а фазу выключения жёстко ограничивает 15 секундами и добивает только свои java-процессы. Ему нужен
  включённый RCON в дев-`run/server.properties` (`enable-rcon=true`, `rcon.port=25585`,
  `rcon.password=…`): stdin gradle до сервера не доходит, консольные команды молча теряются. Логи:
  `run/port_srv15.log`, сводка `run/port_srv15_summary.log`, транскрипт команд
  `run/port_rcon_results.txt` (последняя строка — `RCON summary: ok/skipped/failed`). В каркасе нет
  mod-сущностей, поэтому зависящие от контента проверки драйвера (суммон `reignofnether:wraith_unit`)
  помечаются `skipped` — это норма, а не провал.
* Ни один гейт не проверяет клиент, геометрию на экране и поведение в игре — это ручной прогон
  `runClient` по `docs/RUNCLIENT_CHECKLIST.md` (один сеанс на все фичи). Готовность клиента — строка
  `Setting user:` в логе, здоровый клиент — ноль строк `ERROR`.
* Опечатка в JSON определения ловится гейтом `test`; в рантайме то же сообщение (`[content-validation] …
  unknown field 'x'`) пишется в лог при загрузке мира и на `/reload`.
* **CRLF:** большинство `.java` хранит CRLF. Правку делать так: нормализовать в LF → редактировать →
  вернуть CRLF, иначе дифф «весь файл» (см. `reignofnether-line-endings`).

## Артефакт для теста: jar

`build` собирает `build/libs/reignofnether-<mc>-<ver>.jar` и прогоняет `test`. Перед выдачей jar
наружу обязательна проверка метаданных: `META-INF/neoforge.mods.toml` — это **шаблон**, и его
плейсхолдеры разворачивает `processResources` из `build.gradle`.

```bash
unzip -p build/libs/reignofnether-<mc>-<ver>.jar META-INF/neoforge.mods.toml | grep -c '\${'   # 0
```

`build` кладёт рядом ещё `…-sources.jar`: это не mod-файл, разворот его не касается, и плейсхолдеры
в нём остаются как есть — проверять надо основной jar.

Почему это не видно в dev: дев-прогон грузит мод из `build/classes/java/main` и плейсхолдеры
терпит, а реальная инсталляция — нет. FML парсит `loaderVersion` и каждый `versionRange` в
`VersionRange` прямо при чтении mod-файла (`ModFileInfo` →
`MavenVersionAdapter.createFromVersionSpec`), а Maven трактует `"${...}"` как одну точную версию,
поэтому обязательная зависимость `neoforge` становится неудовлетворимой и jar отвергается.
Ожидаемые значения после разворота: `loaderVersion="[4,)"`, `neoforge [21.1.248,)`,
`minecraft [1.21.1]`.

Комментарий у `version` намеренно не содержит `${...}`: Groovy-шаблонизатор падает на
неразрешённом выражении в самом комментарии. Сам `version` остаётся литералом — в dev мод грузится
из каталога классов, разворачивать его нечему, и FML упал бы с
`Illegal version number specified mod_version (main)`.

## Порядок перед выдачей артефакта

1. `compileJava`, `validateMixins`, `runData`, `test` — зелёные; для правок миксинов/событий ещё
   `runServer` через харнесс.
2. `build` — собирает jar и прогоняет тесты.
3. Проверка `mods.toml` из блока выше — ноль `${`.
4. `runClient` — единственный гейт, который не автоматизируется (см. правила): ноль строк `ERROR`.
