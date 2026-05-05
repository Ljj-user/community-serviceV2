export interface LoginViewModel {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  isSucceed: boolean
}

export interface RegisterViewModel {
  username: string
  password: string
  /** 真实姓名，对应后端 RegisterRequest.realName */
  realName: string
  phone: string
  /** 1 居民老人 2 志愿者 */
  identityType: number
  /** 1 男 2 女，对应默认头像 */
  gender?: number
  email: string
  verificationCode: string
  verificationScene?: string
  communityId?: number
}

export interface RegisterResponse {
  token: string
  isSucceed: boolean
  identityType?: number
}

export interface ForgetPasswordViewModel {
  email: string
}

export interface ForgetPasswordResponse {
  isSucceed: boolean
}

export interface Account {
  username?: string
  token: string
  // 后端用户角色：1超级管理员 2社区管理员 3普通用户
  role?: number
  // 普通用户身份：1居民老人 2志愿者（互斥）
  identityType?: number
}
