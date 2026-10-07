---
name: reignofnether-add-faction
description: Как собрать полноценную играбельную фракцию в Reign of Nether — старт, юниты, здания, дерево, цвета, локализация. Использовать при добавлении новой расы/фракции на РТС-каркас.
---

# Как собрать фракцию

Опора — `_GUIDES/01_faction.md` (полный разбор, ~50 КБ) и `_GUIDES/00_обзор.md` (карта).
Ниже — порядок и то, что легко забыть.

## Предпосылка

`Faction` (enum жителей/нежити/пиглинов) **удалён** и не воссоздаётся (решение 4 плана).
Новая фракция — это набор классов, а не ветка `if`. Роль «фракции» играет владелец строки
(`ownerName`) и набор зарегистрированных сущностей/зданий.

## Порядок (снизу вверх)

1. **Атрибуты/эффекты**, если нужны новые — `registrars/AttributeRegistrar`,
   `registrars/MobEffectRegistrar`.
2. **Юниты** — `reignofnether-add-unit` (воркер/скаут/боевой).
3. **Способности** — `reignofnether-add-ability` (+ `UnitAction`).
4. **Элементы производства** — `reignofnether-add-production`.
5. **Здания** — `reignofnether-add-building`; в конструкторе здания — `this.productions.add(...)`.
6. **Исследования** — `reignofnether-add-research` (если способность гейтится).
7. **Герой** — `reignofnether-add-hero`.
8. **Ассеты и локализация** — `reignofnether-add-assets`.
9. **Старт и цвета** — `PlayerServerEvents.startRTS`, `PlayerColors`.

## Стартовый отряд

`PlayerServerEvents.startRTS` создаёт воркера и скаут у стартовой позиции и (при «readied start»)
сразу ставит `Buildings.TOWN_CENTRE`. Хук «сколько юнитов давать на старте» — этап H.8 плана.
Учитывай лимит армии: база 1 юнит, пока не построена ратуша.

## Обязательные гейты

`compileJava` → `validateMixins` → `runData` → `runServer` (см. `reignofnether-build`).
Новая фракция почти наверняка требует правок миксинов/регистраций — `runServer` обязателен.
