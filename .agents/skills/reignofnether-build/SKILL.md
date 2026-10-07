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
| `runServer` | применение main-миксинов **в рантайме**; завершается сам, если stdin закрыт (`< /dev/null`) |
| `runClient` | `@Override`-модель клиентских миксинов; **не** завершается сам, останавливать вручную |

## Правила

* В конце каждой задачи — минимум `compileJava --rerun` **и** `validateMixins --rerun`.
* Правки миксинов, событий или пакетов → обязателен `runServer` (ловит то, что `validateMixins`
  пропускает: `Invalid LVT row`, `InvalidInjectionException`, сегфолты в ASM).
* `runServer`: перенаправляй stdin из `/dev/null`, иначе процесс висит.
* Ни один гейт не проверяет геометрию на экране и зависание при выходе из мира — это ручной
  прогон `runClient`.
