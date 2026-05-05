from pathlib import Path
from docx import Document


DOC = Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx"
TERMS = [
    "时间银行",
    "二手交易",
    "失物招领",
    "社区生活贴",
    "消息页",
    "聊天功能",
    "校园",
    "地点热度",
    "小程序",
    "论坛",
]


def main() -> None:
    doc = Document(str(DOC))
    for idx, p in enumerate(doc.paragraphs):
        text = p.text
        for term in TERMS:
            if term in text:
                print(f"{idx:03d} [{getattr(p.style, 'name', '')}] {term}: {text}")
                break


if __name__ == "__main__":
    main()
