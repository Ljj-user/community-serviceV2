export interface Profile {
  id: number
  username: string
  email: string
  /** 与后端 realName 一致 */
  realName?: string
  firstName: string
  lastName: string
  role: string
  /** 家庭住址等单行文本，对应后端 address */
  location: string
  socials: Social[]
  avatar: string
  bio: string
  phone: string
  address: Address
  createdAt?: string
  /** 1 居民 2 志愿者 */
  identityType?: number
  /** 0 未知 1 男 2 女 */
  gender?: number
  /** 志愿者能力标签 */
  skillTags?: string[]
  /** 所属社区ID（网格） */
  communityId?: number
}

export interface Social {
  name: string
  url: string
}

export interface Address {
  country: string
  city: string
  postalCode: string
}

export interface ProfileSettings {
  notifications: NotificationSetting[]
}

export interface NotificationSetting {
  type: string
  email: boolean
  push: boolean
  sms: boolean
}
