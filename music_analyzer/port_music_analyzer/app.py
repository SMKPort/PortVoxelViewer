"""Port Music Analyzer: interface desktop PySide6."""
import sys
from pathlib import Path
from PySide6.QtCore import Qt, QThread, QTimer, QUrl, Signal
from PySide6.QtGui import QDesktopServices, QPainter, QColor, QPen
from PySide6.QtMultimedia import QMediaPlayer, QAudioOutput
from PySide6.QtWidgets import (QApplication, QMainWindow, QWidget, QLabel, QPushButton,
    QVBoxLayout, QHBoxLayout, QTabWidget, QFileDialog, QComboBox, QSlider,
    QCheckBox, QScrollArea, QTextEdit, QMessageBox, QFrame, QProgressBar)
from .audio_core import MixerEngine, analyze_track, format_time
from .ai_runtime import install_ai, ai_python, separate, STEMS
from .theme import STYLE

NAMES = {"vocals":"Voz", "drums":"Bateria", "bass":"Baixo", "guitar":"Guitarra",
         "piano":"Piano", "other":"Outros"}
COLORS = ("#E9AC45", "#D44A4A", "#9A83E1", "#4AC7A4", "#7CA8F2", "#CCCCCC")


class Job(QThread):
    success = Signal(object)
    failure = Signal(str)
    message = Signal(str)

    def __init__(self, fn, *args):
        super().__init__()
        self.fn, self.args = fn, args

    def run(self):
        try:
            self.success.emit(self.fn(*self.args, log=self.message.emit))
        except Exception as exc:
            self.failure.emit(f"{type(exc).__name__}: {exc}")


class Wave(QWidget):
    def __init__(self):
        super().__init__()
        self.peaks = []
        self.setMinimumHeight(125)

    def paintEvent(self, event):
        p = QPainter(self)
        p.fillRect(self.rect(), QColor("#171922"))
        middle = self.height() // 2
        if not self.peaks:
            p.setPen(QColor("#9D9BA7"))
            p.drawText(self.rect(), Qt.AlignCenter, "A forma de onda aparecerá aqui")
            return
        p.setPen(QPen(QColor("#E9AC45"), 2))
        for index, peak in enumerate(self.peaks):
            x = 6 + round(index * (self.width() - 12) / max(1, len(self.peaks)-1))
            h = max(1, round(peak * (middle - 8)))
            p.drawLine(x, middle-h, x, middle+h)


def button(label, callback, primary=False):
    b = QPushButton(label)
    if primary:
        b.setObjectName("primary")
    b.clicked.connect(callback)
    return b


def card():
    frame = QFrame()
    frame.setObjectName("card")
    layout = QVBoxLayout(frame)
    layout.setContentsMargins(18, 18, 18, 18)
    return frame, layout


def heading(text):
    result = QLabel(text)
    result.setObjectName("heading")
    return result


class Window(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Port Music Analyzer 1.0")
        self.resize(1080, 760)
        self.setMinimumSize(780, 570)
        self.song = None
        self.stems = {}
        self.output = str(Path.home() / "Music" / "PortMusicAnalyzer")
        self.mixer = MixerEngine()
        self.job = None
        self.player = QMediaPlayer(self)
        self.audio = QAudioOutput(self)
        self.audio.setVolume(0.8)
        self.player.setAudioOutput(self.audio)
        self.setStyleSheet(STYLE)
        self.setup()
        self.timer = QTimer(self)
        self.timer.timeout.connect(self.refresh)
        self.timer.start(250)

    def setup(self):
        root = QWidget()
        self.setCentralWidget(root)
        layout = QVBoxLayout(root)
        layout.setContentsMargins(22, 18, 22, 18)
        banner = QHBoxLayout()
        banner.addWidget(heading("◈  PORT MUSIC ANALYZER"))
        banner.addStretch()
        self.filename = QLabel("Nenhuma música")
        self.filename.setObjectName("secondary")
        banner.addWidget(self.filename)
        banner.addWidget(button("Abrir música", self.choose_song, True))
        layout.addLayout(banner)
        self.tabs = QTabWidget()
        layout.addWidget(self.tabs, 1)
        self.tab_analysis()
        self.tab_separation()
        self.tab_mixer()
        self.tab_settings()
        footer = QHBoxLayout()
        self.status = QLabel("Pronto.")
        self.status.setObjectName("secondary")
        footer.addWidget(self.status, 1)
        self.busy = QProgressBar()
        self.busy.setRange(0, 0)
        self.busy.setMaximumWidth(140)
        self.busy.hide()
        footer.addWidget(self.busy)
        layout.addLayout(footer)

    def tab_analysis(self):
        page = QWidget()
        v = QVBoxLayout(page)
        v.addWidget(heading("Análise de BPM e tonalidade"))
        v.addWidget(QLabel("Estimativas automáticas. O tom pode mudar ao longo da música."))
        row = QHBoxLayout()
        self.metrics = {}
        for key, caption in (("bpm","BPM"), ("key","TONALIDADE PROVÁVEL"), ("duration","DURAÇÃO")):
            box, content = card()
            content.addWidget(QLabel(caption))
            value = heading("—")
            content.addWidget(value)
            row.addWidget(box)
            self.metrics[key] = value
        v.addLayout(row)
        waveform, w = card()
        w.addWidget(QLabel("FORMA DE ONDA"))
        self.wave = Wave()
        w.addWidget(self.wave)
        v.addWidget(waveform)
        actions = QHBoxLayout()
        actions.addWidget(button("▶ Ouvir original", self.play_original))
        actions.addWidget(button("⏸ Pausar original", self.player.pause))
        actions.addStretch()
        actions.addWidget(button("Analisar agora", self.analyze, True))
        v.addLayout(actions)
        self.note = QLabel("Selecione uma música para começar.")
        self.note.setObjectName("secondary")
        v.addWidget(self.note)
        v.addStretch()
        self.tabs.addTab(page, "01  Análise")

    def tab_separation(self):
        page = QWidget()
        v = QVBoxLayout(page)
        v.addWidget(heading("Separação de instrumentos por IA"))
        box, content = card()
        content.addWidget(QLabel("Selecione o modelo de separação"))
        self.model = QComboBox()
        self.model.addItem("6 faixas: voz, bateria, baixo, guitarra, piano e outros", "htdemucs_6s")
        self.model.addItem("4 faixas: voz, bateria, baixo e outros", "htdemucs")
        self.model.addItem("4 faixas: modelo refinado", "htdemucs_ft")
        content.addWidget(self.model)
        self.destination = QLabel(self.output)
        self.destination.setWordWrap(True)
        self.destination.setObjectName("secondary")
        content.addWidget(self.destination)
        controls = QHBoxLayout()
        controls.addWidget(button("Pasta de saída", self.choose_output))
        controls.addWidget(button("Separar música", self.start_separation, True))
        controls.addWidget(button("Importar stems WAV", self.load_folder))
        content.addLayout(controls)
        v.addWidget(box)
        v.addWidget(QLabel("Guitarra e piano são separações experimentais. O modelo será baixado na primeira execução."))
        self.log = QTextEdit()
        self.log.setReadOnly(True)
        self.log.setPlaceholderText("Registro de instalação e separação...")
        v.addWidget(self.log, 1)
        self.tabs.addTab(page, "02  Separação IA")

    def tab_mixer(self):
        page = QWidget()
        v = QVBoxLayout(page)
        v.addWidget(heading("Mixer por instrumento"))
        v.addWidget(QLabel("Volume até 200%, Mudo e Solo. A saída é limitada para evitar clipping digital."))
        self.track_host = QWidget()
        self.tracks_layout = QVBoxLayout(self.track_host)
        self.tracks_layout.addWidget(QLabel("Nenhuma faixa carregada."))
        self.tracks_layout.addStretch()
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setWidget(self.track_host)
        v.addWidget(scroll, 1)
        box, content = card()
        controls = QHBoxLayout()
        controls.addWidget(button("▶ Reproduzir", self.play_mixer, True))
        controls.addWidget(button("⏸ Pausar", self.mixer.pause))
        controls.addWidget(button("↺ Reiniciar", lambda: self.mixer.seek(0)))
        self.time = QLabel("00:00 / 00:00")
        controls.addStretch()
        controls.addWidget(self.time)
        content.addLayout(controls)
        self.seek = QSlider(Qt.Horizontal)
        self.seek.setRange(0, 1000)
        self.seek.sliderReleased.connect(self.seek_mixer)
        content.addWidget(self.seek)
        controls2 = QHBoxLayout()
        controls2.addWidget(button("Exportar mixagem WAV / FLAC", self.export_mix))
        controls2.addWidget(button("Abrir pasta", self.open_stems))
        content.addLayout(controls2)
        v.addWidget(box)
        self.tabs.addTab(page, "03  Mixer")

    def tab_settings(self):
        page = QWidget()
        v = QVBoxLayout(page)
        v.addWidget(heading("Configurações da IA"))
        box, content = card()
        self.ai_state = QLabel("Verificando instalação...")
        self.ai_state.setWordWrap(True)
        content.addWidget(self.ai_state)
        content.addWidget(button("Instalar / reparar motor Demucs (CPU)", self.install, True))
        v.addWidget(box)
        text = QLabel("Requer Python 3.11 de 64 bits instalado no Windows. "
                      "A instalação e o primeiro modelo precisam de internet. "
                      "Após o download, o processamento roda localmente. "
                      "O aplicativo não captura nem separa áudio do Spotify. "
                      "BPM, tonalidade e isolamento de instrumentos são aproximados.")
        text.setWordWrap(True)
        v.addWidget(text)
        v.addStretch()
        self.tabs.addTab(page, "04  Configurações")
        self.update_ai()

    def choose_song(self):
        path, _ = QFileDialog.getOpenFileName(self, "Abrir música", str(Path.home()),
                                             "Áudio (*.wav *.mp3 *.flac *.ogg)")
        if path:
            self.song = path
            self.filename.setText(Path(path).name)
            self.player.setSource(QUrl.fromLocalFile(path))
            self.analyze()

    def choose_output(self):
        path = QFileDialog.getExistingDirectory(self, "Pasta de saída", self.output)
        if path:
            self.output = path
            self.destination.setText(path)

    def run_job(self, fn, args, on_success):
        if self.job and self.job.isRunning():
            QMessageBox.information(self, "Port Music Analyzer", "Uma operação já está em andamento.")
            return
        job = Job(fn, *args)
        self.job = job
        self.busy.show()
        self.status.setText("Processando...")
        job.message.connect(self.message)
        job.failure.connect(self.error)
        job.success.connect(on_success)
        job.finished.connect(self.finished)
        job.start()

    def message(self, text):
        text = str(text)
        self.log.append(text[-600:])
        self.status.setText(text[-120:])

    def error(self, text):
        self.log.append("ERRO: " + text)
        self.status.setText("Falha na operação.")
        QMessageBox.warning(self, "Port Music Analyzer", text)

    def finished(self):
        self.busy.hide()
        if self.job:
            self.job.deleteLater()
            self.job = None

    def analyze(self):
        if not self.song:
            QMessageBox.information(self, "Port Music Analyzer", "Abra uma música primeiro.")
            return
        self.run_job(lambda file, log: analyze_track(file), [self.song], self.analysis_ready)

    def analysis_ready(self, data):
        self.metrics["bpm"].setText(f'{data["bpm"]:.1f}' if data["bpm"] else "Indefinido")
        self.metrics["key"].setText(data["key"])
        self.metrics["duration"].setText(format_time(data["duration"]))
        self.wave.peaks = data["peaks"]
        self.wave.update()
        self.note.setText("Analisados até 120 segundos para estimar ritmo e harmonia.")
        self.status.setText("Análise concluída.")

    def play_original(self):
        self.mixer.pause()
        self.player.play()

    def install(self):
        self.tabs.setCurrentIndex(1)
        self.run_job(install_ai, [], lambda msg: (self.message(msg), self.update_ai()))

    def update_ai(self):
        try:
            self.ai_state.setText("Motor IA instalado: " + ai_python())
        except RuntimeError:
            self.ai_state.setText("Motor IA não instalado. Instale Python 3.11 e clique em instalar.")

    def start_separation(self):
        if not self.song:
            QMessageBox.information(self, "Port Music Analyzer", "Abra uma música primeiro.")
            return
        try:
            ai_python()
        except RuntimeError as exc:
            self.tabs.setCurrentIndex(3)
            QMessageBox.information(self, "Motor IA", str(exc))
            return
        self.player.pause()
        self.mixer.pause()
        self.run_job(separate, [self.song, self.output, self.model.currentData()], self.load_stems)

    def load_folder(self):
        folder = QFileDialog.getExistingDirectory(self, "Pasta contendo os arquivos WAV", self.output)
        if folder:
            stems = {name: str(Path(folder) / (name + ".wav")) for name in STEMS
                     if (Path(folder) / (name + ".wav")).is_file()}
            if not stems:
                QMessageBox.warning(self, "Port Music Analyzer", "Nenhum stem conhecido encontrado.")
            else:
                self.load_stems(stems)

    def load_stems(self, stems):
        try:
            self.mixer.load(stems)
        except Exception as exc:
            QMessageBox.warning(self, "Port Music Analyzer", str(exc))
            return
        self.stems = stems
        while self.tracks_layout.count():
            item = self.tracks_layout.takeAt(0)
            if item.widget():
                item.widget().deleteLater()
        for i, name in enumerate(stems):
            box, content = card()
            row = QHBoxLayout()
            label = QLabel(NAMES.get(name, name))
            label.setStyleSheet(f"font-weight: bold; color: {COLORS[i % len(COLORS)]}")
            label.setMinimumWidth(90)
            row.addWidget(label)
            volume = QSlider(Qt.Horizontal)
            volume.setRange(0, 200)
            volume.setValue(100)
            volume.valueChanged.connect(lambda value, track=name:
                                        self.mixer.set_track(track, volume=value/100))
            row.addWidget(volume, 1)
            percent = QLabel("100%")
            volume.valueChanged.connect(lambda value, lbl=percent: lbl.setText(f"{value}%"))
            row.addWidget(percent)
            mute = QCheckBox("Mudo")
            mute.toggled.connect(lambda enabled, track=name:
                                 self.mixer.set_track(track, muted=enabled))
            row.addWidget(mute)
            solo = QCheckBox("Solo")
            solo.toggled.connect(lambda enabled, track=name:
                                 self.mixer.set_track(track, solo=enabled))
            row.addWidget(solo)
            content.addLayout(row)
            self.tracks_layout.addWidget(box)
        self.tracks_layout.addStretch()
        self.tabs.setCurrentIndex(2)
        self.status.setText(f"{len(stems)} faixas no mixer.")

    def play_mixer(self):
        self.player.pause()
        try:
            self.mixer.play()
        except Exception as exc:
            QMessageBox.warning(self, "Port Music Analyzer", str(exc))

    def refresh(self):
        duration, position = self.mixer.duration, self.mixer.position
        self.time.setText(f"{format_time(position)} / {format_time(duration)}")
        if duration and not self.seek.isSliderDown():
            self.seek.setValue(round(1000*position/duration))

    def seek_mixer(self):
        self.mixer.seek(self.mixer.duration*self.seek.value()/1000)

    def export_mix(self):
        if not self.stems:
            QMessageBox.information(self, "Port Music Analyzer", "Importe faixas primeiro.")
            return
        path, _ = QFileDialog.getSaveFileName(self, "Exportar mixagem",
                    str(Path(self.output)/"port_mix.wav"), "WAV (*.wav);;FLAC (*.flac)")
        if path:
            self.run_job(self.mixer.export_mix, [path],
                         lambda result: self.status.setText("Exportado: " + result))

    def open_stems(self):
        if self.stems:
            QDesktopServices.openUrl(QUrl.fromLocalFile(
                str(Path(next(iter(self.stems.values()))).parent)))

    def closeEvent(self, event):
        if self.job and self.job.isRunning():
            QMessageBox.information(self, "Port Music Analyzer",
                                    "Finalize a operação antes de fechar.")
            event.ignore()
            return
        self.player.stop()
        self.mixer.close()
        event.accept()


def main():
    app = QApplication(sys.argv)
    app.setApplicationName("Port Music Analyzer")
    window = Window()
    window.show()
    return app.exec()
