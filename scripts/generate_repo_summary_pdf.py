from __future__ import annotations

import subprocess
import sys
from pathlib import Path
from typing import Iterable

import pdfplumber
import pypdfium2 as pdfium
from pypdf import PdfReader
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "output" / "pdf"
TMP_DIR = ROOT / "tmp" / "pdfs" / "assistant-demo-repo-summary"
PDF_PATH = OUTPUT_DIR / "assistant-demo-repo-summary.pdf"
PNG_PREFIX = TMP_DIR / "assistant-demo-repo-summary"
PNG_PATH = TMP_DIR / "assistant-demo-repo-summary-1.png"

PAGE_WIDTH, PAGE_HEIGHT = A4
MARGIN_X = 42
TOP_MARGIN = 40
BOTTOM_MARGIN = 34
GUTTER = 18
COLUMN_WIDTH = (PAGE_WIDTH - (MARGIN_X * 2) - GUTTER) / 2
LEFT_X = MARGIN_X
RIGHT_X = MARGIN_X + COLUMN_WIDTH + GUTTER

TITLE_COLOR = HexColor("#16324f")
ACCENT_COLOR = HexColor("#3b6ea8")
TEXT_COLOR = HexColor("#1d2733")
MUTED_COLOR = HexColor("#5c6b7a")
RULE_COLOR = HexColor("#c9d6e3")
PANEL_FILL = HexColor("#f6f9fc")

TITLE_FONT = "Helvetica-Bold"
BODY_FONT = "Helvetica"
BODY_BOLD = "Helvetica-Bold"

TITLE_SIZE = 18
SUBTITLE_SIZE = 9.4
SECTION_SIZE = 11.2
BODY_SIZE = 9.1
SMALL_SIZE = 8.7
LEADING = 11.1

WHAT_IT_IS = (
    "assistant-demo is a multi-module Spring Boot repository for exploring Spring AI "
    "patterns across RAG, chat memory, tool calling, and Model Context Protocol (MCP) "
    "client-server examples."
)

WHAT_IT_IS_2 = (
    "Repo evidence points to the `rag` module as the most complete app, while `springai` "
    "and the MCP modules act as focused demos for memory, streaming, tool approval, and transport variants."
)

WHO_ITS_FOR = (
    "Primary persona: backend and AI engineers evaluating Spring AI integration patterns. "
    "Repo docs also show the `rag` module targeting support teams, internal support engineers, "
    "API consumers, and app-assistant scenarios."
)

FEATURES = [
    "RAG query flow with references and confidence fields in the `rag` API response.",
    "Persona and channel-aware prompt assembly driven by a dynamic prompt template.",
    "Chat history and memory demos spanning JDBC persistence and in-memory conversation state.",
    "Tool-calling examples, including manual `ToolCallingManager` loops and approval-gated flows.",
    "MCP client and server samples covering stdio, SSE, HTTPS SSE, and streamable HTTP transports.",
    "Project docs, SQL guides, Make targets, and kanban notes that support setup and iteration.",
]

HOW_IT_WORKS = [
    "Caller -> a Spring Boot module (`rag`, `springai`, or `mcp/*`) -> controller entrypoint.",
    "`rag` preprocesses the query, retrieves from pgvector, optionally reranks, builds the prompt, then asks the chat model for the final answer.",
    "`springai` focuses on chat, memory, streaming, and tool execution patterns around `ChatClient`, `OpenAiChatModel`, and approval flows.",
    "MCP client modules talk to local MCP servers over configured transports; MCP servers expose weather or trade-style tool services.",
    "Storage and external services are optional by module: PostgreSQL + pgvector for `rag`, MySQL/JDBC memory for `springai`, and HTTP/SSE or streamable links for MCP demos.",
]

RUN_STEPS = [
    "Install JDK 17+, Maven, Python, and PostgreSQL with pgvector for the `rag` module.",
    "Main app: from repo root run `mvn -pl rag spring-boot:run`, or inside `rag` use `make run`.",
    "Optional MCP demo: run `mvn -pl mcp/mcp-server-streamable spring-boot:run`, then `mvn -pl mcp/mcp-client spring-boot:run`.",
    "Single command to run the entire repo: Not found in repo.",
]

REQUIRED_HEADINGS = [
    "What it is",
    "Who it's for",
    "What it does",
    "How it works",
    "How to run",
]


def ensure_dirs() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    TMP_DIR.mkdir(parents=True, exist_ok=True)


def wrap_text(text: str, width: float, font_name: str = BODY_FONT, font_size: float = BODY_SIZE) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if stringWidth(candidate, font_name, font_size) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_lines(pdf: canvas.Canvas, x: float, y: float, lines: Iterable[str], font_name: str = BODY_FONT,
               font_size: float = BODY_SIZE, color=TEXT_COLOR) -> float:
    pdf.setFont(font_name, font_size)
    pdf.setFillColor(color)
    for line in lines:
        pdf.drawString(x, y, line)
        y -= LEADING
    return y


def draw_section_title(pdf: canvas.Canvas, x: float, y: float, title: str) -> float:
    pdf.setFont(BODY_BOLD, SECTION_SIZE)
    pdf.setFillColor(ACCENT_COLOR)
    pdf.drawString(x, y, title)
    y -= 5
    pdf.setStrokeColor(RULE_COLOR)
    pdf.setLineWidth(0.8)
    pdf.line(x, y, x + COLUMN_WIDTH, y)
    return y - 12


def draw_bullets(pdf: canvas.Canvas, x: float, y: float, bullets: list[str], width: float) -> float:
    bullet_indent = 10
    text_width = width - bullet_indent
    pdf.setFillColor(TEXT_COLOR)
    for bullet in bullets:
        lines = wrap_text(bullet, text_width)
        pdf.setFont(BODY_BOLD, BODY_SIZE)
        pdf.drawString(x, y, "-")
        pdf.setFont(BODY_FONT, BODY_SIZE)
        for line in lines:
            pdf.drawString(x + bullet_indent, y, line)
            y -= LEADING
        y -= 2
    return y


def create_pdf() -> None:
    ensure_dirs()
    pdf = canvas.Canvas(str(PDF_PATH), pagesize=A4)
    pdf.setTitle("assistant-demo repo summary")
    pdf.setAuthor("OpenAI Codex")

    pdf.setFillColor(TITLE_COLOR)
    pdf.setFont(TITLE_FONT, TITLE_SIZE)
    pdf.drawString(MARGIN_X, PAGE_HEIGHT - TOP_MARGIN, "assistant-demo")

    pdf.setFillColor(MUTED_COLOR)
    pdf.setFont(BODY_BOLD, SUBTITLE_SIZE)
    pdf.drawString(MARGIN_X, PAGE_HEIGHT - TOP_MARGIN - 15, "One-page repo summary: Spring AI + MCP demo suite")

    panel_y = PAGE_HEIGHT - TOP_MARGIN - 28
    panel_h = 14
    pdf.setFillColor(PANEL_FILL)
    pdf.roundRect(MARGIN_X, panel_y - panel_h + 2, PAGE_WIDTH - (MARGIN_X * 2), panel_h, 5, fill=1, stroke=0)
    pdf.setFillColor(MUTED_COLOR)
    pdf.setFont(BODY_FONT, SMALL_SIZE)
    pdf.drawString(
        MARGIN_X + 8,
        panel_y - 8,
        "Evidence source: root Maven modules, `rag` docs/config, representative `springai` controllers/config, and MCP configs/source.",
    )

    left_y = PAGE_HEIGHT - TOP_MARGIN - 60
    right_y = left_y

    left_y = draw_section_title(pdf, LEFT_X, left_y, "What it is")
    left_y = draw_lines(pdf, LEFT_X, left_y, wrap_text(WHAT_IT_IS, COLUMN_WIDTH))
    left_y -= 3
    left_y = draw_lines(pdf, LEFT_X, left_y, wrap_text(WHAT_IT_IS_2, COLUMN_WIDTH))
    left_y -= 8

    left_y = draw_section_title(pdf, LEFT_X, left_y, "Who it's for")
    left_y = draw_lines(pdf, LEFT_X, left_y, wrap_text(WHO_ITS_FOR, COLUMN_WIDTH))
    left_y -= 8

    left_y = draw_section_title(pdf, LEFT_X, left_y, "What it does")
    left_y = draw_bullets(pdf, LEFT_X, left_y, FEATURES, COLUMN_WIDTH)

    right_y = draw_section_title(pdf, RIGHT_X, right_y, "How it works")
    right_y = draw_bullets(pdf, RIGHT_X, right_y, HOW_IT_WORKS, COLUMN_WIDTH)
    right_y -= 4

    right_y = draw_section_title(pdf, RIGHT_X, right_y, "How to run")
    right_y = draw_bullets(pdf, RIGHT_X, right_y, RUN_STEPS, COLUMN_WIDTH)

    footer_y = BOTTOM_MARGIN
    pdf.setStrokeColor(RULE_COLOR)
    pdf.setLineWidth(0.8)
    pdf.line(MARGIN_X, footer_y + 10, PAGE_WIDTH - MARGIN_X, footer_y + 10)
    pdf.setFillColor(MUTED_COLOR)
    pdf.setFont(BODY_FONT, 8.2)
    pdf.drawString(
        MARGIN_X,
        footer_y,
        "Not found in repo is used where a repo-wide command or unified runtime path was not documented.",
    )

    pdf.showPage()
    pdf.save()


def validate_pdf() -> None:
    reader = PdfReader(str(PDF_PATH))
    if len(reader.pages) != 1:
        raise RuntimeError(f"Expected exactly 1 page, found {len(reader.pages)}.")

    extracted = "\n".join((page.extract_text() or "") for page in reader.pages)
    for heading in REQUIRED_HEADINGS:
        if heading not in extracted:
            raise RuntimeError(f"Missing required heading in extracted text: {heading}")

    feature_section = extracted.split("What it does", 1)[1].split("How it works", 1)[0]
    feature_hits = feature_section.count("\n-")
    if feature_hits != len(FEATURES):
        raise RuntimeError(f"Expected {len(FEATURES)} feature bullets, found {feature_hits} in extracted text.")

    with pdfplumber.open(str(PDF_PATH)) as pdf_doc:
        first_page = pdf_doc.pages[0]
        text = first_page.extract_text() or ""
        if "Single command to run the entire repo: Not found in repo." not in text:
            raise RuntimeError("Missing explicit 'Not found in repo' runtime note.")


def render_png() -> Path:
    pdftoppm = locate_pdftoppm()
    if pdftoppm is not None:
        subprocess.run(
            [str(pdftoppm), "-png", str(PDF_PATH), str(PNG_PREFIX)],
            check=True,
            cwd=str(ROOT),
        )
        candidate = TMP_DIR / "assistant-demo-repo-summary-1.png"
        if candidate.exists():
            return candidate

    pdf = pdfium.PdfDocument(str(PDF_PATH))
    page = pdf[0]
    bitmap = page.render(scale=2.0)
    image = bitmap.to_pil()
    image.save(PNG_PATH)
    page.close()
    pdf.close()
    return PNG_PATH


def locate_pdftoppm() -> Path | None:
    for item in (
        Path(r"C:\Users\Administrator\AppData\Local\UniGetUI\Chocolatey\bin\pdftoppm.exe"),
        Path(r"C:\Program Files\poppler\Library\bin\pdftoppm.exe"),
        Path(r"C:\Program Files\poppler\bin\pdftoppm.exe"),
    ):
        if item.exists():
            return item
    return None


def main() -> int:
    create_pdf()
    validate_pdf()
    png_path = render_png()
    print(f"PDF generated: {PDF_PATH}")
    print(f"PNG rendered: {png_path}")
    if locate_pdftoppm() is None:
        print("Renderer note: pdftoppm was unavailable; used pypdfium2 fallback for PNG validation.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
