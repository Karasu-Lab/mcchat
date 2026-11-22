import os
import re
import subprocess
from datetime import date
from pathlib import Path

CHANGELOG = Path("CHANGELOG.md")
CHANGELOG_HEADER = (
    "# Changelog\n\nAll notable changes to this project will be documented in this file.\n"
)
RELEASABLE_TYPES = {"feat", "fix", "perf", "refactor", "revert"}
SECTIONS = [("feat", "Added"), ("fix", "Fixed")]
COMMIT_PATTERN = re.compile(r"^(?P<type>\w+)(\([^)]*\))?(?P<bang>!)?: (?P<desc>.+)$")


def git(*args):
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout


def read_properties_version(path):
    match = re.search(r"^version=(.+)$", path.read_text(), re.M)
    return match.group(1).strip()


def write_properties_version(path, version):
    path.write_text(
        re.sub(r"^version=.*$", f"version={version}", path.read_text(), flags=re.M)
    )


def read_android_version(path):
    match = re.search(r'versionName = "([^"]+)"', path.read_text())
    return match.group(1)


def write_android_version(path, version):
    text = path.read_text()
    text = re.sub(r'versionName = "[^"]+"', f'versionName = "{version}"', text)
    text = re.sub(
        r"versionCode = (\d+)", lambda m: f"versionCode = {int(m.group(1)) + 1}", text
    )
    path.write_text(text)


COMPONENTS = [
    {
        "name": "mcchat",
        "path": "mods/mcchat",
        "version_file": Path("mods/mcchat/gradle.properties"),
        "read": read_properties_version,
        "write": write_properties_version,
    },
    {
        "name": "mcchat-native",
        "path": "apps/MCChatNative",
        "version_file": Path("apps/MCChatNative/app/build.gradle.kts"),
        "read": read_android_version,
        "write": write_android_version,
    },
]


def commit_range(name):
    last_release = git(
        "log", "-1", "--format=%H", "-E", f"--grep=^release:.*(: |, ){name} v"
    ).strip()
    return f"{last_release}..HEAD" if last_release else "HEAD"


def collect_commits(name, path):
    log = git(
        "log", "--no-merges", "--format=%s%x1f%b%x1e", commit_range(name), "--", path
    )
    commits = []
    for record in log.split("\x1e"):
        if not record.strip():
            continue
        subject, _, body = record.strip().partition("\x1f")
        match = COMMIT_PATTERN.match(subject)
        if not match or match["type"] == "release":
            continue
        breaking = bool(match["bang"]) or "BREAKING CHANGE" in body
        if match["type"] in RELEASABLE_TYPES or breaking:
            commits.append((match["type"], match["desc"], breaking))
    return commits


def bump(version, commits):
    major, minor, patch = (list(map(int, version.split("."))) + [0, 0])[:3]
    if any(breaking for _, _, breaking in commits):
        return f"{major + 1}.0.0"
    if any(kind == "feat" for kind, _, _ in commits):
        return f"{major}.{minor + 1}.0"
    return f"{major}.{minor}.{patch + 1}"


def render_entry(name, version, commits):
    lines = [f"## [{name}] {version} - {date.today().isoformat()}", ""]
    grouped = {title: [] for _, title in SECTIONS}
    grouped["Changed"] = []
    titles = dict(SECTIONS)
    for kind, desc, _ in commits:
        grouped[titles.get(kind, "Changed")].append(desc)
    for title, items in grouped.items():
        if items:
            lines += [f"### {title}", *[f"- {item}" for item in items], ""]
    return "\n".join(lines) + "\n"


def insert_entries(entries):
    text = CHANGELOG.read_text() if CHANGELOG.exists() else CHANGELOG_HEADER
    index = text.find("\n## ")
    if index == -1:
        text = text.rstrip("\n") + "\n\n" + entries
    else:
        text = text[: index + 1] + entries + text[index + 1 :]
    CHANGELOG.write_text(text)


def write_output(**values):
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        for key, value in values.items():
            output.write(f"{key}={value}\n")


def main():
    entries = []
    releases = []
    for component in COMPONENTS:
        commits = collect_commits(component["name"], component["path"])
        if not commits:
            continue
        version = bump(component["read"](component["version_file"]), commits)
        component["write"](component["version_file"], version)
        entries.append(render_entry(component["name"], version, commits))
        releases.append(f"{component['name']} v{version}")

    if not releases:
        write_output(released="false")
        return

    insert_entries("".join(entries))
    write_output(released="true", message="release: " + ", ".join(releases))


main()
