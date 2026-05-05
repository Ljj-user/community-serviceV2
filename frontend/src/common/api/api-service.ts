import type { AxiosInstance } from 'axios'
import type { ListResult } from '~/models/ListResult'
import {
  defaultOptions,
  type PagedAndSortedRequest,
} from '~/models/PagedAndSortedRequest'
import type { PaginatedList } from '~/models/PagedListResult'
import HttpClient from './http-client'

export class ApiService {
  private readonly apiBase
  private httpClient: AxiosInstance
  constructor(apiBase: string, service = null) {
    this.apiBase = apiBase
    this.httpClient = HttpClient(service)
  }

  private buildUrl(url?: string) {
    const cleaned = (url ?? '').replace(/^\/+/, '')
    return cleaned ? `${this.apiBase}/${cleaned}` : `${this.apiBase}`
  }

  async get<T>(url = ''): Promise<T> {
    const response = await this.httpClient.get(this.buildUrl(url))
    return response.data
  }

  async getList<T>(url: string, params: any): Promise<ListResult<T>> {
    const response = await this.httpClient.get<ListResult<T>>(
      this.buildUrl(url),
      { params },
    )
    return response.data as ListResult<T>
  }

  async getPagedList<T>(
    url = '',
    options: PagedAndSortedRequest = defaultOptions,
  ): Promise<PaginatedList<T>> {
    const response = await this.httpClient.get<PaginatedList<T>>(
      this.buildUrl(url),
      { params: this.removeDefaultOptions(options) },
    )
    return response.data as PaginatedList<T>
  }

  async query<T>(url: string, params?: any): Promise<T> {
    const result = await this.httpClient.get<T>(this.buildUrl(url), {
      params,
    })
    return result.data
  }

  async post<T>(url: string, data: any): Promise<T> {
    const response = await this.httpClient.post<T>(this.buildUrl(url), data)
    return response.data
  }

  async put<T>(url: string, data: any): Promise<T> {
    try {
      const response = await this.httpClient.put<T>(this.buildUrl(url), data)
      return response.data as T
    } catch (error) {
      console.error(`${error} was occurred`)
      throw new Error('cannot put')
    }
  }

  async delete<T>(url: string): Promise<T> {
    try {
      const response = await this.httpClient.delete<T>(this.buildUrl(url))
      return response.data as T
    } catch (error) {
      throw new Error(`${error} was occurred`)
    }
  }

  async getBlobFile(url: string, params: any) {
    return this.httpClient
      .get(url, {
        params,
        responseType: 'blob',
      })
      .catch((error) => {
        throw new Error(`ApiService ${error}`)
      })
  }

  async postFile(url: string, params: { files: any[] }) {
    const formData = new FormData()
    params.files.forEach((file) => {
      formData.append('files', file)
    })
    return this.httpClient.post(`${url}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  }

  removeDefaultOptions(options: PagedAndSortedRequest) {
    const result: PagedAndSortedRequest = {} as PagedAndSortedRequest
    for (const prop of Object.keys(options)) {
      const value = options[prop as keyof PagedAndSortedRequest]
      if (Object.hasOwn(options, prop) && value !== null && value !== '') {
        if (this.isDefaultProperty(prop, value)) continue

        if (Array.isArray(prop)) continue
        result[prop] = value
      }
    }

    return result
  }

  isDefaultProperty(prop: string, _value: number) {
    if (
      ['pageCount', 'onUpdatePageSize', 'showSizePicker', 'pageSizes'].includes(
        prop,
      )
    )
      return true

    // if (prop === 'page' && value === 1) return true

    return false
  }
}
