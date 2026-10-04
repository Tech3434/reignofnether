import io
import re
import subprocess

# --- UnitItemRarity: 1.21.1 made Rarity a closed vanilla enum -----------------------
PATH = 'src/main/java/com/solegendary/reignofnether/items/UnitItemRarity.java'
text = io.open(PATH, encoding='utf-8', newline='').read()
text = re.sub(r'Rarity\.create\("[A-Z]+",\s*ChatFormatting\.\w+\)',
              'Rarity.EPIC', text)
if 'ChatFormatting' in text and 'ChatFormatting.' not in text:
    text = text.replace('import net.minecraft.ChatFormatting;\n', '')
text = text.replace('public class UnitItemRarity {',
                    '/**\n'
                    ' * 1.21.1 turned Rarity into a closed vanilla enum with no custom values, so the\n'
                    ' * mod\'s legendary/mythic tiers both map onto EPIC. The two names are kept because\n'
                    ' * call sites use them to mean "the best stuff".\n'
                    ' */\n'
                    'public class UnitItemRarity {')
text = text.replace('import com.solegendary.reignofnether.items.UnitItemRarity;\n', '')
io.open(PATH, 'w', encoding='utf-8', newline='').write(text)
print('UnitItemRarity mapped onto the vanilla enum')

# --- ItemUtil: copy the drop-list methods by their declaration lines ----------------
PATH = 'src/main/java/com/solegendary/reignofnether/items/ItemUtil.java'
up = subprocess.run(['git', 'show', f'c68e0de:{PATH}'], capture_output=True, text=True).stdout
lines = up.split('\n')

blocks = []
i = 0
while i < len(lines):
    if re.search(r'\bgetRandomItemDropsList\s*\(', lines[i]):
        start = i
        while start > 0 and not re.search(r'\b(public|private|static)\b', lines[start]):
            start -= 1
        depth = 0
        end = start
        for j in range(start, len(lines)):
            depth += lines[j].count('{') - lines[j].count('}')
            if '{' in lines[j] and depth <= 0:
                end = j
                break
        blocks.append((start, end, '\n'.join(lines[start:end + 1])))
        i = end + 1
    else:
        i += 1

if not blocks:
    print('no drop-list methods found')
else:
    block = '\n\n'.join(b[2] for b in blocks)
    mine = io.open(PATH, encoding='utf-8', newline='').read()
    if 'getRandomItemDropsList' in mine:
        print('already present')
    else:
        mlines = mine.split('\n')
        close = max(k for k, l in enumerate(mlines) if l.rstrip() == '}')
        mlines[close:close] = ['', block, '']
        io.open(PATH, 'w', encoding='utf-8', newline='').write('\n'.join(mlines))
        print(f'inserted {len(block.splitlines())} lines ({len(blocks)} methods)')
