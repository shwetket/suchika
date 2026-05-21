#!/usr/bin/env python3
"""
documentWriter: staging, consolidation, and README tree sync for .md files.

Run from repository root: python tools/documentWriter.py
"""
import shutil
from pathlib import Path
from datetime import datetime

IGNORED_DIRS = {'.git', 'node_modules', '.gradle', '.idea', 'build', 'out', 'target', '.github'}


def is_ignored(path: Path) -> bool:
    for part in path.parts:
        if part in IGNORED_DIRS:
            return True
    return False


def collect_unorganized_md(repo_root: Path):
    md_files = []
    for p in repo_root.rglob('*.md'):
        try:
            rel = p.relative_to(repo_root)
        except Exception:
            continue
        # skip root README
        if rel == Path('README.md'):
            continue
        # skip anything already in documents or .github
        if rel.parts and rel.parts[0] in ('documents', '.github'):
            continue
        # skip hidden/system files
        if is_ignored(rel):
            continue
        md_files.append(p)
    return md_files


def make_unique_name(repo_root: Path, src: Path, target_dir: Path):
    rel = src.relative_to(repo_root)
    parts = rel.with_suffix('').parts
    name = '_'.join(parts) + '.md'
    dest = target_dir / name
    i = 1
    base = dest.stem
    while dest.exists():
        dest = target_dir / f"{base}_{i}.md"
        i += 1
    return dest


def move_to_temp(repo_root: Path, files):
    temp = repo_root / 'documents' / 'temp'
    temp.mkdir(parents=True, exist_ok=True)
    moved = []
    for p in files:
        dest = make_unique_name(repo_root, p, temp)
        shutil.move(str(p), str(dest))
        moved.append((p, dest))
    return moved


def classify_file(path: Path):
    text = path.read_text(encoding='utf-8').lower()
    if any(k in text for k in ('requirement', 'user story', 'acceptance', 'business requirement', 'functional')):
        return 'business'
    if any(k in text for k in ('architecture', 'adr', 'design decision', 'diagram', 'hexagonal', 'architecture decision')):
        return 'architecture'
    if any(k in text for k in ('roadmap', 'timeline', 'milestone', 'schedule')):
        return 'management'
    return 'other'


MASTER_MAP = {
    'business': Path('documents') / 'BUSINESS_REQUIREMENTS.md',
    'architecture': Path('documents') / 'ARCHITECTURE.md',
    'management': Path('documents') / 'ROADMAP.md'
}


def split_sections(text: str):
    # simple split: sections start at lines beginning with '#'
    lines = text.splitlines()
    sections = []
    current = []
    for line in lines:
        if line.startswith('#') and current:
            sections.append('\n'.join(current))
            current = [line]
        else:
            current.append(line)
    if current:
        sections.append('\n'.join(current))
    return sections


def heading_of_section(section: str):
    for line in section.splitlines():
        if line.startswith('#'):
            return line.strip()
    return None


def merge_into_master(temp_path: Path, master_path: Path):
    master_text = master_path.read_text(encoding='utf-8') if master_path.exists() else ''
    temp_text = temp_path.read_text(encoding='utf-8')
    sections = split_sections(temp_text)
    appended = False
    with master_path.open('a', encoding='utf-8') as m:
        for sec in sections:
            heading = heading_of_section(sec)
            if heading and heading in master_text:
                continue
            # avoid duplicating small common phrases: check if exact section exists
            if sec.strip() and sec.strip() in master_text:
                continue
            m.write('\n\n' + sec.strip() + '\n')
            appended = True
    return appended


def promote_to_documents(temp_path: Path, repo_root: Path):
    dest_dir = repo_root / 'documents'
    dest_dir.mkdir(parents=True, exist_ok=True)
    dest = make_unique_name(repo_root, temp_path, dest_dir)
    shutil.move(str(temp_path), str(dest))
    return dest


def process_temp(repo_root: Path):
    temp_dir = repo_root / 'documents' / 'temp'
    if not temp_dir.exists():
        return []
    results = []
    for p in sorted(temp_dir.iterdir()):
        if not p.is_file() or p.suffix.lower() != '.md':
            continue
        category = classify_file(p)
        master = MASTER_MAP.get(category)
        if master and master.exists():
            merged = merge_into_master(p, master)
            if merged:
                p.unlink()
                results.append((p.name, 'merged', master.name))
            else:
                # nothing merged, promote as standalone
                promoted = promote_to_documents(p, repo_root)
                results.append((p.name, 'promoted', promoted.name))
        elif master:
            # promote and rename to canonical master name
            dest = repo_root / master
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(p), str(dest))
            results.append((p.name, 'promoted_as_master', dest.name))
        else:
            # other category -> promote as standalone
            promoted = promote_to_documents(p, repo_root)
            results.append((p.name, 'promoted', promoted.name))
    return results


def generate_tree(repo_root: Path):
    lines = []
    def walk(dir_path: Path, indent: str = ''):
        items = sorted([p for p in dir_path.iterdir() if p.name not in IGNORED_DIRS], key=lambda x: (x.is_file(), x.name.lower()))
        for idx, item in enumerate(items):
            connector = '├── ' if idx < len(items)-1 else '└── '
            rel = item.relative_to(repo_root)
            lines.append(f"{indent}{connector}{rel.name}{'/' if item.is_dir() else ''}")
            if item.is_dir():
                walk(item, indent + ('│   ' if idx < len(items)-1 else '    '))

    lines.append(f"{repo_root.name}/")
    walk(repo_root)
    return '\n'.join(lines)


def replace_tree_in_readme(repo_root: Path, tree_text: str):
    readme = repo_root / 'README.md'
    if not readme.exists():
        return False
    content = readme.read_text(encoding='utf-8')
    header = '## 📁 Repository Structure'
    if header in content:
        before, rest = content.split(header, 1)
        if '```' in rest:
            pre_code, code_and_after = rest.split('```', 1)
            if '```' in code_and_after:
                _, after = code_and_after.split('```', 1)
            else:
                after = ''
            new_code_block = '\n```\n' + tree_text + '\n```\n'
            new_rest = pre_code + new_code_block + after
            new_content = before + header + new_rest
            readme.write_text(new_content, encoding='utf-8')
            return True
        else:
            new_content = content + '\n' + header + '\n```\n' + tree_text + '\n```\n'
            readme.write_text(new_content, encoding='utf-8')
            return True
    else:
        new_content = content + '\n\n' + header + '\n```\n' + tree_text + '\n```\n'
        readme.write_text(new_content, encoding='utf-8')
        return True


def main():
    repo_root = Path.cwd()
    # Phase 1: Isolation staging - move unorganized md into documents/temp
    unorganized = collect_unorganized_md(repo_root)
    moved = move_to_temp(repo_root, unorganized) if unorganized else []

    # Phase 2 & 3: Context evaluation and consolidation
    results = process_temp(repo_root)

    # Phase 4: Tree sync
    tree = generate_tree(repo_root)
    replace_tree_in_readme(repo_root, tree)

    # Caveman confirmation with summary
    ts = datetime.utcnow().isoformat() + 'Z'
    print(f"OOK! documentWriter run at {ts}. Moved {len(moved)} files to documents/temp. Processed {len(results)} temp files. Root README updated.")


if __name__ == '__main__':
    main()
