import os
base = 'F:/Storage/ai/create-qbp-1.21.1-neoforge'
path = os.path.join(base, 'src/main/java/org/kdvcs/vbm/client/GuiOverlay.java')
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
print('Length:', len(content))
print('First:', repr(content[:100]))

