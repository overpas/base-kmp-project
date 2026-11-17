import importlib.util
import io
import os
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


_ROOT = Path(__file__).resolve().parents[2]
_SPEC = importlib.util.spec_from_file_location("rename_project", _ROOT / "scripts/rename-project.py")
rename_project = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(rename_project)


def _copy_project(target):
    listed = subprocess.run(
        ["git", "-C", str(_ROOT), "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
        capture_output=True,
        check=True,
    )
    for relative in listed.stdout.decode().split("\0"):
        source = _ROOT / relative
        if relative and source.is_file() and not source.is_symlink():
            destination = target / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)


def _texts(root):
    for directory, _, names in os.walk(root):
        for name in names:
            path = Path(directory) / name
            try:
                yield path, path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue


def _rename(root, *arguments):
    with redirect_stdout(io.StringIO()):
        return rename_project.main([*arguments, "--root", str(root)])


class RenameProjectTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        cls.root = Path(cls.directory.name) / "project"
        _copy_project(cls.root)
        cls.old = rename_project.read_current(cls.root)
        _rename(cls.root, "--package", "com.example.notes", "--name", "notes", "--app-name", "Notes")

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def test_reads_the_new_names_back(self):
        self.assertEqual(("com.example.notes", "notes", "Notes"), rename_project.read_current(self.root))

    def test_leaves_no_old_package_in_any_file(self):
        old_package = self.old[0]
        leftovers = [
            str(path.relative_to(self.root))
            for path, text in _texts(self.root)
            if (old_package in text or old_package.replace(".", "/") in text)
            and str(path.relative_to(self.root)) not in rename_project.SELF
        ]
        self.assertEqual([], leftovers)

    def test_moves_the_sources_into_the_new_package(self):
        self.assertTrue((self.root / "shared/src/commonMain/kotlin/com/example/notes/App.kt").is_file())
        self.assertFalse((self.root / "shared/src/commonMain/kotlin/by").exists())

    def test_moves_the_sqldelight_schema_into_the_new_package(self):
        self.assertTrue((self.root / "core/db/src/commonMain/sqldelight/com/example/notes/core/db/Item.sq").is_file())

    def test_renames_the_ios_app(self):
        xcconfig = (self.root / "iosApp/Configuration/Config.xcconfig").read_text(encoding="utf-8")
        project = (self.root / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
        self.assertIn("PRODUCT_NAME=notes\n", xcconfig)
        self.assertIn("PRODUCT_BUNDLE_IDENTIFIER=com.example.notes.notes$(TEAM_ID)", xcconfig)
        self.assertIn("path = notes.app;", project)
        self.assertIn("PRODUCT_BUNDLE_IDENTIFIER = com.example.notes.notesUITests;", project)

    def test_renames_the_e2e_script_defaults(self):
        android = (self.root / "scripts/android-e2e-test.sh").read_text(encoding="utf-8")
        ios = (self.root / "scripts/ios-e2e.sh").read_text(encoding="utf-8")
        self.assertIn('APP_ID="${APP_ID:-com.example.notes}"', android)
        self.assertIn('BUNDLE_ID="${BUNDLE_ID:-com.example.notes.notes}"', ios)

    def test_renames_the_plugin_marketplace(self):
        marketplace = (self.root / ".claude-plugin/marketplace.json").read_text(encoding="utf-8")
        settings = (self.root / ".claude/settings.json").read_text(encoding="utf-8")
        self.assertIn('"name": "notes"', marketplace)
        self.assertIn('"chrisbanes-skills@notes": true', settings)

    def test_renames_the_app_data_folder(self):
        provider = next(self.root.glob("core/db/src/jvmMain/kotlin/**/AppDataPathProvider.kt")).read_text(encoding="utf-8")
        self.assertIn('DATA_DIR_PROPERTY = "notes.data.dir"', provider)
        self.assertIn('APP_DIR_NAME = "Notes"', provider)


class NestedPackageTests(unittest.TestCase):

    def test_renames_into_a_package_below_the_old_one(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "project"
            _copy_project(root)
            old_package = rename_project.read_current(root)[0]
            new_package = f"{old_package}.app"

            _rename(root, "--package", new_package, "--name", "app", "--app-name", "App")

            source = root.joinpath("shared/src/commonMain/kotlin", *new_package.split("."), "App.kt")
            build = (root / "androidApp/build.gradle.kts").read_text(encoding="utf-8")
            self.assertTrue(source.is_file())
            self.assertIn(f'applicationId = "{new_package}"', build)
            self.assertNotIn(f"{new_package}.app", build)

    def test_rejects_an_invalid_package(self):
        with tempfile.TemporaryDirectory() as directory, redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                rename_project.main(["--package", "Notes", "--name", "notes", "--app-name", "Notes", "--root", directory])


if __name__ == "__main__":
    unittest.main()
