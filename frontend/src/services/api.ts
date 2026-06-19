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

export async function createSession(content: string) {
  return request<{ sessionId: number; status: string }>('/api/learning/sessions', 'POST', {
    inputType: 'text',
    content
  })
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
