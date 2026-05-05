from pathlib import Path
from docx import Document


DOC = Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx"
TERMS = [
    "SpringBoot",
    "Spring Boot",
    "Vue3",
    "Vue 3",
    "Mybatis-plus",
    "MyBatis-Plus",
    "社区公益服务对接管理平台",
    "社区公益服务平台",
    "社区公益志愿服务平台",
]


def main() -> None:
    doc = Document(str(DOC))
    for idx, p in enumerate(doc.paragraphs):
        text = p.text
        if not text.strip():
            continue
        hits = [term for term in TERMS if term in text]
        if hits:
            print(f"{idx:03d} {hits} :: {text}")


if __name__ == "__main__":
    main()
