#!/usr/bin/env python3
import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SELF = {"scripts/rename-project.py", "scripts/tests/test_rename_project.py"}
SKIPPED_DIRS = {".git", ".gradle", ".kotlin", ".idea", "build", "node_modules", ".allure", ".demos", ".codegraph"}
SOURCE_ROOTS = {"kotlin", "sqldelight"}
PACKAGE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
NAME = re.compile(r"^[a-z][a-z0-9-]*$")
STRINGS = "core/strings/src/commonMain/composeResources/values/strings.xml"


def read_current(root):
    def find(relative, pattern):
        match = re.search(pattern, (root / relative).read_text(encoding="utf-8"))
        if not match:
            sys.exit(f"Cannot find the current value in {relative}")
        return match.group(1)

    return (
        find("androidApp/build.gradle.kts", r'applicationId = "([^"]+)"'),
        find("settings.gradle.kts", r'rootProject\.name = "([^"]+)"'),
        find(STRINGS, r'<string name="app_name">([^<]+)</string>'),
    )


def tracked_files(root):
    completed = subprocess.run(["git", "-C", str(root), "ls-files", "-z", "--cached", "--others", "--exclude-standard"], capture_output=True)
    if completed.returncode == 0 and completed.stdout:
        return [path for path in completed.stdout.decode().split("\0") if path and (root / path).is_file()]
    files = []
    for directory, subdirectories, names in os.walk(root):
        subdirectories[:] = [name for name in subdirectories if name not in SKIPPED_DIRS]
        files += [os.path.relpath(os.path.join(directory, name), root) for name in names]
    return files


PACKAGE_TOKEN = "\0package\0"
PATH_TOKEN = "\0path\0"
NAME_TOKEN = "\0name\0"
APP_TOKEN = "\0app\0"
APP_LOWER_TOKEN = "\0app-lower\0"


def replacements(old):
    old_package, old_name, old_app = old
    package = re.escape(old_package)
    name = re.escape(old_name)
    app = re.escape(old_app)
    return [
        (re.compile(rf"{package}\.{name}UITests"), f"{PACKAGE_TOKEN}.{NAME_TOKEN}UITests"),
        (re.compile(rf"{package}\.{name}\$\(TEAM_ID\)"), f"{PACKAGE_TOKEN}.{NAME_TOKEN}$(TEAM_ID)"),
        (re.compile(rf"(BUNDLE_ID:-){package}\.{name}\}}"), rf"\g<1>{PACKAGE_TOKEN}.{NAME_TOKEN}}}"),
        (re.compile(rf"(?<![\w.]){package}(?!\w)"), PACKAGE_TOKEN),
        (re.compile(rf"(?<!\w){re.escape(old_package.replace('.', '/'))}(?!\w)"), PATH_TOKEN),
        (re.compile(rf'rootProject\.name = "{name}"'), f'rootProject.name = "{NAME_TOKEN}"'),
        (re.compile(rf"PRODUCT_NAME={name}$", re.M), f"PRODUCT_NAME={NAME_TOKEN}"),
        (re.compile(rf"(?<![\w.-]){name}\.app\b"), f"{NAME_TOKEN}.app"),
        (re.compile(rf"(?<![\w.-]){name}\.data\.dir\b"), f"{NAME_TOKEN}.data.dir"),
        (re.compile(rf'"{name} coverage"'), f'"{NAME_TOKEN} coverage"'),
        (re.compile(rf"Pictures/{name}-e2e"), f"Pictures/{NAME_TOKEN}-e2e"),
        (re.compile(rf"@{name}(?![\w-])"), f"@{NAME_TOKEN}"),
        (re.compile(rf'"name": "{name}"'), f'"name": "{NAME_TOKEN}"'),
        (re.compile(rf'^(\s*)"{name}": \{{', re.M), rf'\g<1>"{NAME_TOKEN}": {{'),
        (re.compile(rf'<string name="app_name">{app}</string>'), f'<string name="app_name">{APP_TOKEN}</string>'),
        (re.compile(rf'APP_DIR_NAME = "{app}"'), f'APP_DIR_NAME = "{APP_TOKEN}"'),
        (re.compile(rf"(\\Local\\|Support/){app}(?=\")"), rf"\g<1>{APP_TOKEN}"),
        (re.compile(rf'/\.{re.escape(old_app.lower())}"'), f'/.{APP_LOWER_TOKEN}"'),
        (re.compile(rf"^# {app}$", re.M), f"# {APP_TOKEN}"),
    ]


def tokens(new):
    new_package, new_name, new_app = new
    return {
        PACKAGE_TOKEN: new_package,
        PATH_TOKEN: new_package.replace(".", "/"),
        NAME_TOKEN: new_name,
        APP_TOKEN: new_app,
        APP_LOWER_TOKEN: new_app.lower(),
    }


def rewrite(root, files, rules, values):
    changed = []
    for relative in files:
        if relative in SELF:
            continue
        path = root / relative
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        result = text
        for pattern, replacement in rules:
            result = pattern.sub(lambda match, replacement=replacement: match.expand(replacement), result)
        for token, value in values.items():
            result = result.replace(token, value)
        if result != text:
            path.write_text(result, encoding="utf-8")
            changed.append(relative)
    return changed


def move_sources(root, old_package, new_package):
    old_parts = old_package.split(".")
    new_parts = new_package.split(".")
    moved = []
    for directory, subdirectories, _ in os.walk(root):
        subdirectories[:] = [name for name in subdirectories if name not in SKIPPED_DIRS]
        base = Path(directory)
        if base.name not in SOURCE_ROOTS or base.parent.parent.name != "src":
            continue
        source = base.joinpath(*old_parts)
        if not source.is_dir():
            continue
        staging = base / ".rename-project"
        shutil.move(str(source), str(staging))
        parent = source.parent
        while parent != base and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent
        target = base.joinpath(*new_parts)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(staging), str(target))
        moved.append(str(base.relative_to(root)))
        subdirectories[:] = []
    return moved


def main(arguments=None):
    parser = argparse.ArgumentParser(description="Rename the template project.")
    parser.add_argument("--package", required=True, help="root package and app id, such as com.example.notes")
    parser.add_argument("--name", required=True, help="project name in lowercase, such as notes")
    parser.add_argument("--app-name", required=True, help="name the user sees, such as Notes")
    parser.add_argument("--root", type=Path, default=ROOT, help=argparse.SUPPRESS)
    options = parser.parse_args(arguments)
    if not PACKAGE.match(options.package):
        parser.error(f"{options.package} is not a package such as com.example.notes")
    if not NAME.match(options.name):
        parser.error(f"{options.name} is not a lowercase name such as notes")
    if not options.app_name.strip() or re.search(r'[<>&"\\]', options.app_name):
        parser.error("the app name must not be empty or hold < > & \" or \\")
    root = options.root.resolve()
    old = read_current(root)
    new = (options.package, options.name, options.app_name)
    if old == new:
        print("The project already has these names.")
        return 0
    changed = rewrite(root, tracked_files(root), replacements(old), tokens(new))
    moved = move_sources(root, old[0], new[0]) if old[0] != new[0] else []
    print(f"Renamed {old[0]} to {new[0]}, {old[1]} to {new[1]} and {old[2]} to {new[2]}.")
    print(f"Changed {len(changed)} files and moved the sources of {len(moved)} source sets.")
    print("Next: ./gradlew detekt jvmTest, then open iosApp in Xcode once to refresh it.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
