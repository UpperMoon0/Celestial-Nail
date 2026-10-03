"""Pinned runtime installation must not depend on loader catalogue discovery."""
import json
from pathlib import Path
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch
import zipfile

from run_sodium_extras_compat import install_runtime


class RuntimeInstallerTest(unittest.TestCase):
    def setUp(self):
        self.loader = Mock()
        self.loader.get_installed_version.return_value = "neoforge-21.1.228"
        self.library = SimpleNamespace(
            install=Mock(), mod_loader=Mock(), vanilla_launcher=Mock())
        self.library.mod_loader.get_mod_loader.return_value = self.loader
        self.modules = patch.dict("sys.modules", {"minecraft_launcher_lib": self.library})
        self.modules.start()
        self.addCleanup(self.modules.stop)

    def installer(self, directory, game="1.21.1", version="neoforge-21.1.228"):
        path = Path(directory) / "installer.jar"
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("install_profile.json", json.dumps({"minecraft": game, "version": version}))
        return path

    def test_pinned_neoforge_installs_without_catalogue_and_finishes_libraries(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = self.installer(directory)
            launcher = Path(directory) / "launcher"
            with patch("run_sodium_extras_compat.prepare", return_value=artifact), \
                    patch("run_sodium_extras_compat.subprocess.run") as run:
                self.assertEqual("neoforge-21.1.228", install_runtime("neoforge-1.21.1", launcher, "java21"))
            self.assertEqual([("1.21.1", launcher.resolve()), ("neoforge-21.1.228", launcher.resolve())],
                             [call.args for call in self.library.install.install_minecraft_version.call_args_list])
            self.library.vanilla_launcher.ensure_vanilla_launcher_profiles_exists.assert_called_once_with(launcher.resolve())
            self.assertEqual(["java21", "-jar", str(artifact.resolve()), "--install-client", str(launcher.resolve())], run.call_args.args[0])
            self.assertTrue(run.call_args.kwargs["check"])
            self.loader.install.assert_not_called()
            self.loader.get_minecraft_versions.assert_not_called()

    def test_wrong_game_or_loader_is_rejected_before_installation(self):
        for game, version in [("1.21.2", "neoforge-21.1.228"), ("1.21.1", "neoforge-21.1.229")]:
            with self.subTest(game=game, version=version), tempfile.TemporaryDirectory() as directory:
                artifact = self.installer(directory, game, version)
                with patch("run_sodium_extras_compat.prepare", return_value=artifact), \
                        patch("run_sodium_extras_compat.subprocess.run") as run:
                    with self.assertRaisesRegex(ValueError, "does not match"):
                        install_runtime("neoforge-1.21.1", Path(directory) / "launcher", "java21")
                run.assert_not_called()
                self.library.install.install_minecraft_version.assert_not_called()

    def test_installer_failure_does_not_complete_runtime(self):
        with tempfile.TemporaryDirectory() as directory:
            artifact = self.installer(directory)
            with patch("run_sodium_extras_compat.prepare", return_value=artifact), \
                    patch("run_sodium_extras_compat.subprocess.run", side_effect=subprocess.CalledProcessError(1, "java")):
                with self.assertRaises(subprocess.CalledProcessError):
                    install_runtime("neoforge-1.21.1", Path(directory) / "launcher", "java21")
            self.assertEqual(1, self.library.install.install_minecraft_version.call_count)

    def test_other_loaders_keep_standard_installer(self):
        for target, pinned in [("forge-1.20.1", "47.4.0"), ("fabric-1.20.1", "0.18.4"), ("fabric-1.21.1", "0.18.4")]:
            with self.subTest(target=target), patch("run_sodium_extras_compat.prepare") as prepare:
                self.loader.install.reset_mock()
                install_runtime(target, Path("launcher"), "java21")
                self.loader.install.assert_called_once_with(target.split("-", 1)[1], Path("launcher"), loader_version=pinned, java="java21")
                prepare.assert_not_called()


if __name__ == "__main__":
    unittest.main()
