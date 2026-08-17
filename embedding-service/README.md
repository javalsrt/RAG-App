# 本地 BGE-M3 Embedding 服务

本项目用于替代 DashScope 在线文本向量化服务，部署在本地，为后端 AI 问答（RAG）和课程内容向量化提供 Embedding 能力。

## 功能

- 基于 `BAAI/bge-m3` 模型
- 提供 OpenAI 兼容的 `/embeddings` 接口
- 支持单条和批量文本向量化
- 支持 CPU / CUDA 运行

## 环境要求

- Python 3.9+
- 至少 4GB 内存（CPU 运行 BGE-M3 约需 2GB）
- 如需 GPU 加速，需安装 CUDA 和对应 PyTorch

## 安装

在项目根目录的 `embedding-service` 文件夹下执行：

```bash
pip install -r requirements.txt
```

## 下载模型

服务默认优先使用项目内 `embedding-service/models/bge-m3/` 目录下的模型；如果不存在，会自动从 HuggingFace 下载（约 2.2GB）。

### 方式一：自动下载（首次启动时）

直接启动服务，会自动检测并下载模型。

### 方式二：手动下载到项目内（推荐）

#### HuggingFace 渠道

```bash
python download_model.py
```

#### ModelScope 镜像（国内更稳定）

```bash
pip install modelscope
python download_model_modelscope.py
```

下载完成后，模型会存放在 `embedding-service/models/bge-m3/`。

## 启动服务

### Windows

```bash
start.bat
```

### Linux / macOS

```bash
chmod +x start.sh
./start.sh
```

服务默认运行在 `http://localhost:8000`。

## 验证服务

```bash
curl -X POST http://localhost:8000/embeddings \
  -H "Content-Type: application/json" \
  -d '{"input": "人工智能"}'
```

## 接入后端

修改 [backend/src/main/resources/application.yml](file:///c:/Users/jay/Desktop/myBishe/aiStudy/backend/src/main/resources/application.yml) 中的 `dashscope.embedding-url`：

```yaml
dashscope:
  embedding-url: http://localhost:8000/embeddings
```

或者通过环境变量启动后端：

```bash
set DASHSCOPE_EMBEDDING_URL=http://localhost:8000/embeddings
```

然后重启后端即可使用本地 Embedding。

## 常见问题

1. **模型下载慢**：可提前从 [BGE-M3 HuggingFace 仓库](https://huggingface.co/BAAI/bge-m3) 下载，放到本地目录后设置 `EMBEDDING_MODEL=本地路径`。
2. **显存不足**：默认使用 CPU 运行；如启用 CUDA 后显存不足，可改回 CPU。
3. **后端报错连接失败**：请确保先启动本服务，再启动后端。
