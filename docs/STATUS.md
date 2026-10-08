# STATUS — единый статус-документ

**Дата:** 2026-10-08
**Ветка:** `wip/stage-d-deletions`
**HEAD:** `fad1f813`
**compileJava:** ✅ 0 ошибок
**validateMixins:** ✅ 48 injection point'ов / 31 mixin-класс, все резолвятся
**runData:** ✅ BUILD SUCCESSFUL
**runServer:** ✅ `Done (1.005s)!`, 0 `mixin apply failed`, 0 SEVERE/ERROR
**runClient:** ⚠ прогонялся владельцем — найдено 9 багов, разбор в `BUGS_RUNCLIENT.md`

Версия: 1.4.4d, MC 1.21.1, NeoForge 21.1.250.

---

## 1. Что сделано

### Этап E — дыры каркаса (закрыт)

| Пункт | Что |
|---|---|
| E.2 | Удалены 6 атрибутов без потребителей + геттеры в `AttackerUnit` + lang-ключи |
| E.3 | Удалены 5 эффектов + `MobEffectIcon` + `@Inject` морозного урона |
| E.4 | Лимит армии: база 1, ратуша даёт `populationSupply` (TownCentre = 10) |
| E.5 | Ленивое `SavedData`: `getLoaded()`/`isStored()`, пустой мир не пишет файлов |
| E.9 | Прямое сопоставление `ProductionItem` ↔ `EntityType<?>`; убран чит Killer Rabbit |
| HeroUnit | Удалены `HeroUnitSave`/`HeroUnitSaveData`/`ResearchSaveData` и их вызовы |
| — | Песочница удалена целиком (меню, кнопки, пакеты, все `isSandboxPlayer`-гейты) |

### Удаление читов

`Cheats`, `CheatsClientboundPacket`, `PlayerAction.SET_CHEAT`, `onPlayerChat` и все читы
(`warpten`, `foodforthought`, `operationcwal`, `slipslopslap`, `modifythephasevariance`,
`wouldyoukindly`, `greedisgood`, `allcheats`) **вырезаны** вместе с гейтами и lang-ключами.
`wouldyoukindly` унёс и управление ванильными мобами: не-принадлежащих юнитов больше не
командуешь.

### Этап G — камера

| Пункт | Статус |
|---|---|
| G.1 | Высота камеры пинится (`setCameraHeight`/`clearCameraHeightOverride`) — сделано раньше |
| G.2 | Отсечение секций выше камеры — **не сделано**, проверяемо только в игре |
| G.3 | Вход в орторежим больше не стирает зелья и не поднимает Peaceful→Easy |
| G.4 | `TopdownGui` больше не форсит `guiScale` и не глотает клавиши инвентаря/достижений |
| G.5 | Проверка удушья камеры — только в игре (удушье возвращено ваниле ещё в этапе B) |

### Этап F — хвосты

| Пункт | Что |
|---|---|
| F.4 | `CustomBuilding`: три `buildableBy*` флага сведены к одному (`buildableByVillagers`) |
| F.7 | Удалены ключи `hud.faction.reignofnether.*` из 17 языковых файлов |
| C.3 | Удалены мёртвые геймрулы `coopMode` и `scenarioMode` (режимы сценария/кооператива) |

### Документация

* `docs/reference/` — новая папка: инвентарь удалённого контента + способ восстановления
  (`git show 5079004e:<путь>`).
* `.agents/skills/` — навыки для будущих ИИ-агентов (как коммитить, как собирать, как добавить
  фракцию и т. д.).

---

## 2. Что осталось

### Гейты и проверки в игре

1. **`runClient`** — единственный непройденный гейт; проверяет `@Override`-модель
   (`ClientPacketListenerMixin`, 16 методов), которую `runServer` не трогает.
2. **Ручной прогон** — G.2 (секции выше камеры), G.5 (удушье), геометрия на экране, зависание
   при выходе из мира.

### Этап H — РТС-режим для обычной игры — закрыт

H.1–H.8 сделаны (`15bd1a69`): пропуск = право оператора 2, вход по команде и хоткею, конец сессии
по потере последнего здания с условием «здание вообще было», `force-loose`, юнит за границей мира
заталкивается внутрь, спавн рядом с игроком, хук стартовой армии.

### Система юнитов и строений «как в Warcraft 3» (§14 плана) — закрыта

§14.1–§14.5 сделаны (коммиты `76f33930`, `4f9b893b`, `63bf1a14`, `46ed5856`, `a422e885`,
`21d511f3`); подробности и остаток — `PLAN_RTS_ONLY.md` §15. Вкратце: подменю способностей
(`Ability.subAbilities`, `MenuAbility`, стек меню в HUD); приказы и кнопки постройки — способности
(`CommandAbility`, `CommandAbilities`, `BuildMenuAbility`, `ActionButtons` удалён); производство
зданий — способности (`ProductionAbility`, HUD рисует один список); вскапывание
`DIG_BLOCK`/`DIG_AREA` (`DigAbility`, дроп в `UnitInventory`) с рамкой box-select для области;
вскапывание блока строения **отменяется** и наносит **процентный** урон по HP — 5 % максимального HP
за удар (`DigAbility.BUILDING_DAMAGE_PERCENT_PER_HIT`), снос сверху вниз
(`BuildingPlacement.demolishTopDown`). Открытых вопросов нет; дизайн-значения и HUD-прогресс сноса —
на усмотрение владельца.

### Документация

* `FEATURES.md` и `INTRUSION_AUDIT.md` — актуализировать (снять удалённое, отметить читы/туман).
* Сверить `docs/README.md` (ссылка на `reference/` уже поправлена).

---

## 3. Как собирать

`gradlew.bat` не работает (передаёт java одновременно `-classpath ""` и `-jar`). Запускать через java:

```bash
cd ___temp
JAVA="/c/Program Files/Java/jdk-21/bin/java.exe"
"$JAVA" -Dorg.gradle.appname=gradlew -jar gradle/wrapper/gradle-wrapper.jar <task> --offline --console=plain
```

`runServer` при `--offline` и закрытом stdin завершается сам (`Done (...)`, затем `Stopping server`).

---

## 4. Ловушки (не повторять)

1. **Переводы строк.** Репозиторий хранит CRLF в большинстве `.java` и частью в `lang/*.json`.
   `sed -i`/`perl`-скрипты, меняющие `\r\n`↔`\n`, превращают дифф в «весь файл изменён».
   Правку CRLF-файла делать так: `perl -i -pe 's/\r\n/\n/g'`, затем редактировать, затем
   `perl -i -pe 's/\r?\n$/\r\n/'`. Перед коммитом проверять `git diff --numstat`.
2. **`git show <ref>:<path> > <файл>`** под PowerShell 5.1 пишет UTF-16LE → javac: `unmappable
   character`. Только `cmd /c "git show ... > file"`.
3. **`validateMixins` не ловит часть mixin-ошибок** — они всплывают в рантайме. Надёжный гейт —
   `runServer` / `runClient`.

---

## 5. Ссылки

| Файл | О чём |
|---|---|
| `PLAN_RTS_ONLY.md` | исходный план A–H, все решения |
| `reference/README.md` | инвентарь удалённого контента |
| `../_GUIDES/README.md` | как добавить юнит/здание/способность/фракцию |
| `FEATURES.md` | каталог функций с вердиктами |
| `BUGS_RUNCLIENT.md` | баги после первого `runClient`: разбор причин, вопросы, план правок |
| `INTRUSION_AUDIT.md` | что мод ломал в обычном мире |
| `../.agents/skills/` | навыки для ИИ-агентов |
