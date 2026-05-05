import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
if (existsSync(join(root, 'lefthook.yml'))) {
  spawnSync('pnpm', ['exec', 'lefthook', 'install'], {
    cwd: root,
    stdio: 'inherit',
    shell: true,
  })
}
