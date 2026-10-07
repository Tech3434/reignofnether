---
name: reignofnether-add-ability
description: Как добавить способность юнита или здания в Reign of Nether — класс, кнопка, UnitAction, цель, кулдаун. Использовать при создании новой способности.
---

# Как добавить способность

Полный разбор — `_GUIDES/04_ability.md` (юнит), `_GUIDES/05_hero.md` (герой). Каркас —
`ability/Ability`, `ability/Abilities`, `AbilityButton`, `ability/UnitAction`.

## Шаги

1. **Класс** `ability/abilities/<Name>.java extends Ability` (или `HeroAbility`). Задай:
   кулдаун, ману, `oneClickOneUse`, `action` (`UnitAction`), радиус/цель.
2. **Константа действия** — добавь в `ability/UnitAction`, если способность активируется
   действием (иначе она не привяжется к вводу).
3. **Кнопка** — `AbilityButton`; иконка из `assets/reignofnether/textures/icons/abilities/**`.
4. **Привязка к юниту/зданию** — в конструкторе юнита (список способностей) или здания
   (`this.abilities`).
5. **Пакеты** — если способность с серверной логикой, посмотри `AbilityServerboundPacket` /
   `AbilityClientboundPacket` как образец.
6. **Гейт по исследованию** — `reignofnether-add-research` (гейт **только клиентский**).
7. **Локализация/иконка** — `reignofnether-add-assets`.

## Грабли

* 36 из 93 старых способностей были жёстко привязаны к конкретному классу юнита — способность
  без юнита бессмысленна, делай юнита первым.
* Гейтирование по исследованиям было только клиентским (сервер не проверяет) — не полагайся на
  него как на защиту.
