---
name: reignofnether-build
description: Как собрать мод Reign of Nether и прогнать гейты (compileJava, validateMixins, runData, runServer, runClient). Использовать при любой правке кода перед коммитом.
---

# Сборка и гейты

Проект — git-репозиторий `___temp/` (NeoForge 1.21.1, MC 1.21.1). **`gradlew.bat` не работает**:
он передаёт java одновременно `-classpath ""` и `-jar`. Запускать java напрямую:

```bash
cd ___temp
JAVA="/c/Program Files/Java/jdk-21/bin/java.exe"      # Windows, git-bash
"$JAVA" -Dorg.gradle.appname=gradlew -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

`--offline` обязателен (нет сети). `--rerun` заставляет повторить одну задачу, даже если Gradle
считает её up-to-date:

```bash
... compileJava --rerun --offline --console=plain
```

## Задачи-гейты (по возрастанию строгости)

| Задача | Что проверяет |
|---|---|
| `compileJava` | типы и синтаксис |
| `validateMixins` | что каждый `@Inject` резолвится в `neoforge-21.1.250.jar` (печатает «N injection point(s) … / M mixin class(es)») |
| `runData` | генерация данных/ресурсов, `Missing:` не должно быть |
| `runServer` | применение main-миксинов **в рантайме**; запускать **только через харнесс** (см. ниже), а не сырым gradle-таском |
| `runClient` | `@Override`-модель клиентских миксинов; **не** завершается сам, останавливать вручную |

## Правила

* В конце каждой задачи — минимум `compileJava --rerun` **и** `validateMixins --rerun`.
* Правки миксинов, событий или пакетов → обязателен `runServer` (ловит то, что `validateMixins`
  пропускает: `Invalid LVT row`, `InvalidInjectionException`, сегфолты в ASM).
* `runServer`: **только `bash run/port_srv15.sh`**. Сырой `runServer --offline` может не завершиться после
  `stop` (висит процесс, гейт не закрывается), а харнесс жёстко ограничивает фазу выключения 15 секундами
  и добивает только свои java-процессы. Ему нужен включённый RCON в `run/server.properties`
  (`enable-rcon=true`, `rcon.port=25585`, `rcon.password=…`): stdin gradle до сервера не доходит, консольные
  команды теряются. Логи: `run/port_srv15.log`, сводка `run/port_srv15_summary.log`, транскрипт команд
  `run/port_rcon_results.txt`. В каркасе нет mod-сущностей, поэтому часть проверок драйвера (суммон
  `reignofnether:wraith_unit`) помечается `skipped` — это норма, а не провал.
* Ни один гейт не проверяет геометрию на экране и зависание при выходе из мира — это ручной
  прогон `runClient`.

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

1. `compileJava`, `validateMixins`, `runData` — зелёные; для правок миксинов/событий ещё `runServer`
   через харнесс.
2. `build` — собирает jar и прогоняет тесты.
3. Проверка `mods.toml` из блока выше — ноль `${`.
4. `runClient` — единственный гейт, который не автоматизируется (см. шапку): ноль строк `ERROR`.
