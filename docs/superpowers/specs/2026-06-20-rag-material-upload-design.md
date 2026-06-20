# RAG 资料上传设计

## 目标

新增独立资料上传页面，支持用户导入学习资料生成题目。资料进入后端后完成文本解析、切分、向量化并写入 PostgreSQL pgvector。出题时由 AI 自动判断优先使用 RAG 资料，资料不足时再联网搜索，证据不足时拒绝生成题目。

## 页面

新增 Taro 页面 `pages/material/index`。

页面结构参考用户提供图片：

- 顶部：返回按钮、标题“输入来源”、帮助按钮。
- 说明区：标题“导入资料生成题目”，说明首版支持文本和文件，网页与视频仅展示禁用入口。
- 来源卡片：文本、网页、文件、视频。
- 上传区：支持 PDF、DOCX、TXT，单个文件不超过 10MB。
- 底部按钮：解析并生成。

首版行为：

- 文本：输入资料文本。
- 文件：上传 PDF、DOCX、TXT。
- 网页：禁用入口，不进入解析流程。
- 视频：禁用入口，不进入解析流程。

## 接口

新增资料接口：

```http
POST /api/materials/text
POST /api/materials/files
POST /api/materials/{materialId}/questions
```

`POST /api/materials/text` 接收资料文本和出题要求。

`POST /api/materials/files` 接收文件和出题要求，后端解析文件文本。

`POST /api/materials/{materialId}/questions` 触发 RAG 检索、AI 判断和题目生成。

原学习会话接口保留，用于普通文本主题直接出题。

## 存储

MySQL 保存业务数据：

- 资料元数据
- 学习会话
- 题目
- 答题记录
- 学习报告

PostgreSQL pgvector 保存资料分片向量：

- `material_id`
- `chunk_index`
- `content`
- `metadata`
- `embedding`

pgvector 需要启用：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

向量表使用 HNSW + cosine distance。

## 后端模块

新增模块：

- `MaterialController`：资料上传接口。
- `MaterialService`：资料创建、文件解析、切分入库。
- `DocumentParser`：PDF、DOCX、TXT 文本提取。
- `RagService`：pgvector 检索。
- `QuestionContextRouter`：构造 RAG 与联网搜索上下文。

调整模块：

- `AiClient.generateQuestions` 增加上下文入参。
- `DashScopeAiClient` 使用 Spring AI Alibaba / Spring AI 的 RAG 能力和 tool calling。
- `Question` 增加 `sourceType`。

## 出题链路

```text
用户上传资料
  |
  v
解析文本
  |
  v
切分文本
  |
  v
Embedding
  |
  v
写入 PostgreSQL pgvector
  |
  v
用户点击解析并生成
  |
  v
pgvector 相似度检索
  |
  v
AI 判断 RAG 证据是否足够
  |
  |-- 足够：基于 RAG 出题
  |-- 不足：调用 webSearch
  |-- 仍不足：返回 AI_GENERATION_FAILED
```

## 题目来源

题目新增字段：

- `sourceType`: `rag` 或 `web`
- `sourceUrl`
- `evidence`
- `confidence`

RAG 题：

- `sourceType = rag`
- `sourceUrl = rag://material/{materialId}`
- `evidence` 必须来自资料分片原文

联网题：

- `sourceType = web`
- `sourceUrl` 为网页链接
- `evidence` 必须来自搜索结果

## 校验

- 资料为空拒绝上传。
- 文件超过 10MB 拒绝上传。
- 非 PDF、DOCX、TXT 拒绝上传。
- 文本解析为空拒绝入库。
- 题目数量 5 到 10 道。
- 每题必须有题干、选项、正确答案、解析、来源、证据和置信度。
- `confidence` 必须在 `0.6` 到 `1`。
- RAG 题证据必须能在资料文本中匹配到。
- 联网题必须有有效 URL。
- 校验失败自动重试一次。

## 验收

- 用户能从首页进入资料上传页面。
- 用户能上传 TXT/PDF/DOCX 或输入文本资料。
- 后端能解析资料并写入 pgvector。
- 生成题目时能优先检索资料。
- AI 能在 RAG 和联网搜索之间自动判断。
- 题目解析页能展示“题库”或“联网”来源。
- RAG 证据不足时不会编造题目。
