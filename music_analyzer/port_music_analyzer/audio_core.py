"""Análise aproximada de BPM/tonalidade e mixer WAV multifaixa."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import threading
import numpy as np
import soundfile as sf


NOTE_NAMES = ("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá",
              "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
MAJOR_PROFILE = np.array([6.35, 2.23, 3.48, 2.33, 4.38, 4.09,
                          2.52, 5.19, 2.39, 3.66, 2.29, 2.88])
MINOR_PROFILE = np.array([6.33, 2.68, 3.52, 5.38, 2.60, 3.53,
                          2.54, 4.75, 3.98, 2.69, 3.34, 3.17])


def detect_key(chroma: np.ndarray) -> tuple[str, float]:
    """Correlacionar cromagrama médio com perfis maiores/menores de Krumhansl."""
    vector = np.mean(chroma, axis=1)
    if not np.any(np.isfinite(vector)) or float(np.max(vector)) < 1e-8:
        return "Indeterminado", 0.0
    scores: list[tuple[float, str]] = []
    for mode, profile in (("maior", MAJOR_PROFILE), ("menor", MINOR_PROFILE)):
        for root in range(12):
            shifted = np.roll(profile, root)
            correlation = np.corrcoef(vector, shifted)[0, 1]
            scores.append((float(correlation) if np.isfinite(correlation) else -1.0,
                           f"{NOTE_NAMES[root]} {mode}"))
    scores.sort(reverse=True)
    margin = max(0.0, scores[0][0] - scores[1][0])
    return scores[0][1], margin


def _peaks(y: np.ndarray, count: int = 450) -> list[float]:
    if not len(y):
        return []
    size = max(1, int(np.ceil(len(y) / count)))
    results = [float(np.max(np.abs(y[i:i + size]))) for i in range(0, len(y), size)]
    maximum = max(results, default=1.0)
    return [min(1.0, v / maximum) for v in results] if maximum else results


def analyze_track(filename: str) -> dict:
    """Analisa até 120 segundos para poupar RAM. Resultados são estimativas."""
    import librosa

    path = Path(filename)
    if not path.is_file():
        raise FileNotFoundError(filename)
    duration = float(librosa.get_duration(path=str(path)))
    y, sr = librosa.load(str(path), sr=22050, mono=True, duration=120)
    if y.size == 0:
        raise ValueError("O arquivo não contém áudio.")
    if float(np.max(np.abs(y))) < 1e-5:
        return {"duration": duration, "bpm": 0.0, "key": "Indeterminado",
                "key_margin": 0.0, "peaks": _peaks(y)}
    # Um onset envelope permite estimar BPM mesmo em músicas sem batidas muito claras.
    onset = librosa.onset.onset_strength(y=y, sr=sr)
    tempo, beats = librosa.beat.beat_track(onset_envelope=onset, sr=sr, trim=False)
    bpm = float(np.asarray(tempo).reshape(-1)[0]) if len(beats) >= 3 else 0.0
    # Harmonia da primeira parte (até 45 s) para reduzir influência de percussão.
    harmonic = librosa.effects.harmonic(y[:sr * 45])
    chroma = librosa.feature.chroma_stft(y=harmonic, sr=sr)
    key, margin = detect_key(chroma)
    return {"duration": duration, "bpm": round(bpm, 1), "key": key,
            "key_margin": round(margin, 3), "peaks": _peaks(y)}


def format_time(seconds: float) -> str:
    seconds = max(0, int(seconds))
    return f"{seconds // 60:02d}:{seconds % 60:02d}"


@dataclass
class TrackSettings:
    volume: float = 1.0
    muted: bool = False
    solo: bool = False


def _stereo(samples: np.ndarray) -> np.ndarray:
    if samples.ndim != 2:
        raise ValueError("O buffer precisa ter duas dimensões.")
    if samples.shape[1] == 1:
        return np.repeat(samples, 2, axis=1)
    return samples[:, :2]


class MixerEngine:
    """Reprodução local com streaming em blocos, sem carregar todas as faixas na RAM."""

    def __init__(self) -> None:
        self._lock = threading.RLock()
        self._files: dict[str, sf.SoundFile] = {}
        self.settings: dict[str, TrackSettings] = {}
        self._stream = None
        self.sample_rate = 44100
        self.total_frames = 0
        self.position_frames = 0

    def load(self, tracks: dict[str, str]) -> None:
        import sounddevice as sd  # noqa: F401  (verifica biblioteca e backend de áudio)
        self.close()
        new_files: dict[str, sf.SoundFile] = {}
        try:
            for name, filepath in tracks.items():
                new_files[name] = sf.SoundFile(filepath)
            if not new_files:
                raise ValueError("Nenhuma faixa separada disponível.")
            rates = {f.samplerate for f in new_files.values()}
            if len(rates) != 1:
                raise ValueError("As faixas precisam ter a mesma taxa de amostragem.")
            with self._lock:
                self._files = new_files
                self.settings = {name: TrackSettings() for name in new_files}
                self.sample_rate = next(iter(rates))
                self.total_frames = min(len(f) for f in new_files.values())
                self.position_frames = 0
        except Exception:
            for f in new_files.values():
                f.close()
            raise

    @property
    def duration(self) -> float:
        return self.total_frames / self.sample_rate if self.sample_rate else 0.0

    @property
    def position(self) -> float:
        with self._lock:
            return self.position_frames / self.sample_rate if self.sample_rate else 0.0

    def set_track(self, name: str, *, volume=None, muted=None, solo=None) -> None:
        with self._lock:
            if name not in self.settings:
                return
            s = self.settings[name]
            if volume is not None:
                s.volume = max(0.0, min(float(volume), 2.0))
            if muted is not None:
                s.muted = bool(muted)
            if solo is not None:
                s.solo = bool(solo)

    @staticmethod
    def _weights(settings: dict[str, TrackSettings]) -> dict[str, float]:
        has_solo = any(s.solo for s in settings.values())
        return {name: (0.0 if s.muted or (has_solo and not s.solo) else s.volume)
                for name, s in settings.items()}

    def _callback(self, outdata, frames, time_info, status) -> None:
        import sounddevice as sd
        with self._lock:
            outdata.fill(0)
            if self.position_frames >= self.total_frames:
                raise sd.CallbackStop
            weights = self._weights(self.settings)
            valid = min(frames, self.total_frames - self.position_frames)
            mix = np.zeros((valid, 2), dtype=np.float32)
            for name, handle in self._files.items():
                block = handle.read(valid, dtype="float32", always_2d=True)
                if len(block) < valid:
                    block = np.pad(block, ((0, valid-len(block)), (0, 0)))
                weight = weights[name]
                if weight:
                    mix += _stereo(block) * weight
            outdata[:valid] = np.clip(mix, -1.0, 1.0)
            self.position_frames += valid

    def play(self) -> None:
        import sounddevice as sd
        if not self._files:
            raise RuntimeError("Separe uma música antes de iniciar o mixer.")
        with self._lock:
            if self.position_frames >= self.total_frames:
                self.seek(0)
            if self._stream is None:
                self._stream = sd.OutputStream(samplerate=self.sample_rate, channels=2,
                                               dtype="float32", blocksize=2048,
                                               callback=self._callback)
            stream = self._stream
        if not stream.active:
            stream.start()

    def pause(self) -> None:
        stream = self._stream
        if stream is not None and stream.active:
            stream.stop()

    def seek(self, seconds: float) -> None:
        with self._lock:
            frame = min(self.total_frames, max(0, int(seconds * self.sample_rate)))
            for handle in self._files.values():
                handle.seek(frame)
            self.position_frames = frame

    def export_mix(self, output_path: str, log=lambda message: None) -> str:
        if not self._files:
            raise RuntimeError("Não há faixas para exportar.")
        with self._lock:
            paths = {name: file.name for name, file in self._files.items()}
            settings = {name: TrackSettings(v.volume, v.muted, v.solo)
                        for name, v in self.settings.items()}
        weights = self._weights(settings)
        files = {name: sf.SoundFile(filename) for name, filename in paths.items()}
        try:
            rate = next(iter(files.values())).samplerate
            total = min(len(f) for f in files.values())
            suffix = Path(output_path).suffix.lower()
            if suffix not in (".wav", ".flac"):
                raise ValueError("Exporte a mixagem como WAV ou FLAC.")
            with sf.SoundFile(output_path, "w", samplerate=rate, channels=2) as output:
                while output.tell() < total:
                    size = min(8192, total - output.tell())
                    mix = np.zeros((size, 2), dtype=np.float32)
                    for name, handle in files.items():
                        block = handle.read(size, dtype="float32", always_2d=True)
                        if weights[name]:
                            mix += _stereo(block) * weights[name]
                    output.write(np.clip(mix, -1, 1))
            log("Mixagem exportada com sucesso.")
            return output_path
        finally:
            for file in files.values():
                file.close()

    def close(self) -> None:
        self.pause()
        stream, self._stream = self._stream, None
        if stream is not None:
            stream.close()
        with self._lock:
            for handle in self._files.values():
                handle.close()
            self._files.clear()
            self.settings.clear()
            self.total_frames = 0
            self.position_frames = 0
