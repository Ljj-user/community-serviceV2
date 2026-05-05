import { HttpResponse, http } from 'msw'

/**
 * 站内通知（对接 /notifications/**）；未接后端时可返回空数据
 */
const handlers = [
  http.get('/api/notifications/unread-count', () => {
    return HttpResponse.json({
      code: 200,
      message: 'ok',
      data: { total: 0, business: 0, announcement: 0 },
    })
  }),
  http.get('/api/notifications/mine', () => {
    return HttpResponse.json({
      code: 200,
      message: 'ok',
      data: { records: [], total: 0, size: 20, current: 1 },
    })
  }),
  http.put('/api/notifications/mark-all-read', () => {
    return HttpResponse.json({ code: 200, message: 'ok', data: null })
  }),
  http.put(/\/api\/notifications\/\d+\/read$/, () => {
    return HttpResponse.json({ code: 200, message: 'ok', data: null })
  }),
]

export default handlers
