import os
import pathlib
import subprocess
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]


FAKE_ADB = r"""#!/usr/bin/env python3
import os
import pathlib
import shutil
import sys

args = sys.argv[1:]
log = pathlib.Path(os.environ["FAKE_ADB_LOG"])
with log.open("a", encoding="utf-8") as handle:
    handle.write(" ".join(args) + "\n")
if args[:1] == ["-s"]:
    args = args[2:]

if args == ["get-state"]:
    print("device")
    raise SystemExit(0)
if args[:2] == ["install", "-r"]:
    print("Success")
    raise SystemExit(0)
if args and args[0] == "pull":
    destination = pathlib.Path(args[2])
    source = os.environ.get("FAKE_REMOTE_CONF")
    if source and pathlib.Path(source).exists():
        shutil.copyfile(source, destination)
        raise SystemExit(0)
    raise SystemExit(1)
if args and args[0] == "push":
    shutil.copyfile(args[1], os.environ["FAKE_REMOTE_CONF"])
    print("1 file pushed")
    raise SystemExit(0)
if args and args[0] == "shell":
    command = args[1:]
    if command == ["getprop", "ro.product.manufacturer"]:
        print("Google")
    elif command == ["getprop", "ro.build.version.sdk"]:
        print("30")
    elif command[:2] == ["pm", "path"]:
        if command[2] == "tv.dumbtv.home":
            print("package:/data/app/tv.dumbtv.home/base.apk")
        else:
            raise SystemExit(1)
    elif command[:2] == ["dumpsys", "window"]:
        print("mCurrentFocus=Window{abc u0 tv.dumbtv.home/tv.dumbtv.home.Home}")
    elif command[:2] == ["am", "broadcast"]:
        print("Broadcast completed: result=1")
    raise SystemExit(0)
raise SystemExit(0)
"""


class LauncherToolsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        folder = pathlib.Path(self.temp.name)
        self.adb = folder / "adb"
        self.adb.write_text(FAKE_ADB, encoding="utf-8")
        self.adb.chmod(0o755)
        self.log = folder / "adb.log"
        self.remote = folder / "home.conf"
        self.env = os.environ.copy()
        self.env.update(
            ADB=str(self.adb),
            ADB_SERIAL="test-tv:5555",
            FAKE_ADB_LOG=str(self.log),
            FAKE_REMOTE_CONF=str(self.remote),
        )

    def run_script(self, name, *args):
        return subprocess.run(
            ["bash", str(ROOT / "launcher" / name), *args],
            cwd=ROOT,
            env=self.env,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )

    def test_standard_android_install_uses_home_role_without_accessibility(self):
        result = self.run_script("install.sh")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("standard Android HOME role", result.stdout)
        calls = self.log.read_text(encoding="utf-8")
        self.assertIn("cmd package set-home-activity tv.dumbtv.home/.Home", calls)
        self.assertNotIn("enabled_accessibility_services", calls)
        self.assertNotIn("pm disable", calls)

    def test_standard_android_undo_does_not_disable_launcher(self):
        result = self.run_script("install.sh", "--undo")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("HOME preference cleared", result.stdout)
        calls = self.log.read_text(encoding="utf-8")
        self.assertIn("clear-package-preferred-activities tv.dumbtv.home", calls)
        self.assertNotIn("pm disable", calls)

    def test_configure_uses_scoped_receiver_and_expands_profile(self):
        result = self.run_script(
            "configure.sh",
            "--weather-name", "Manchester",
            "--latitude", "53.4808",
            "--longitude", "-2.2426",
            "--timezone", "Europe/London",
            "--profile", "tcl-50t6c-uk",
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.log.read_text(encoding="utf-8")
        self.assertIn("--es key weather.name --es value Manchester", calls)
        self.assertIn("--es key weather.latitude --es value 53.4808", calls)
        self.assertIn("--es key shortcut.netflix --es value com.plexapp.android", calls)
        self.assertIn("--es key shortcut.prime --es value airplay", calls)
        self.assertIn("--es key shortcut.freely --es value com.spotify.tv.android", calls)
        self.assertNotIn(" pull ", calls)
        self.assertNotIn(" push ", calls)


if __name__ == "__main__":
    unittest.main()
