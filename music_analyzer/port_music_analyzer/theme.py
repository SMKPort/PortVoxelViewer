"""Identidade visual preto, vermelho e dourado."""
STYLE = """
QWidget { background:#0E1016; color:#F2F0EC; font:13px 'Segoe UI'; }
QLabel#heading { font-size:23px; font-weight:800; color:#E9AC45; }
QLabel#secondary { color:#A5A5B1; }
QTabWidget::pane { border:1px solid #33313A; background:#11131A; }
QTabBar::tab { padding:14px 22px; background:#171922; color:#A8A7B1; }
QTabBar::tab:selected { color:#E9AC45; border-bottom:2px solid #D44A4A; }
QFrame#card { border:1px solid #38343D; border-radius:12px; background:#191B23; }
QFrame#card QLabel, QFrame#card QCheckBox { background:transparent; }
QPushButton { background:#292B35; color:#F5F3EE; border:1px solid #3B3D48;
              border-radius:7px; padding:11px 15px; font-weight:600; }
QPushButton:hover { background:#373946; }
QPushButton#primary { color:#160E0A; background:#E9AC45; border-color:#E9AC45; }
QPushButton#primary:hover { background:#FFC36A; }
QComboBox, QTextEdit { padding:8px; border:1px solid #3B3D48; border-radius:7px;
                       background:#1F212A; color:#F0EDE7; }
QComboBox QAbstractItemView { background:#1F212A; }
QSlider::groove:horizontal { height:6px; background:#3A3B45; border-radius:3px; }
QSlider::handle:horizontal { background:#E9AC45; width:14px; margin:-5px 0; border-radius:7px; }
QProgressBar { border:0; background:#272932; height:9px; border-radius:4px; }
QProgressBar::chunk { background:#D44A4A; }
QScrollArea { border:0; background:#11131A; }
"""
