import { Button, Text, Textarea, View } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useState } from 'react'
import { createTextMaterial, uploadMaterialFile } from '../../services/api'
import './index.scss'

type SourceType = 'text' | 'web' | 'file' | 'video'

export default function MaterialPage () {
  const [sourceType, setSourceType] = useState<SourceType>('text')
  const [title, setTitle] = useState('学习资料')
  const [content, setContent] = useState('')
  const [filePath, setFilePath] = useState<string | File>()
  const [fileName, setFileName] = useState('')
  const [saving, setSaving] = useState(false)

  async function chooseFile() {
    if (process.env.TARO_ENV === 'h5') {
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = '.pdf,.docx,.txt'
      input.onchange = () => {
        const file = input.files?.[0]
        if (!file) return
        setFilePath(file)
        setFileName(file.name)
        if (!title || title === '瀛︿範璧勬枡') {
          setTitle(file.name)
        }
      }
      input.click()
      return
    }

    const result = await Taro.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf', 'docx', 'txt']
    })
    const file = result.tempFiles[0]
    if (!file) return
    setFilePath(file.path)
    setFileName(file.name)
    if (!title || title === '学习资料') {
      setTitle(file.name)
    }
  }

  async function save() {
    try {
      setSaving(true)
      if (sourceType === 'text') {
        if (!content.trim()) {
          Taro.showToast({ title: '请输入资料内容', icon: 'none' })
          return
        }
        await createTextMaterial(title, content)
      } else if (sourceType === 'file') {
        if (!filePath) {
          Taro.showToast({ title: '请选择文件', icon: 'none' })
          return
        }
        await uploadMaterialFile(filePath, title)
      } else {
        Taro.showToast({ title: '当前入口暂未开放', icon: 'none' })
        return
      }
      Taro.showToast({ title: '资料已上传', icon: 'success' })
      setTimeout(() => Taro.navigateBack(), 600)
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '上传失败', icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <View className='material-page'>
      <View className='material-phone'>
        <View className='material-inner'>
          <View className='status'><Text>9:41</Text><Text>5G ▰</Text></View>
          <View className='material-topbar'>
            <Button className='icon-btn' onClick={() => Taro.navigateBack()}>‹</Button>
            <Text className='pill'>输入来源</Text>
            <Button className='icon-btn'>?</Button>
          </View>

          <View className='material-hero'>
            <Text className='material-title'>导入资料生成题目</Text>
            <Text className='material-muted'>资料上传后会作为出题参考，开始闯关时由 AI Agent 自动检索。</Text>
          </View>

          <View className='source-grid'>
            <Button className={`source-card ${sourceType === 'text' ? 'active' : ''}`} onClick={() => setSourceType('text')}>
              <Text className='source-pill'>文本</Text>
              <Text className='source-note'>适合笔记和短文</Text>
            </Button>
            <Button className='source-card disabled' onClick={() => setSourceType('web')}>
              <Text className='source-pill'>网页</Text>
              <Text className='source-note'>粘贴文章链接</Text>
            </Button>
            <Button className={`source-card ${sourceType === 'file' ? 'active' : ''}`} onClick={() => setSourceType('file')}>
              <Text className='source-pill'>文件</Text>
              <Text className='source-note'>PDF / DOCX / TXT</Text>
            </Button>
            <Button className='source-card disabled' onClick={() => setSourceType('video')}>
              <Text className='source-pill warn'>视频</Text>
              <Text className='source-note'>后续开放</Text>
            </Button>
          </View>

          <View className='material-input'>
            <Textarea value={title} maxlength={60} onInput={(event) => setTitle(event.detail.value)} />
          </View>

          {sourceType === 'text' && (
            <View className='material-input large'>
              <Textarea value={content} maxlength={12000} placeholder='粘贴资料内容' onInput={(event) => setContent(event.detail.value)} />
              <Text className='material-muted'>已输入 {content.length} 字</Text>
            </View>
          )}

          {sourceType === 'file' && (
            <View className='upload-zone' onClick={chooseFile}>
              <Text className='upload-icon'>↥</Text>
              <Text className='upload-title'>{fileName || '点击上传学习资料'}</Text>
              <Text className='material-muted'>单个文件不超过 10MB</Text>
            </View>
          )}

          <View className='material-bottom'>
            <Button className='material-primary' loading={saving} onClick={save}>上传资料</Button>
          </View>
        </View>
      </View>
    </View>
  )
}
