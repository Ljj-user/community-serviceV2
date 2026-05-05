from pathlib import Path


def main() -> None:
    path = Path("AGENTS.md")
    lines = path.read_text(encoding="utf-8").splitlines()
    print(f"TOTAL={len(lines)}")
    for idx in range(240, min(340, len(lines))):
        print(f"{idx + 1:03d}: {lines[idx]}")


if __name__ == "__main__":
    main()
