"""Instalação isolada e execução do Demucs no Windows."""
from __future__ import annotations
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import sys

APP_DIR = Path(os.environ.get("LOCALAPPDATA", Path.home() / ".port_music_analyzer")) / "PortMusicAnalyzer"
AI_DIR = APP_DIR / "ai_runtime"
STEMS = ("vocals", "drums", "bass", "guitar", "piano", "other")
MODELS = ("htdemucs", "htdemucs_ft", "htdemucs_6s")


def _venv_python() -> Path:
    return AI_DIR / ("Scripts/python.exe" if os.name == "nt" else "bin/python")


def _probe_python(command: list[str]) -> str | None:
    try:
        result = subprocess.run(
            command + ["-c", "import sys; print(sys.executable)"],
            capture_output=True, text=True, timeout=12, check=True
        )
        return result.stdout.strip() or None
    except (OSError, subprocess.SubprocessError):
        return None


def find_system_python() -> str:
    """Usar Python 3.11 para manter compatibilidade com o Demucs 4.0.1."""
    candidates = [["py", "-3.11"], ["python3.11"], ["python"]]
    if not getattr(sys, "frozen", False) and sys.version_info[:2] == (3, 11):
        return sys.executable
    for cmd in candidates:
        found = _probe_python(cmd)
        if found:
            version = subprocess.run([found, "-c", "import sys;print(sys.version_info[:2])"],
                                     capture_output=True, text=True).stdout
            if "(3, 11)" in version:
                return found
    raise RuntimeError(
        "Para instalar a IA, instale o Python 3.11 (64 bits), marque "
        "'Add Python to PATH' e reinicie o aplicativo."
    )


def ai_python() -> str:
    exe = _venv_python()
    if exe.is_file():
        return str(exe)
    if not getattr(sys, "frozen", False) and importlib.util.find_spec("demucs"):
        return sys.executable
    raise RuntimeError("A IA ainda não foi instalada. Abra Configurações e clique em 'Instalar IA'.")


def _run(cmd: list[str], log) -> None:
    # Never shell=True: paths are passed as independent arguments.
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               encoding="utf-8", errors="replace", bufsize=1)
    assert process.stdout is not None
    for line in process.stdout:
        line = line.strip()
        if line:
            log(line)
    code = process.wait()
    if code:
        raise RuntimeError(f"Comando terminou com código {code}. Consulte o registro de execução.")


def install_ai(log=lambda message: None) -> str:
    base_python = find_system_python()
    AI_DIR.parent.mkdir(parents=True, exist_ok=True)
    if not _venv_python().exists():
        log("Criando ambiente isolado para a IA...")
        _run([base_python, "-m", "venv", str(AI_DIR)], log)
    python = str(_venv_python())
    log("Preparando dependências da IA (download necessário)...")
    _run([python, "-m", "pip", "install", "--upgrade", "pip"], log)
    # Versioned CPU wheels; GPU users may replace these in the AI venv.
    _run([python, "-m", "pip", "install", "torch==2.3.1", "torchaudio==2.3.1",
          "--index-url", "https://download.pytorch.org/whl/cpu"], log)
    _run([python, "-m", "pip", "install", "demucs==4.0.1", "numpy<2", "soundfile"], log)
    _run([python, "-c", "import demucs, torch, torchaudio; print('Demucs pronto')"], log)
    return "IA instalada. Os modelos serão baixados na primeira separação."


def _ffmpeg_env() -> dict[str, str]:
    env = os.environ.copy()
    if shutil.which("ffmpeg", path=env.get("PATH")):
        return env
    try:
        import imageio_ffmpeg
        ffmpeg = Path(imageio_ffmpeg.get_ffmpeg_exe())
        target = APP_DIR / "bin" / ("ffmpeg.exe" if os.name == "nt" else "ffmpeg")
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.is_file() or target.stat().st_size != ffmpeg.stat().st_size:
            shutil.copy2(ffmpeg, target)
        env["PATH"] = str(target.parent) + os.pathsep + env.get("PATH", "")
    except Exception as exc:
        raise RuntimeError("Não foi possível preparar o FFmpeg: " + str(exc)) from exc
    return env


def separate(input_path: str, output_root: str, model: str, log=lambda msg: None) -> dict[str, str]:
    if model not in MODELS:
        raise ValueError("Modelo de separação desconhecido.")
    source = Path(input_path)
    if not source.is_file():
        raise FileNotFoundError(source)
    destination = Path(output_root)
    destination.mkdir(parents=True, exist_ok=True)
    python = ai_python()
    log("Iniciando separação local. Na primeira execução o modelo será baixado.")
    # Pass FFmpeg to the child process without altering the user's PATH permanently.
    process = subprocess.Popen(
        [python, "-m", "demucs", "-n", model, "-o", str(destination), str(source)],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, encoding="utf-8",
        errors="replace", bufsize=1, env=_ffmpeg_env()
    )
    assert process.stdout is not None
    for line in process.stdout:
        if line.strip():
            log(line.strip())
    code = process.wait()
    if code:
        raise RuntimeError(f"Demucs falhou (código {code}). Confira o registro de execução.")
    folder = destination / model / source.stem
    tracks = {name: str(folder / (name + ".wav")) for name in STEMS
              if (folder / (name + ".wav")).is_file()}
    if not tracks:
        raise RuntimeError("A separação terminou, mas nenhum arquivo WAV foi encontrado.")
    log(f"Separação concluída: {len(tracks)} faixas em {folder}")
    return tracks
