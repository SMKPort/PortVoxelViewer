"""Testes de regressão básicos executados na compilação."""
import unittest
import numpy as np
from port_music_analyzer.audio_core import (
    MAJOR_PROFILE, TrackSettings, MixerEngine, detect_key, format_time, _peaks, _stereo
)


class MusicCoreTests(unittest.TestCase):
    def test_time(self):
        self.assertEqual(format_time(125.9), "02:05")
        self.assertEqual(format_time(-10), "00:00")

    def test_major_key_template(self):
        chroma = np.tile(MAJOR_PROFILE[:, None], (1, 30))
        name, confidence = detect_key(chroma)
        self.assertEqual(name, "Dó maior")
        self.assertGreater(confidence, 0)

    def test_silence_key(self):
        key, margin = detect_key(np.zeros((12, 40)))
        self.assertEqual(key, "Indeterminado")
        self.assertEqual(margin, 0)

    def test_waveform(self):
        values = _peaks(np.array([0.0, 0.5, -1.0], dtype=np.float32))
        self.assertTrue(all(0 <= x <= 1 for x in values))
        self.assertEqual(max(values), 1.0)

    def test_solo_and_mute(self):
        config = {
            "vocals": TrackSettings(volume=0.8, solo=True),
            "bass": TrackSettings(volume=0.7),
            "drums": TrackSettings(volume=1.0, muted=True),
        }
        weights = MixerEngine._weights(config)
        self.assertEqual(weights["vocals"], 0.8)
        self.assertEqual(weights["bass"], 0)
        self.assertEqual(weights["drums"], 0)

    def test_mono_to_stereo(self):
        x = _stereo(np.array([[0.1], [-0.2]], dtype=np.float32))
        self.assertEqual(x.shape, (2, 2))
        np.testing.assert_array_equal(x[:, 0], x[:, 1])


if __name__ == "__main__":
    unittest.main()
