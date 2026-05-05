const ua = process.env.npm_config_user_agent || ''
if (!ua.includes('pnpm')) {
  console.error('此项目请使用 pnpm 安装依赖：pnpm install')
  process.exit(1)
}
