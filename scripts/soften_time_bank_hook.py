from pathlib import Path
from docx import Document


DOC_PATHS = [
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx",
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1-5.1已修订.docx",
]


UPDATED_TEXT = (
    "（3）运营激励方面：后续可在保持社区公益属性不变的前提下，进一步完善志愿服务激励、"
    "活跃度评价和长期服务效果分析机制；同时可适度借鉴时间银行的理念，将服务时长、服务质量"
    "与持续参与度纳入更完整的激励评价体系，为平台后续探索更可持续的公益互助机制提供研究基础。"
)


def main() -> None:
    updated = []
    skipped = []
    for path in DOC_PATHS:
        if not path.exists():
            continue
        try:
            doc = Document(str(path))
            doc.paragraphs[554].text = UPDATED_TEXT
            doc.save(str(path))
            updated.append(str(path))
        except PermissionError:
            skipped.append(str(path))

    print("UPDATED:")
    for item in updated:
        print(item)
    print("SKIPPED:")
    for item in skipped:
        print(item)


if __name__ == "__main__":
    main()
