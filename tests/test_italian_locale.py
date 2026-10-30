"""The launcher must have complete, selectable Italian UI copy."""

from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1] / "launcher"
SOURCE = ROOT / "src/tv/dumbtv/home"


def strings(path):
    return {node.attrib["name"]: "".join(node.itertext())
            for node in ET.parse(path).getroot() if node.tag == "string"}


class ItalianLocaleTests(unittest.TestCase):
    def test_italian_covers_every_default_string(self):
        english = strings(ROOT / "res/values/strings.xml")
        italian = strings(ROOT / "res/values-it/strings.xml")
        self.assertEqual(english.keys(), italian.keys())
        self.assertTrue(all(value.strip() for value in italian.values()))
        self.assertEqual("Opzioni", italian["options"])
        self.assertEqual("Sposta", italian["move"])
        self.assertEqual("Buongiorno", italian["good_morning"])

    def test_every_java_string_reference_has_a_default(self):
        english = strings(ROOT / "res/values/strings.xml")
        references = set()
        for path in SOURCE.glob("*.java"):
            references.update(re.findall(r"R\.string\.([a-z][a-z0-9_]*)", path.read_text()))
        self.assertFalse(references - english.keys())

    def test_ui_copy_uses_resources_instead_of_english_literals(self):
        phrases = (
            "Good morning", "Could not open that", "Hidden. Bring it back",
            "Apps on home screen", "Reset the home screen layout",
            "No signal", "AirPlay is ready", "Partly cloudy",
        )
        ui = "\n".join((SOURCE / name).read_text() for name in
                       ("Home.java", "Options.java", "Input.java", "AirPlay.java", "Weather.java"))
        for phrase in phrases:
            with self.subTest(phrase=phrase):
                self.assertFalse('"' + phrase in ui, f"Hardcoded UI copy: {phrase}")


if __name__ == "__main__":
    unittest.main()
