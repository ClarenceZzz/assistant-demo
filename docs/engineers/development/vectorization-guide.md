# 向量化流程操作指引（供后端 / AI 平台工程师使用）

## 目标
将原始文档拆分成结构化分块，为每个分块生成嵌入向量，并连同元数据写入向量库（Postgres + pgvector），供 RAG 检索使用。

## 1. 拉取原始文档
- 支持格式：Markdown、HTML、PDF、CSV/JSON 工单导出等。
- 将文档放入 `data/raw/`，命名时包含 `document_id`（例：`manual_ac_2024.pdf`）。

## 2. 文本清洗
- 工具建议：
  - PDF：`pdfplumber`、`pdfminer.six`
  - HTML：`BeautifulSoup` 去除脚本/样式
  - Markdown：直接读取或使用 `markdown` 库转换
- 统一处理：转换为 UTF-8，去除噪声（页眉/页脚、版权信息），保留结构标记（标题使用 `##`、列表使用 `- ` 等）。
- 输出到 `data/clean/manual_ac_2024.txt`。

## 3. 分块（Chunking）
- 原则：
  - 每段 300–500 字左右。
  - 前后块重叠 80–100 字，以保留语义上下文。
  - 先按文档结构（标题、段落）切分，再按长度细分。
- 输出 JSONL：`data/chunks/manual_ac_2024.jsonl`，每行包含：
  ```json
  {
    "document_id": "manual_ac_2024",
    "chunk_id": "manual_ac_2024-0001",
    "content": "……文本……",
    "metadata": {
      "title": "XX 型号空调快速指南",
      "section": "重置步骤",
      "page_number": 12,
      "last_modified_at": "2024-09-01",
      "source_type": "manual"
    }
  }
  ```

## 4. 调用 DashScope Embedding
- 示例（Python）：
  ```python
  import os
  from dashscope import TextEmbedding

  api_key = os.environ["DASHSCOPE_API_KEY"]

  def embed(text: str) -> list[float]:
      resp = TextEmbedding.call(model="text-embedding-v2", input=text, api_key=api_key)
      return resp["output"]["embeddings"][0]
  ```
- 批量处理 JSONL，每批 5–10 条，做好失败重试与速率控制。
- 将向量追加到每个 chunk 中：
  ```json
  {
    "chunk_id": "...",
    "content": "...",
    "embedding": [0.01, -0.03, ...],
    "metadata": {...}
  }
  ```

## 5. 写入 PgVector
- 建表参考：
  ```sql
  CREATE TABLE rag_chunks (
    chunk_id TEXT PRIMARY KEY,
    document_id TEXT,
    content TEXT,
    embedding VECTOR(1536),
    metadata JSONB,
    last_modified TIMESTAMP DEFAULT now()
  );
  CREATE INDEX idx_rag_chunks_metadata ON rag_chunks USING GIN (metadata jsonb_path_ops);
  CREATE INDEX idx_rag_chunks_document ON rag_chunks(document_id);
  ```
- 插入逻辑：
  ```python
  def upsert_chunks(conn, chunks):
      doc_id = chunks[0]["document_id"]
      with conn.cursor() as cur:
          cur.execute("DELETE FROM rag_chunks WHERE document_id = %s", (doc_id,))
          for c in chunks:
              cur.execute(
                  """
                  INSERT INTO rag_chunks (chunk_id, document_id, content, embedding, metadata)
                  VALUES (%s, %s, %s, %s, %s)
                  """,
                  (c["chunk_id"], c["document_id"], c["content"], c["embedding"], json.dumps(c["metadata"]))
              )
      conn.commit()
  ```
- 注意：同一 `document_id` 需先删除旧分块再插入新数据，以保证知识一致性。

## 6. 封装 CLI/脚本
- 建议提供命令行入口：
  ```bash
  python tools/ingest.py --input data/raw/manual_ac_2024.pdf --doc-id manual_ac_2024
  ```
- 主要流程：清洗 → 分块 → 嵌入 → 写库 → 输出日志（处理条数、失败列表）。

## 7. 抽检与监控
- 编写抽检脚本，随机输出部分 chunk 的内容、元数据、向量维度。
- 与 QA/PM 联合校验结果是否符合业务预期（语义完整、引用正确）。

## 8. 后续扩展建议
- 配置化参数：分块长度、重叠、模型名称，通过 `configs/ingest.yaml` 管理。
- 定时更新：可在 CI/CD 或调度平台上运行增量同步任务。
- 统计指标：记录本次导入文档数量、chunk 数、向量库占用，便于容量规划。
