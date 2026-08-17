import os

# 必须在导入 huggingface_hub 之前设置镜像站，否则不会生效
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"

from huggingface_hub import snapshot_download

model_name = "BAAI/bge-m3"
local_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "bge-m3")
os.makedirs(local_dir, exist_ok=True)

print(f"=== 开始下载模型 {model_name} 到 {local_dir} (镜像: {os.environ['HF_ENDPOINT']})")
snapshot_download(repo_id=model_name, local_dir=local_dir)
print(f"=== 模型下载完成: {local_dir}")
