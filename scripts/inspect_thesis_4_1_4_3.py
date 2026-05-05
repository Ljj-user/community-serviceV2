from pathlib import Path
from docx import Document


DOC_CANDIDATES = [
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1-5.1已修订.docx",
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx",
]


def main() -> None:
    doc_path = next((p for p in DOC_CANDIDATES if p.exists()), None)
    if doc_path is None:
        raise FileNotFoundError("未找到论文文件")

    doc = Document(str(doc_path))
    print(f"FILE: {doc_path}")
    for idx in range(180, 275):
        p = doc.paragraphs[idx]
        style = getattr(p.style, "name", "")
        print(f"{idx:03d} [{style}] {p.text}")


if __name__ == "__main__":
    main()
