---
name: reignofnether-commit
description: Соглашения коммитов в репозитории Reign of Nether — стиль сообщений, что стейджить, чек-лист перед коммитом. Использовать при любой просьбе закоммитить изменения в этом проекте.
---

# Коммиты в Reign of Nether

## Стиль сообщения

Исторически сообщения — короткие, человеческие, с префиксом этапа:

```
E.1/E.2/E.3: remove 7 dead enchantments (JSON + registrar + call sites)
WIP stage D: strip the HUD start buttons, minimap and tutorial gating
docs: STATUS.md - verified gates (runData/runServer green), correct HEAD
G.3/G.4: stop orthoview from meddling with the player and the GUI
```

* Тема — императив/описание, ≤ ~72 символов, без точки.
* Тело — список `-`-пунктов **зачем**, а не пересказ диффа. Ссылайся на пункты плана
  (`E.2`, `G.3`) когда есть.
* Разделяй несвязанные изменения на отдельные коммиты (удаление читов, камеру, геймрулы —
  тремя коммитами, как это уже сделано в этой ветке).
* **Никаких AI-подписей**, `Co-Authored-By`, «Generated with …» — автор должен выглядеть как
  автор репозитория. Коммит должен быть похож на написанный человеком.

## Что стейджить

* `git add -u` — только отслеживаемые правки/удаления. Этого достаточно в 99% случаев.
* **Не** используй `git add -A`/`git add .` вслепую: в дереве есть скретч-артефакты
  (`run/`, `*.log`, `errs*.txt`, `run-game-test-server/`). Проверь `git status` перед коммитом.
* Не коммить секреты/креды; в этом проекте их быть не должно.

## Чек-лист перед коммитом

1. Гейты: `compileJava --rerun` + `validateMixins --rerun` зелёные (см. скилл `reignofnether-build`).
2. Проверка окончаний строк: нет коммитов «весь файл изменён» (см. `reignofnether-line-endings`).
3. `git diff --cached --stat` просмотрен: нет неожиданных файлов.
4. `git log -5 --format=%s` — сообщение в стиле репозитория.

Многострочное сообщение — через HEREDOC:

```bash
git commit -m "$(cat <<'EOF'
E.2-E.9: dead attributes/effects, army cap, lazy save data, drop sandbox

- AttributeRegistrar/AttackerUnit: drop the 6 unused combat attributes
- ...
EOF
)"
```
