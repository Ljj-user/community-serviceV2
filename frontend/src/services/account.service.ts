import { ApiService } from '~/common/api/api-service'
import type {
  ForgetPasswordResponse,
  LoginResponse,
  LoginViewModel,
  RegisterResponse,
  RegisterViewModel,
} from '~/models/Account'

// 对接后端 /api/auth/** 接口
const apiService = new ApiService('auth')

interface BackendResult<T> {
  code: number
  message: string
  data?: T
}

interface BackendLoginData {
  token: string
  userInfo: {
    role: number
    identityType: number
    [key: string]: any
  }
}

interface BackendRegisterData {
  identityType?: number
  role?: number
  [key: string]: any
}

interface VerificationTicketData {
  ticketId: number
  scene: string
  target: string
  expiresAt: string
  devCode?: string
}

interface OnboardingPayload {
  skillTags?: string[]
  preferredFeatures?: string[]
  intentNote?: string
}

class AccountService {
  async login(loginInfo: LoginViewModel): Promise<LoginResponse> {
    const result = await apiService.post<BackendResult<BackendLoginData>>(
      'login',
      loginInfo,
    )

    return {
      token: result.data?.token ?? '',
      isSucceed: result.code === 200 && !!result.data?.token,
      // 透传后端的用户信息，方便前端根据角色/身份做跳转
      userInfo: result.data?.userInfo,
    }
  }

  async register(registerModel: RegisterViewModel): Promise<RegisterResponse> {
    const result = await apiService.post<BackendResult<BackendRegisterData>>(
      'register',
      registerModel,
    )

    return {
      token: '',
      isSucceed: result.code === 200,
      identityType: result.data?.identityType,
    }
  }

  async sendVerificationCode(email: string, scene = 'REGISTER') {
    return apiService.post<BackendResult<VerificationTicketData>>(
      'verification/send',
      {
        email,
        scene,
      },
    )
  }

  async submitOnboarding(payload: OnboardingPayload) {
    return apiService.post<BackendResult<any>>('onboarding', payload)
  }

  async getOnboarding() {
    return apiService.get<BackendResult<any>>('onboarding')
  }

  async forgetPassword(
    forgetPasswordModel: LoginViewModel,
  ): Promise<ForgetPasswordResponse> {
    const result = await apiService.post<BackendResult<any>>(
      'forget-password',
      forgetPasswordModel,
    )

    return {
      isSucceed: result.code === 200,
    }
  }

  async changePassword(
    oldPassword: string,
    newPassword: string,
  ): Promise<boolean> {
    const result = await apiService.post<BackendResult<any>>(
      'change-password',
      {
        oldPassword,
        newPassword,
      },
    )
    return result.code === 200
  }
}

export default new AccountService()
