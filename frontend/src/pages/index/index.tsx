import { Button, Text, Textarea, View } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useMemo, useState } from 'react'
import { AnswerResult, Question, Report, createSession, generateQuestions, generateReport, submitAnswer } from '../../services/api'
import './index.scss'

type Step = 'home' | 'loading' | 'quiz' | 'feedback' | 'report' | 'share'

export default function Index () {
  const [step, setStep] = useState<Step>('home')
  const [content, setContent] = useState('光合作用是绿色植物利用光能，将二氧化碳和水转化为有机物，并释放氧气的过程。')
  const [sessionId, setSessionId] = useState<number>()
  const [questions, setQuestions] = useState<Question[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [selected, setSelected] = useState('')
  const [answerResult, setAnswerResult] = useState<AnswerResult>()
  const [report, setReport] = useState<Report>()
  const [loadingText, setLoadingText] = useState('')

  const currentQuestion = questions[currentIndex]
  const answeredCount = answerResult?.progress.answeredCount || currentIndex
  const accuracyText = report ? `${Number(report.accuracy).toFixed(1)}%` : '0%'
  const topic = useMemo(() => {
    const value = content.trim().replace(/\s+/g, ' ')
    return value.length > 12 ? value.slice(0, 12) : value || '学习主题'
  }, [content])

  async function start() {
    if (!content.trim()) {
      Taro.showToast({ title: '请输入学习内容', icon: 'none' })
      return
    }

    try {
      setLoadingText('正在创建会话')
      setStep('loading')
      const session = await createSession(content)
      setSessionId(session.sessionId)
      setLoadingText('正在生成题目')
      const questionRes = await generateQuestions(session.sessionId)
      setQuestions(questionRes.questions)
      setCurrentIndex(0)
      setSelected('')
      setAnswerResult(undefined)
      setStep('quiz')
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '生成失败', icon: 'none' })
      setStep('home')
    }
  }

  async function submit() {
    if (!sessionId || !currentQuestion || !selected) {
      Taro.showToast({ title: '请选择答案', icon: 'none' })
      return
    }

    try {
      const result = await submitAnswer(sessionId, currentQuestion.id, selected)
      setAnswerResult(result)
      setStep('feedback')
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
    }
  }

  async function next() {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex(currentIndex + 1)
      setSelected('')
      setAnswerResult(undefined)
      setStep('quiz')
      return
    }

    if (!sessionId) return
    try {
      setLoadingText('正在生成复盘报告')
      setStep('loading')
      const nextReport = await generateReport(sessionId)
      setReport(nextReport)
      setStep('report')
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '报告生成失败', icon: 'none' })
      setStep('feedback')
    }
  }

  return (
    <View className='page'>
      <View className='phone'>
        <View className='phone-inner'>
          <View className='status'><Text>9:41</Text><Text>5G ▰</Text></View>
          {step === 'home' && (
            <>
              <View className='topbar'>
                <View className='brand'><View className='brand-mark'>AI</View><Text>闯关学 AI</Text></View>
                <Button className='icon-btn'>☰</Button>
              </View>
              <View className='hero'>
                <Text className='pill reward'>今日可生成 3 次</Text>
                <Text className='h2'>把任意知识变成闯关题</Text>
                <Text className='muted'>粘贴笔记、网页或上传文件，AI 会生成 5 到 10 道关卡题。</Text>
              </View>
              <View className='tabs'>
                <Button className='tab active'>文本</Button>
                <Button className='tab disabled'>链接</Button>
                <Button className='tab disabled'>文件</Button>
                <Button className='tab disabled'>视频</Button>
              </View>
              <View className='input-box'>
                <Textarea value={content} maxlength={2000} onInput={(event) => setContent(event.detail.value)} />
                <View className='helper-row'>
                  <Text className='muted'>已输入 {content.length} 字</Text>
                  <Text className='pill'>单选 + 判断</Text>
                </View>
              </View>
              <View className='mini-cards'>
                <View className='mini-card'><Text className='muted'>题量</Text><Text className='b'>5-10 题</Text></View>
                <View className='mini-card'><Text className='muted'>难度</Text><Text className='b'>适中</Text></View>
                <View className='mini-card'><Text className='muted'>模式</Text><Text className='b'>闯关</Text></View>
              </View>
              <View className='bottom'><Button className='primary-btn' onClick={start}>开始生成闯关</Button></View>
            </>
          )}

          {step === 'loading' && (
            <>
              <View className='topbar'>
                <Button className='icon-btn' onClick={() => setStep('home')}>‹</Button>
                <Text className='pill'>{sessionId ? `会话 #${sessionId}` : '生成中'}</Text>
                <Button className='icon-btn' onClick={() => setStep('home')}>×</Button>
              </View>
              <View className='loader-wrap'><View className='loader' /><Text className='loader-core'>72%</Text></View>
              <View className='question-card'>
                <Text className='h2'>{loadingText || '正在生成你的闯关题'}</Text>
                <Text className='muted'>AI 正在提取知识点并校验题目质量，完成后自动进入下一步。</Text>
                <View className='progress'><View className='progress-fill' style='width:72%' /></View>
              </View>
              <View className='steps'>
                <View className='step done'><Text className='dot' />解析输入内容</View>
                <View className='step done'><Text className='dot' />提取核心知识点</View>
                <View className='step active'><Text className='dot' />生成选择题与判断题</View>
                <View className='step'><Text className='dot' />校验答案和解析</View>
              </View>
            </>
          )}

          {step === 'quiz' && currentQuestion && (
            <>
              <View className='topbar'>
                <Button className='icon-btn' onClick={() => setStep('home')}>‹</Button>
                <Text className='pill reward'>第 {currentIndex + 1} / {questions.length} 关</Text>
                <Button className='icon-btn'>⋮</Button>
              </View>
              <View className='stats-row'>
                <Text className='muted'>{topic}</Text>
                <Text className='pill good'>已答 {answeredCount} 题</Text>
              </View>
              <View className='progress progress-space'><View className='progress-fill' style={`width:${((currentIndex + 1) / questions.length) * 100}%`} /></View>
              <View className='question-card'>
                <View className='choice-head'>
                  <Text className='pill'>{currentQuestion.type === 'true_false' ? '判断题' : '单选题'}</Text>
                  <Text className='muted'>{currentQuestion.difficulty}</Text>
                </View>
                <Text className='h3'>{currentQuestion.stem}</Text>
                <View className='choice-list'>
                  {currentQuestion.options.map((option, index) => (
                    <Button key={option} className={`choice ${selected === option ? 'selected' : ''}`} onClick={() => setSelected(option)}>
                      <Text className='choice-key'>{String.fromCharCode(65 + index)}</Text>
                      <Text>{option}</Text>
                    </Button>
                  ))}
                </View>
              </View>
              <View className='bottom'><Button className='primary-btn' onClick={submit}>提交答案</Button></View>
            </>
          )}

          {step === 'feedback' && currentQuestion && answerResult && (
            <>
              <View className='topbar'>
                <Button className='icon-btn' onClick={() => setStep('quiz')}>‹</Button>
                <Text className='pill reward'>第 {currentIndex + 1} / {questions.length} 关</Text>
                <Button className='icon-btn'>☆</Button>
              </View>
              <View className='question-card'>
                <View className='choice-head'>
                  <Text className={`pill ${answerResult.correct ? 'good' : 'bad'}`}>{answerResult.correct ? '回答正确' : '回答错误'}</Text>
                  <Text className='muted'>{answerResult.correct ? '+12 XP' : '继续加油'}</Text>
                </View>
                <Text className='h3'>{currentQuestion.stem}</Text>
                <View className='choice-list'>
                  <View className={`choice ${answerResult.correct ? 'correct' : 'wrong'}`}>
                    <Text className='choice-key'>{answerResult.correctAnswer}</Text>
                  </View>
                </View>
                <View className={`explain ${answerResult.correct ? 'success' : ''}`}>
                  <Text className='h3'>解析</Text>
                  <Text className='muted'>{answerResult.explanation}</Text>
                </View>
                <View className='helper-row'>
                  <Text className='pill'>知识点：{currentQuestion.knowledgePoint}</Text>
                  <Text className='pill reward'>掌握 +1</Text>
                </View>
              </View>
              <View className='list'>
                <View className='list-item'><Text className='b'>易错点</Text><Text className='muted'>答错时重点回看题干中的限定词和知识点。</Text></View>
              </View>
              <View className='bottom'><Button className='primary-btn' onClick={next}>{currentIndex < questions.length - 1 ? '进入下一关' : '生成复盘报告'}</Button></View>
            </>
          )}

          {step === 'report' && report && (
            <>
              <View className='topbar'>
                <View className='brand'><View className='brand-mark'>↟</View><Text>学习复盘</Text></View>
                <Button className='icon-btn' onClick={() => setStep('share')}>↗</Button>
              </View>
              <View className='score-card'>
                <Text className='pill reward'>{topic}</Text>
                <View className='report-row'>
                  <View><Text className='score'>{report.score}<Text className='score-small'>分</Text></Text><Text>你已完成本次闯关</Text></View>
                  <Text className='trophy'>♕</Text>
                </View>
              </View>
              <View className='report-grid'>
                <View className='metric'><Text className='muted'>正确率</Text><Text className='metric-value'>{accuracyText}</Text></View>
                <View className='metric'><Text className='muted'>薄弱点</Text><Text className='metric-value'>{report.weakPoints.length} 个</Text></View>
                <View className='metric'><Text className='muted'>错题</Text><Text className='metric-value'>{report.wrongQuestions.length} 题</Text></View>
                <View className='metric'><Text className='muted'>题量</Text><Text className='metric-value'>{questions.length} 题</Text></View>
              </View>
              <View className='list'>
                <View className='list-item'><Text className='b'>本次摘要</Text><Text className='muted'>{report.summary}</Text></View>
                <View className='list-item'><Text className='b'>下一步建议</Text><Text className='muted'>{report.suggestions.join('；')}</Text></View>
              </View>
              <View className='bottom'><Button className='primary-btn' onClick={() => setStep('share')}>分享学习结果</Button></View>
            </>
          )}

          {step === 'share' && report && (
            <>
              <View className='topbar'>
                <Button className='icon-btn' onClick={() => setStep('report')}>‹</Button>
                <Text className='pill'>分享预览</Text>
                <Button className='icon-btn'>□</Button>
              </View>
              <View className='poster'>
                <View><Text className='pill reward'>AI 闯关完成</Text><Text className='h2 poster-title'>我刚学完{topic}</Text></View>
                <View className='poster-card'>
                  <Text>本次得分</Text>
                  <Text className='score'>{report.score}<Text className='score-small'>分</Text></Text>
                  <View className='share-line'><Text>正确率</Text><Text>{accuracyText}</Text></View>
                  <View className='share-line'><Text>连续闯关</Text><Text>{questions.length} 关</Text></View>
                </View>
                <View className='poster-card'><Text className='b'>学习总结</Text><Text>{report.summary}</Text></View>
                <View className='share-line'>
                  <View><Text className='b'>闯关学 AI</Text><Text className='qr-text'>扫码生成你的专属题目</Text></View>
                  <View className='qr' />
                </View>
              </View>
              <View className='bottom'><Button className='primary-btn' openType='share'>发送给好友</Button></View>
            </>
          )}
        </View>
      </View>
    </View>
  )
}
