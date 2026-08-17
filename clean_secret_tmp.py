import io, os, re
p = "backend/src/main/resources/application.yml"
if os.path.exists(p):
    s = open(p, encoding="utf-8").read()
    # 把任何形式的 DEEPSEEK_API_KEY 兜底密钥替换为纯环境变量引用(无默认值)
    new = re.sub(r"api-key:\s*\$\{DEEPSEEK_API_KEY:[^}]*\}", "api-key: ${DEEPSEEK_API_KEY}", s)
    if new != s:
        open(p, "w", encoding="utf-8").write(new)
        print("CLEANED:", p)
    else:
        print("NO-CHANGE:", p)
else:
    print("ABSENT:", p)