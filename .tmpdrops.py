import io
import re
import subprocess

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

print('methods found:', len(blocks))
for start, end, _ in blocks:
    print(f'  lines {start + 1}-{end + 1}')

mine = io.open(PATH, encoding='utf-8', newline='').read()
if 'getRandomItemDropsList' in mine:
    print('already present in ItemUtil')
elif blocks:
    block = '\n\n'.join(b[2] for b in blocks)
    mlines = mine.split('\n')
    close = max(k for k, l in enumerate(mlines) if l.rstrip() == '}')
    mlines[close:close] = ['', block, '']
    io.open(PATH, 'w', encoding='utf-8', newline='').write('\n'.join(mlines))
    print(f'inserted {len(block.splitlines())} lines')
