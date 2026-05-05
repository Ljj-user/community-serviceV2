from __future__ import annotations

from pathlib import Path

from docx import Document


DOC_NAME = "天津商业大学毕业设计(论文)张正豪v4.1-5.1已修订.docx"


def resolve_doc_path() -> Path:
    thesis_dir = Path.cwd() / "毕业论文"
    exact = thesis_dir / DOC_NAME
    if exact.exists():
        return exact

    matches = sorted(thesis_dir.glob("*v4.1-5.1*已修订*.docx"))
    if not matches:
        raise FileNotFoundError("未找到目标论文文档。")
    return matches[0]


def copy_paragraph_format(src, dst) -> None:
    src_fmt = src.paragraph_format
    dst_fmt = dst.paragraph_format
    dst.style = src.style
    dst.alignment = src.alignment
    dst_fmt.left_indent = src_fmt.left_indent
    dst_fmt.right_indent = src_fmt.right_indent
    dst_fmt.first_line_indent = src_fmt.first_line_indent
    dst_fmt.keep_together = src_fmt.keep_together
    dst_fmt.keep_with_next = src_fmt.keep_with_next
    dst_fmt.page_break_before = src_fmt.page_break_before
    dst_fmt.widow_control = src_fmt.widow_control
    dst_fmt.space_before = src_fmt.space_before
    dst_fmt.space_after = src_fmt.space_after
    dst_fmt.line_spacing = src_fmt.line_spacing
    dst_fmt.line_spacing_rule = src_fmt.line_spacing_rule


def set_text(paragraph, text: str) -> None:
    if paragraph.text != text:
        paragraph.text = text


def main() -> None:
    doc_path = resolve_doc_path()
    doc = Document(str(doc_path))
    paragraphs = doc.paragraphs

    title_1_template = paragraphs[527]
    heading_2_template = paragraphs[529]
    heading_3_template = paragraphs[276]
    heading_4_template = paragraphs[277]
    body_template = paragraphs[275]
    caption_template = paragraphs[285]

    content_updates = {
        86: "在理论层面，本系统尝试将在共享经济中的“即时匹配”逻辑引入非营利性的社区治理中，探讨技术如何重塑邻里间的互助关系，为基层治理从“单向管控”向“多元共治”的转型提供参考。在实践意义层面，该系统不单单是一个技术平台，更是一套治理工具。对居民角色而言，它通过简化的交互设计，让养老助残服务触手可及；对管理者立场而言，它通过一套全链路的数据持久化留档，解决了传统公益服务中“质量难控、纠纷难断、成效难评”的痛点。更加有意义的是，通过服务评价、志愿服务记录和社区激励等机制，系统有望激活社区的内生动力，让社区互帮互助的理念在数字时代更易落地。",
        105: "志愿者智能匹配与认领：研究基于服务列表的“认领”模式。志愿者浏览需求池，系统根据认领动作自动关联志愿者档案与服务认领记录，状态流转至“已接单”或“服务中”。",
        106: "服务监管与评价闭环：开发异常干预功能，允许管理员对超时未完成的服务任务进行手动处理。同时，建立“完成确认 + 居民评价 + 服务时长记录”机制，志愿者完成服务后提交完成说明与服务时长，居民对服务质量进行评价，形成可追踪的服务数据沉淀。",
        178: "认证志愿者能力用例分析",
        179: "志愿者能力是在统一用户账号基础上经认证后获得的服务能力，其核心诉求是便捷地发现服务机会、高效地参与服务并获得认可。认证志愿者的核心功能包括：浏览需求列表、认领任务、提交服务完成、接受居民评价、查看服务记录等。认证志愿者能力用例图如图3所示。",
        240: "具体流程如下：居民在完成登录和社区绑定后提交需求，需求首先进入“待审核”状态；社区管理员审核通过后，需求进入“待接单”状态；通过认证的志愿者可在服务大厅查看并认领任务，任务随即进入“已接单”或“服务中”状态；志愿者完成服务后提交完成信息，需求进入“待确认”状态；居民确认并完成评价后，服务记录、认领记录与评价数据统一沉淀到数据库，并可进一步参与统计分析和异常预警判断。",
        271: "综上所述，当前数据库结构已经能够稳定支撑论文所述主流程，并为后续社区治理、统计分析和功能扩展预留了清晰的数据支撑基础。",
    }

    for idx, text in content_updates.items():
        set_text(paragraphs[idx], text)
        copy_paragraph_format(body_template, paragraphs[idx])

    set_text(paragraphs[182], "图 3 认证志愿者能力用例图")
    copy_paragraph_format(caption_template, paragraphs[182])

    format_as_normal = [322, 332, 345, 350, 362, 394, 420, 450, 451, 471, 490, 516, 544, 571, 586]
    format_as_heading_3 = [435]
    format_as_heading_4 = [329]
    format_as_title_1 = [540, 572, 580]

    for idx in format_as_normal:
        if idx < len(paragraphs):
            copy_paragraph_format(body_template, paragraphs[idx])

    for idx in format_as_heading_3:
        if idx < len(paragraphs):
            copy_paragraph_format(heading_3_template, paragraphs[idx])

    for idx in format_as_heading_4:
        if idx < len(paragraphs):
            copy_paragraph_format(heading_4_template, paragraphs[idx])

    for idx in format_as_title_1:
        if idx < len(paragraphs):
            copy_paragraph_format(title_1_template, paragraphs[idx])

    if 540 < len(paragraphs):
        set_text(paragraphs[540], "参考文献")
    if 572 < len(paragraphs):
        set_text(paragraphs[572], "致谢")
    if 580 < len(paragraphs):
        set_text(paragraphs[580], "附    录")

    doc.save(str(doc_path))
    print(doc_path)


if __name__ == "__main__":
    main()
