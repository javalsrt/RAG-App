from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Union, List
import numpy as np
import uvicorn
import os

app = FastAPI(title="Local BGE-M3 Embedding Service", version="1.0.0")

# 设备选择：auto 自动检测，优先 CUDA，否则 CPU；也可显式设置为 cpu/cuda
raw_device = os.environ.get("EMBEDDING_DEVICE", "auto")
if raw_device == "auto":
    try:
        import torch
        DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
    except Exception:
        DEVICE = "cpu"
else:
    DEVICE = raw_device

# 模型路径优先级：环境变量 > 项目内 models/bge-m3 > HuggingFace 在线下载
LOCAL_MODEL_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "bge-m3")
MODEL_NAME = os.environ.get("EMBEDDING_MODEL")
if MODEL_NAME is None:
    MODEL_NAME = LOCAL_MODEL_PATH if os.path.exists(LOCAL_MODEL_PATH) else "BAAI/bge-m3"

print(f"=== 正在加载本地 Embedding 模型: {MODEL_NAME} (device={DEVICE}) ...")
try:
    from sentence_transformers import SentenceTransformer
    model = SentenceTransformer(MODEL_NAME, device=DEVICE)
    print("=== 模型加载完成")
except Exception as e:
    print(f"=== 模型加载失败: {e}")
    raise


class EmbeddingRequest(BaseModel):
    input: Union[str, List[str]]
    model: str = "bge-m3"
    encoding_format: str = "float"


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME}


@app.post("/embeddings")
def embeddings(req: EmbeddingRequest):
    try:
        texts = req.input if isinstance(req.input, list) else [req.input]
        if not texts:
            raise HTTPException(status_code=400, detail="input 不能为空")

        # BGE-M3 官方建议对检索任务前缀加 "representation: "，但通用 embedding 可不加
        embeddings = model.encode(texts, normalize_embeddings=True, convert_to_numpy=True)

        data = []
        for idx, vec in enumerate(embeddings):
            data.append({
                "object": "embedding",
                "embedding": vec.tolist(),
                "index": idx
            })

        return {
            "object": "list",
            "data": data,
            "model": req.model,
            "usage": {
                "prompt_tokens": sum(len(t) for t in texts),
                "total_tokens": sum(len(t) for t in texts)
            }
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"=== Embedding 推理异常: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    port = int(os.environ.get("EMBEDDING_PORT", "8000"))
    uvicorn.run(app, host="0.0.0.0", port=port)
