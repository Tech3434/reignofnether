# Чек-лист: всё за ОДИН прогон `runClient`

> **Зачем.** `runClient` с data-driven контентом не запускался ни разу (см. `STATUS.md`). Этот файл —
> сценарий одного клиентского сеанса, который проверяет **весь** новый функционал подряд, без
> повторных запусков. Идти сверху вниз; каждый пункт — «сделай → ожидай».
>
> Запуск (из `___temp`):
> ```
> & "C:\Program Files\Java\jdk-21\bin\java.exe" '-Dorg.gradle.appname=gradlew' `
>   -jar gradle/wrapper/gradle-wrapper.jar runClient --offline --console=plain
> ```
> Автоматические гейты перед этим: `compileJava` · `validateMixins` · `runData` · `test`.
>
> Демо-контент: фракция `reignofnether:villagers` (`faction/villagers.json`), здания
> `reignofnether:town_centre` / `reignofnether:barracks`, юниты `villager_unit` (worker),
> `vindicator_unit` (melee), `skeleton_unit` (ranged + меню), `skeleton_marksman` (inherits),
> аддоны garrison / night_source, исследование `example_research`, апгрейд «Barracks II».
>
> Имя игрока ниже — `<you>`; подставь своё. В одиночной игре есть права на команды.

---

## 0. Загрузка (не падает и грузит датапак)

- [ ] Клиент доходит до главного меню **без крашей** в логе (искать `Exception`/`Failed to load`).
- [ ] Создать/загрузить плоский мир, зайти в него. В логе нет предупреждений вида
      `Data menu entry could not be built` и нет «skipped definition».
- [ ] UI-подсказка: если в чате/логе есть жалобы на `unit`/`building` JSON — стоп, записать.

## 1. Старт матча и ресурсы

- [ ] `/startrts reignofnether:villagers` → открывается меню выбора фракции / матч стартует,
      у тебя появляется столица `town_centre` и **1** воркер.
- [ ] Воркер выделяется, показываются кнопки-приказы (attack/stop/gather/build/garrison/hold).
- [ ] `/rtsapi player resources set food 5000 <you>`, затем то же для `wood` и `ore`
      (`/rtsapi player resources get <you>` показывает баланс). Ресурсы нужны, чтобы не упираться
      в стоимость на следующих шагах.
- [ ] Лимит армии: без барака ставится максимум 1 юнит (проверится на шаге 3).

## 2. JSON-здание: постановка, выбор, рендер, сохранение

- [ ] `/rtsapi building place reignofnether:barracks <you> <x> <y> <z> true none` — ставится
      **мгновенно** и **не исчезает** через секунду (это была регрессия: пустой `startingBlockTypes`
      → `shouldBeDestroyed` на первом тике).
- [ ] Здание видно, выделяется, у него правильный портрет/имя; HP > 0 и растёт до `maxHealth`.
- [ ] Аддон garrison: в панели выделенного барака есть слот гарнизона (capacity 3) — посади воркера
      внутрь и вынь обратно.
- [ ] Аддон night_source у столицы (`range 24`) и range_indicator, если включён в конфиге: нет ошибок
      рендера при выделении.
- [ ] **Сейв**: `Esc` → Save & Quit → снова войти. Здание и его HP/владелец на месте, кнопки
      производства/исследований восстановились, очередь пуста.

## 3. Производство (стоимость, `costOverride`, лимит)

- [ ] Выделить барак → кнопки производства: `vindicator_unit`, `skeleton_unit`, `skeleton_marksman`.
- [ ] Запустить `vindicator_unit` → списывается **его** цена из определения (food 100), не цена здания;
      юнит появляется через 20 с у здания и получает владельца.
- [ ] `skeleton_marksman` запускается со **своей** ценой из `costOverride` барака (food 55, а не 80
      из определения юнита) и учится 8 с, а не 18 — проверь, что списывается ровно 55.
- [ ] Отмена производства возвращает стоимость.
- [ ] Лимит: столица даёт `populationSupply 10` (+1 база) — набрать 11 юнитов нельзя, 11-й не стартует.

## 4. K6 — data-driven меню способностей

- [ ] Произвести `skeleton_unit`, выделить его. В ряду способностей есть кнопка меню «Abilities»
      (`abilities.reignofnether.menu`) — **видна**, не спрятана.
- [ ] Открыть меню: элементы раскладываются по `row`/`col` из `skeleton_unit.json`
      (heal@0,0 / stop@0,1 / hold@1,0 / вложенное меню@1,2), back-кнопка слева от колонки 0.
- [ ] Нажать `heal` → лечит выделенного юнита, уходит на cooldown (200 тиков), тултип с именем.
- [ ] Нажать `stop` / `hold` → приказ выполняется (юнит останавливается / держит позицию).
- [ ] Открыть **вложенное** меню (элемент @1,2) → внутри элемент `attack`; back закрывает один
      уровень, а не всё меню.
- [ ] Меню с **только** building-элементами остаётся открываемым (кнопка не исчезает) — для проверки
      вручную добавить в `skeleton_unit.json` элемент `{ "building": "reignofnether:barracks" }`
      (см. шаг 10).
- [ ] Пассивные способности `regeneration` (params `amount`/`interval`) тикают: HP медленно растёт.

## 5. K4 — воркер данными

- [ ] `.json` воркера содержит блок `worker`; в игре воркер **добывает** ресурс (это была регрессия:
      `maxResources = 0` → воркер не мог собрать ничего).
- [ ] Кнопка сбора переключает цикл `NONE → FOOD → WOOD → ORE → NONE` (только ресурсы из `gatherable`).
- [ ] Воркер несёт ресурс в столицу (`flags.canAcceptResources`) и баланс в HUD растёт.
- [ ] `carryCapacity` берётся из блока `worker` (не с верхнего уровня `carryCapacity`, который удалён).
- [ ] `buildSpeed` (по умолчанию 1.0): два воркера строят быстрее одного; поставь в `worker`
      `"buildSpeed": 2.0` и убедись, что один воркер ускоряет стройку.

## 6. Исследования и гейты

- [ ] Выделить барак → кнопка исследования `example_research` (не серая), запуск списывает wood 50.
- [ ] По завершении исследование выдано владельцу (`/research` или HUD-панель показывает его).
- [ ] Гейт: у способности/производства/здания с `requiredResearch` кнопка **серая** до исследования
      и **активная** после.
- [ ] (Опционально) `type: equip` — поменять `example_research.json` на
      `"type": "equip"`, указав предмет/броню, и проверить выдачу экипировки юнитам владельца.

## 7. Апгрейды здания (per-level)

- [ ] Запустить апгрейд барака «Barracks II» (wood 75) у выделенного барака.
- [ ] По завершении: HP 250, появился аддон `night_source` (range 16), кнопка/имя «Barracks II».
- [ ] Структура не съехала (габарит/фундамент те же; вариант уровня — отдельный `JsonBuilding`).
- [ ] Save & Quit → войти снова: уровень апгрейда сохранился.

## 8. Ranged-юнит и `inherits`

- [ ] `skeleton_unit` (role ranged) стреляет стрелой по врагу/животному с дистанции (снаряд
      `minecraft:arrow`, урон 3).
- [ ] `skeleton_marksman` (`inherits: reignofnether:skeleton_unit`) — дистанция/урон как в оверрайде
      (range 20, damage 4), а не базовые 16/3; роль/цели — ranged, не melee.

## 9. Инструменты автора и команды

- [ ] Кастомные кнопки `rts_buttons`: кнопки появляются у юнита по id определения и у здания по id
      (маппинг в `custom_button_mappings.json`), нажатие выполняет своё действие.
- [ ] `/rtsapi unit action …` / `/rtsapi building …` не падают и делают то, что заявлено в имени.
- [ ] `/rtsapi building destroy <pos>` снимает здание, клиент убирает его у себя.

## 10. Остаток из прежних планов (только в игре)

- [ ] **G.2** — отсечение секций выше камеры (orthoview) работает при движении камеры.
- [ ] **G.5** — камера не задыхается/не проваливается при приближении к земле.
- [ ] Над юнитами **нет** полосок здоровья (удалены по плану).

## 11. Строгая валидация полей (без перестроения мода)

Кодеки определений молча отбрасывают незнакомые поля, поэтому их ловит отдельный проход
(`ContentValidator` → лог `[content-validation]`) и гейт `test`. Проверить в игре так:

- [ ] В логе загрузки мира есть `[content-validation] N definition file(s) checked, no unknown fields`
      (значит датапак-файлы разобраны и лишних полей нет).
- [ ] Создать в мире датапак с нарочной опечаткой (клиент перезапускать не нужно):
      `<папка мира>/datapacks/typo-test/pack.mcmeta`:
      ```json
      { "pack": { "pack_format": 48, "description": "validation test" } }
      ```
      и `<папка мира>/datapacks/typo-test/data/typo/unit/broken_unit.json`:
      ```json
      { "base": "minecraft:villager", "carryCapcity": 100, "attributes": { "minecraft:generic.max_health": 20 } }
      ```
- [ ] `/reload` в игре → в логе
      `[content-validation] typo:unit/broken_unit.json <root>: unknown field 'carryCapcity' (accepted: abilities, attributes, base, …)`
      плюс итоговая строка с числом файлов/полей.
- [ ] Убедиться, что `attributes` (свободная карта) **не** вызывает ошибок — ключи там авторские.
- [ ] Удалить `typo-test`, `/reload` → снова чистая INFO-строка.

---

## Демо-данные, которые стоит переключать по ходу (правка JSON + перезапуск)

| Что | Где | Для чего |
|---|---|---|
| `flags.foundationYLayers: 2` | `data/reignofnether/building/barracks.json` | больше слоёв основания пре-квеится (проверка нового флага) |
| элемент `{ "building": "reignofnether:barracks" }` | `data/reignofnether/unit/skeleton_unit.json` | меню с building-элементом |
| `worker.gatherable: ["wood"]` | `data/reignofnether/unit/villager_unit.json` | цикл сбора ограничен одним ресурсом |
| `"type": "equip"` | `data/reignofnether/research/example_research.json` | эффект выдачи экипировки |

## Что записать по итогам

1. Номер пункта, что сделал, что увидел, лог с `Exception`.
2. Если что-то не сошлось — создавай запись в `BUGS_RUNCLIENT.md` (новый раздел «Сессия с
   data-driven контентом») с воспроизведением и своей формулировкой.
