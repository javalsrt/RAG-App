@echo off
chcp 65001 >nul
set EMBEDDING_PORT=8000

:: 如需使用 NVIDIA 显卡，取消下面注释
:: set EMBEDDING_DEVICE=cuda

echo === 启动本地 BGE-M3 Embedding 服务（端口 %EMBEDDING_PORT%）===
python -m uvicorn main:app --host 0.0.0.0 --port %EMBEDDING_PORT%
