from __future__ import annotations

import os
from pathlib import Path

from docx import Document


UPDATES = {
    70: (
        "With the deepening of population aging and the continuous advancement of "
        "digital governance at the grassroots level, community public welfare "
        "services have exposed practical problems in supply-demand matching, "
        "response efficiency, and process traceability. This paper designs and "
        "implements a community public welfare service matching and management "
        "platform, and builds a business closed loop around community joining, "
        "demand submission, review circulation, volunteer claiming, service "
        "completion, evaluation feedback, and anomaly alerts."
    ),
    71: (
        "The system adopts a front-end and back-end separation architecture. "
        "The back end provides RESTful APIs based on Spring Boot 3 and uses "
        "MyBatis-Plus together with MySQL 8 for business data persistence; "
        "the administration side is developed with Vue 3 and Naive UI, while "
        "the mobile side is implemented with Vue 3 and NutUI."
    ),
    72: (
        "The system realizes functions such as unified accounts, community "
        "binding, demand review, volunteer certification, service tracking, "
        "evaluation feedback, key care-subject management, and anomaly alerts."
    ),
    73: (
        "It can improve the discovery efficiency of community public welfare "
        "needs, the traceability of service processes, and the governance "
        "capacity of the administration side, providing a practical digital "
        "support solution for grassroots communities in scenarios such as "
        "elderly assistance, disability assistance, and accompanying medical visits."
    ),
}


def main() -> None:
    path = Path(os.environ["DOCX_PATH"])
    doc = Document(str(path))
    for idx, text in UPDATES.items():
        doc.paragraphs[idx].text = text
    doc.save(str(path))
    print(path)


if __name__ == "__main__":
    main()
