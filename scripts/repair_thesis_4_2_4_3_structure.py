from pathlib import Path
from docx import Document


DOC_PATHS = [
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1.docx",
    Path("毕业论文") / "天津商业大学毕业设计(论文)张正豪v4.1-5.1已修订.docx",
]


P1 = "社区加入与后台审核治理流程用于保证平台的数据边界与社区归属关系正确。用户注册后默认并不直接拥有社区数据访问权限，而是需要通过邀请码校验或提交加入申请的方式与具体社区建立绑定关系；管理员审核通过后，系统将用户的 community_id 写入账户信息，后续公告、便民信息、公益需求和治理数据均按该字段进行隔离。"
P2 = "在治理流程上，社区加入审核、需求审核和志愿者审核共同构成后台人工把关机制。也就是说，系统并不把关键节点完全交给自动流转，而是在加入社区、发布需求和获得志愿者资格等环节设置人工确认步骤，从而降低错误绑定、虚假求助和无效接单等风险，保证平台运行秩序。"
DB_INTRO = "系统开发当中，数据库存储设计是一个非常重要的环节，它直接影响到项目的性能、稳定性和可扩展性。本系统采用 MySQL 8 作为关系型数据库，后端通过 MyBatis-Plus 完成数据持久化与对象映射，本小节将结合当前业务主线，对数据库概念模型与核心表结构进行说明。"


def main() -> None:
    updated = []
    for path in DOC_PATHS:
        if not path.exists():
            continue
        doc = Document(str(path))
        paras = doc.paragraphs

        paras[245].text = "4.2.2  社区加入与后台审核治理流程设计"
        paras[248].text = "4.3  数据库存储设计"
        paras[249].text = DB_INTRO

        # Avoid duplicate insertion on repeated runs.
        has_p1 = any(p.text == P1 for p in paras[246:250])
        if not has_p1:
            p2 = paras[248].insert_paragraph_before(P2)
            p1 = paras[248].insert_paragraph_before(P1)
            p1.style = paras[247].style
            p2.style = paras[247].style
        else:
            if paras[246].text == P2 and paras[247].text == P1:
                paras[246].text = P1
                paras[247].text = P2

        doc.save(str(path))
        updated.append(str(path))

    print("UPDATED:")
    for item in updated:
        print(item)


if __name__ == "__main__":
    main()
