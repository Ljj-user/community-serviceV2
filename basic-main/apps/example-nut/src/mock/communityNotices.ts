export interface CommunityNotice {
  id: number
  title: string
  time: string
}

export const communityNoticeMap: Record<string, CommunityNotice[]> = {
  幸福里社区: [
    { id: 1, title: '本周六 9:00 社区义诊活动，请提前登记。', time: '今天 09:20' },
    { id: 2, title: '电梯 2 号机计划检修，预计 14:00 恢复。', time: '今天 08:45' },
    { id: 3, title: '志愿服务积分双倍日，欢迎报名参与。', time: '昨天 18:30' },
    { id: 4, title: '垃圾分类宣讲在 3 号楼活动室举行。', time: '昨天 10:12' },
    { id: 5, title: '社区图书角新增儿童绘本，欢迎借阅。', time: '前天 16:08' },
    { id: 6, title: '停车位优化方案已公示，请业主投票。', time: '前天 09:26' },
  ],
  阳光社区: [
    { id: 1, title: '阳光社区夜间跑步团本周三 20:00 集合。', time: '今天 11:02' },
    { id: 2, title: 'A 区路灯维护中，请注意夜间出行安全。', time: '今天 08:18' },
    { id: 3, title: '周末亲子活动报名通道已开启。', time: '昨天 19:44' },
    { id: 4, title: '社区停车规范倡议书已发布。', time: '昨天 14:33' },
  ],
  和谐社区: [
    { id: 1, title: '和谐社区便民服务站新增证件照打印。', time: '今天 10:28' },
    { id: 2, title: '本周物业开放日将于周五举行。', time: '今天 09:40' },
    { id: 3, title: '消防演练通知：请 3 号楼居民准时参加。', time: '昨天 16:20' },
    { id: 4, title: '图书漂流活动本周继续，欢迎捐书。', time: '前天 12:05' },
  ],
}

