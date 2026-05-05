from pathlib import Path

from docx import Document


DOC_PATH = Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1-内容重做版.docx"


def main() -> None:
    doc = Document(str(DOC_PATH))
    updates = {
        397: "图 20 志愿者认证实现图",
        417: "",
        435: "图 22 AI 助手页实现图",
        451: "",
        519: "图 29 用户管理与审计日志页实现图",
    }
    for idx, text in updates.items():
        doc.paragraphs[idx].text = text
    doc.save(str(DOC_PATH))
    print(DOC_PATH)


if __name__ == "__main__":
    main()
