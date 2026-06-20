import Taro from '@tarojs/taro'

const BASE_URL = process.env.TARO_APP_API_BASE || 'http://localhost:8080'

type ApiResponse<T> = {
  code: string
  message: string
  data: T
}

async function request<T>(url: string, method: 'GET' | 'POST', data?: unknown): Promise<T> {
  const res = await Taro.request<ApiResponse<T>>({
    url: `${BASE_URL}${url}`,
    method,
    data,
    header: {
      'content-type': 'application/json'
    }
  })

  if (res.statusCode < 200 || res.statusCode >= 300 || res.data.code !== 'OK') {
    throw new Error(res.data?.message || '请求失败')
  }

  return res.data.data
}

export type Question = {
  id: number
  type: string
  stem: string
  options: string[]
  correctAnswer: string
  explanation: string
  knowledgePoint: string
  difficulty: string
  sourceUrl: string
  evidence: string
  confidence: number
  sortOrder: number
}

export type AnswerResult = {
  correct: boolean
  correctAnswer: string
  explanation: string
  progress: {
    answeredCount: number
    questionCount: number
    correctCount: number
  }
}

export type Report = {
  reportId: number
  summary: string
  mastery: string
  score: number
  accuracy: number
  weakPoints: string[]
  suggestions: string[]
  wrongQuestions: Array<{
    questionId: number
    stem: string
    userAnswer: string
    correctAnswer: string
    explanation: string
    knowledgePoint: string
  }>
}

export type MaterialResult = {
  materialId: number
  title: string
  type: string
  status: string
  chunkCount: number
}

export async function createSession(content: string) {
  return request<{ sessionId: number; status: string }>('/api/learning/sessions', 'POST', {
    inputType: 'text',
    content
  })
}

export async function createTextMaterial(title: string, content: string) {
  return request<MaterialResult>('/api/materials/text', 'POST', {
    title,
    content
  })
}

export async function uploadMaterialFile(filePath: string | File, title = '') {
  if (process.env.TARO_ENV === 'h5' && filePath instanceof File) {
    const formData = new FormData()
    formData.append('file', filePath)
    formData.append('title', title)
    const res = await fetch(`${BASE_URL}/api/materials/files`, {
      method: 'POST',
      body: formData
    })
    const data = await res.json() as ApiResponse<MaterialResult>
    if (!res.ok || data.code !== 'OK') {
      throw new Error(data?.message || '涓婁紶澶辫触')
    }
    return data.data
  }

  const res = await Taro.uploadFile({
    url: `${BASE_URL}/api/materials/files`,
    filePath: filePath as string,
    name: 'file',
    formData: { title }
  })

  const data = typeof res.data === 'string' ? JSON.parse(res.data) as ApiResponse<MaterialResult> : res.data
  if (res.statusCode < 200 || res.statusCode >= 300 || data.code !== 'OK') {
    throw new Error(data?.message || '上传失败')
  }
  return data.data
}

export async function generateQuestions(sessionId: number) {
  return request<{ sessionId: number; questions: Question[] }>(`/api/learning/sessions/${sessionId}/questions`, 'POST')
}

export async function submitAnswer(sessionId: number, questionId: number, userAnswer: string) {
  return request<AnswerResult>(`/api/learning/sessions/${sessionId}/answers`, 'POST', {
    questionId,
    userAnswer
  })
}

export async function generateReport(sessionId: number) {
  return request<Report>(`/api/learning/sessions/${sessionId}/report`, 'POST')
}
