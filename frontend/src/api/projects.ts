import axios from 'axios'

export function fetchProjectList(params?: any) {
  return axios.get('/api/projects', { params })
}
