from pathlib import Path
from docx import Document


DOC_CANDIDATES = [
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx",
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1-5.1已修订.docx",
]


def main() -> None:
    for path in DOC_CANDIDATES:
        if not path.exists():
            continue
        doc = Document(str(path))
        print(f"FILE: {path}")
        for idx in range(543, 550):
            p = doc.paragraphs[idx]
            print(f"{idx} [{getattr(p.style, 'name', '')}] {p.text}")


if __name__ == "__main__":
    main()
