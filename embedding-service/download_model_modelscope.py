# ModelScope 镜像下载脚本（国内访问更稳定）
# 先安装 modelscope: pip install modelscope

from modelscope import snapshot_download
import os

model_id = "BAAI/bge-m3"
local_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "bge-m3")
os.makedirs(local_dir, exist_ok=True)

print(f"=== 开始从 ModelScope 下载模型 {model_id} 到 {local_dir}")
snapshot_download(model_id=model_id, cache_dir=local_dir)
print(f"=== 模型下载完成: {local_dir}")
