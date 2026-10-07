---
name: reignofnether-add-building
description: Как добавить здание в Reign of Nether — класс, NBT-структура, HP, регистрация, кнопка, аддоны. Использовать при создании нового строения.
---

# Как добавить здание

Полный разбор — `_GUIDES/03_building.md`. Шаблоны — `building/buildings/villagers/TownCentre`
(ратуша) и `Barracks` (производство).

## Шаги

1. **Класс** `building/buildings/<Group>/<Name>.java` — наследует `Building`; при необходимости
   override `getFaction()` (строка для lang-ключей), `getBuildButton`, затраты (`cost`).
2. **Структура** — NBT `.nbt` (в старом дереве лежали в `data/reignofnether/structures/`), из него
   выводятся блоки, HP по блокам, `minBlocksPercent`.
3. **Производство** — `this.productions.add(ProductionItems.X, Keybindings.abilitySlotN)` в
   конструкторе, если здание что-то учит.
4. **Регистрация** — реестр `ReignOfNetherRegistries.BUILDING` (через `Buildings.init()`).
5. **Размещение** — при нестандартном поведении создай `placements/<Name>Placement`; для обычного
   производства хватает общего `ProductionPlacement` (он восстановлен и нужен ядру).
6. **Кнопка** — `BuildingPlaceButton` в `getBuildButton(hotkey)`; предусловие ставить через
   `hasFinishedBuilding(...)`.
7. **Аддоны** — `building/addon/**` (гарнизон, аура, конвертация, магазин). HP/починка/каптюр —
   поля `Building` (`capturable`, `invulnerable`, `repairable`, `populationSupply`).
8. **Ассеты и переводы** — `reignofnether-add-assets`.

## Грабли

* Не удаляй каталог `buildings/placements/` целиком: там общий `ProductionPlacement` и код ядра.
* Лимит армии: прирост даёт ратуша — выставляй `populationSupply` только у ратуши.
* Инструмент кастомных строений (`building/custombuilding/**`) — отдельный, самодостаточный.
