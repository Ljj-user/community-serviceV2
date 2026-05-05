from pathlib import Path
from docx import Document


DOC_PATHS = [
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx",
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1-5.1已修订.docx",
]


UPDATES = {
    65: "系统采用前后端分离架构，后端基于 Spring Boot 3 框架构建 RESTful API，结合 MyBatis-Plus 实现数据持久化，前端采用 Vue 3 框架开发移动端 H5 应用与管理后台。系统实现了基于 RBAC 模型的三级权限体系（超级管理员、社区管理员、普通用户），支持社区加入与绑定审核、需求标准化采集与审核、志愿者认证与认领、服务过程监控与评价反馈、重点关怀对象管理和异常预警分析等核心功能。此外，系统引入 AI 助手辅助需求发布，并通过审计日志、数据看板和系统配置模块增强了平台的可管可控性。",
    68: "关键词：社区公益服务；志愿者管理；Spring Boot；Vue 3；数字化治理；社区治理",
    71: "The system adopts a front-end and back-end separation architecture. The back-end is built on the Spring Boot 3 framework to construct RESTful APIs, combined with MyBatis-Plus for data persistence. The front-end uses the Vue 3 framework to develop mobile H5 applications and management backends. The system implements a three-level permission system based on the RBAC model (super administrator, community administrator, ordinary users), supporting core functions such as community joining and binding review, standardized demand collection and review, volunteer certification and claiming, service process monitoring and evaluation feedback, key care subject management, and anomaly alert analysis. In addition, the system introduces an AI assistant to assist demand publication and enhances platform manageability through audit logs, dashboards, and system configuration modules.",
    75: "Key Words: Community Public Welfare Service; Volunteer Management; Spring Boot; Vue 3; Digital Governance; Community Governance",
    116: "导言。主要介绍社区公益服务对接管理平台的设计与实现背景、研究意义、国内外研究现状，以及本文的研究内容与研究方法。",
    145: "本系统的总体结构依据居民用户、志愿者用户、社区管理员和系统管理员的实际业务需求展开规划，整体可划分为移动端用户模块、管理端治理模块、后端业务服务模块和平台支撑模块四个部分。社区公益服务对接管理平台系统总体规划结构功能如图1所示。",
    148: "社区公益服务平台的目标群体主要包括需要发布求助的社区居民、通过认证后参与服务的志愿者，以及负责审核和治理的社区管理员。平台围绕“居民发布需求—管理员审核—志愿者认领—服务完成—评价反馈”的主流程展开，同时补充社区绑定、重点关怀对象管理、异常预警监测、AI 辅助草稿和数据可视化分析等能力，形成较完整的社区公益服务闭环。",
    172: "通过用例图可以清晰展示不同角色与系统功能之间的关系，帮助开发者从业务参与者视角理解平台的功能边界与操作路径。结合当前系统设计，本文分别对居民用户、志愿者用户、社区管理员和系统管理员进行用例分析，以支撑后续模块设计与实现。",
    173: "居民用户是社区公益服务的主要需求发起方，也是移动端的基础用户。系统采用统一账户设计，用户完成注册登录与社区绑定后，可发布求助、查看处理进度、确认服务完成、提交评价反馈、浏览社区公告、查询便民信息，并在需要时借助 AI 助手整理需求内容。居民用户的用例图如图2所示。",
    179: "志愿者用户是社区公益服务的供给方，由居民用户在通过志愿者认证后扩展获得服务能力。其核心功能包括：提交认证申请、维护技能标签与可服务时间、浏览待接单需求、认领公益任务、反馈服务完成情况、查看历史服务记录与评价结果。志愿者用户的用例图如图3所示。",
    185: "社区管理员主要承担本社区范围内的审核与治理职责，其核心功能包括：审核社区加入申请、审核公益需求、审核志愿者认证、维护重点关怀对象、处理异常预警、发布社区公告、维护便民信息、查看数据看板以及管理辖区用户。社区管理员的用例图如图4所示。",
    191: "系统管理员是平台级运营与治理角色，除具备社区管理员的全部业务能力外，还负责跨社区统计分析、系统配置、审计日志查看、数据导出与备份等全局管理功能。系统管理员的用例如图5所示。",
    551: "社区公益服务对接管理平台的基本功能已完成，能够支撑社区辖内公益需求发布、审核、认领、评价和后台治理等核心场景。但从长期应用角度看，平台仍可结合真实社区运行反馈继续优化，后续可重点从以下几个方向展开：",
    552: "（1）业务协同方面：进一步完善社区加入审核、需求审核、志愿者审核与异常预警之间的联动机制，增强服务超时提醒、任务催办、重点对象主动关怀等治理能力；同时在 AI 对话基础上继续优化需求整理、任务分类和辅助分析能力，提高平台处理效率。",
    553: "（2）社区真实接入方面：后续可引入更精细的地理位置与社区边界校验能力，在保护用户隐私前提下，提升社区归属验证、服务范围识别和线下协同效率，使平台更贴近真实社区运行环境。",
    554: "（3）运营评估方面：后续可进一步完善志愿服务激励、活跃度评价和长期服务效果分析机制，围绕志愿者持续参与度、重点关怀覆盖情况、需求响应时效和服务满意度等指标构建更完整的社区公益治理评估体系。",
}


def main() -> None:
    updated = []
    skipped = []
    for path in DOC_PATHS:
        if not path.exists():
            continue
        try:
            doc = Document(str(path))
            paras = doc.paragraphs
            for idx, text in UPDATES.items():
                paras[idx].text = text
            if paras[555].text != "参考文献":
                paras[555].text = "参考文献"
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
