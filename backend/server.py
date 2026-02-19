import json
import sqlite3
import urllib.request
from urllib.parse import urlparse, parse_qs

from fastapi import FastAPI, Form
from fastapi.responses import HTMLResponse, RedirectResponse

app = FastAPI()
DB_PATH = "movies.db"


# ---------------------------------------------------------------------------
# Database
# ---------------------------------------------------------------------------

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS movies (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            title         TEXT    NOT NULL,
            thumbnail_url TEXT    NOT NULL DEFAULT '',
            stream_url    TEXT    NOT NULL
        )
    """)
    conn.commit()
    conn.close()


init_db()


# ---------------------------------------------------------------------------
# URL resolver — converts share-page links to direct stream URLs
# ---------------------------------------------------------------------------

def resolve_pcloud_url(url: str) -> str:
    """Convert a pCloud share page URL to a direct download URL via pCloud API."""
    if "pcloud.link/publink/show" not in url:
        return url

    code = parse_qs(urlparse(url).query).get("code", [None])[0]
    if not code:
        return url

    try:
        api_url = f"https://api.pcloud.com/getpublinkdownload?code={code}&forcedownload=0"
        with urllib.request.urlopen(api_url, timeout=8) as resp:
            data = json.loads(resp.read())
        if data.get("result") == 0:
            host = data["hosts"][0]
            path = data["path"]
            return f"https://{host}{path}"
    except Exception:
        pass

    return url  # fall back to original if resolution fails


def resolve_stream_url(url: str) -> str:
    """Entry point — add more cloud providers here as needed."""
    return resolve_pcloud_url(url)


# ---------------------------------------------------------------------------
# Android API
# ---------------------------------------------------------------------------

@app.get("/api/movies")
def api_get_movies():
    conn = get_db()
    rows = conn.execute("SELECT * FROM movies ORDER BY title").fetchall()
    conn.close()
    return [
        {
            "id": str(row["id"]),
            "title": row["title"],
            "thumbnail_url": row["thumbnail_url"],
            "stream_url": resolve_stream_url(row["stream_url"]),
        }
        for row in rows
    ]


# ---------------------------------------------------------------------------
# Admin UI
# ---------------------------------------------------------------------------

def _render_admin(movies, error: str = ""):
    rows_html = "".join(
        f"""
        <tr>
            <td>{m['title']}</td>
            <td><img src="{m['thumbnail_url']}" height="50" onerror="this.style.display='none'"></td>
            <td style="word-break:break-all;max-width:260px">{m['stream_url']}</td>
            <td>
                <form method="post" action="/admin/delete/{m['id']}" onsubmit="return confirm('Delete {m['title']}?')">
                    <button type="submit">Delete</button>
                </form>
            </td>
        </tr>"""
        for m in movies
    )

    error_html = f'<p style="color:#ff6b6b">{error}</p>' if error else ""

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Movie Night — Admin</title>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    background: #141414; color: #eee;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    padding: 32px;
  }}
  h1 {{ color: #e50914; margin-bottom: 28px; font-size: 1.8rem; }}
  h2 {{ color: #ccc; margin-bottom: 16px; font-size: 1.1rem; }}

  /* Add form */
  .add-form {{
    background: #1f1f1f; border-radius: 10px;
    padding: 24px; max-width: 600px; margin-bottom: 40px;
  }}
  .field {{ margin-bottom: 14px; }}
  label {{ display: block; font-size: 0.85rem; color: #aaa; margin-bottom: 4px; }}
  input[type=text] {{
    width: 100%; padding: 10px 12px;
    background: #2a2a2a; border: 1px solid #444; border-radius: 6px;
    color: #eee; font-size: 0.95rem;
  }}
  input[type=text]:focus {{ outline: none; border-color: #e50914; }}
  button[type=submit] {{
    background: #e50914; color: #fff; border: none;
    padding: 10px 24px; border-radius: 6px;
    font-size: 0.95rem; cursor: pointer; margin-top: 6px;
  }}
  button[type=submit]:hover {{ background: #c40812; }}

  /* Movies table */
  table {{
    width: 100%; border-collapse: collapse;
    background: #1f1f1f; border-radius: 10px; overflow: hidden;
  }}
  th {{
    background: #2a2a2a; text-align: left;
    padding: 12px 16px; font-size: 0.85rem; color: #aaa; font-weight: 500;
  }}
  td {{ padding: 12px 16px; border-top: 1px solid #2a2a2a; vertical-align: middle; }}
  tr:hover td {{ background: #252525; }}
  table button {{ background: #333; color: #ff6b6b; border: 1px solid #444;
    padding: 5px 12px; border-radius: 5px; cursor: pointer; font-size: 0.85rem; }}
  table button:hover {{ background: #c40812; color: #fff; border-color: #c40812; }}
  .empty {{ color: #666; padding: 24px 16px; }}
</style>
</head>
<body>

<h1>Movie Night — Admin</h1>

<div class="add-form">
  <h2>Add Movie</h2>
  {error_html}
  <form method="post" action="/admin/add">
    <div class="field">
      <label>Title</label>
      <input type="text" name="title" placeholder="Inception" required>
    </div>
    <div class="field">
      <label>Thumbnail URL</label>
      <input type="text" name="thumbnail_url" placeholder="https://...">
    </div>
    <div class="field">
      <label>Movie URL</label>
      <input type="text" name="stream_url" placeholder="https://..." required>
    </div>
    <button type="submit">Add Movie</button>
  </form>
</div>

<h2>{len(movies)} movie{"s" if len(movies) != 1 else ""} in library</h2>
<br>
<table>
  <thead>
    <tr>
      <th>Title</th><th>Thumbnail</th><th>Stream URL</th><th></th>
    </tr>
  </thead>
  <tbody>
    {"".join([rows_html]) if movies else '<tr><td colspan="4" class="empty">No movies yet. Add one above.</td></tr>'}
  </tbody>
</table>

</body>
</html>"""


@app.get("/", response_class=HTMLResponse)
def preview_page():
    conn = get_db()
    movies = [dict(r) for r in conn.execute("SELECT * FROM movies ORDER BY title").fetchall()]
    conn.close()

    cards_html = "".join(
        f"""
        <div class="card">
          <div class="poster">
            <img src="{m['thumbnail_url']}" alt="{m['title']}"
                 onerror="this.parentElement.style.background='#2a2a2a'">
          </div>
          <p class="title">{m['title']}</p>
        </div>"""
        for m in movies
    ) or '<p class="empty">No movies yet. <a href="/admin">Add some</a>.</p>'

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Movie Night</title>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    background: #141414; color: #eee;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    padding: 32px;
  }}
  header {{
    display: flex; align-items: center; justify-content: space-between;
    margin-bottom: 32px;
  }}
  header h1 {{ color: #e50914; font-size: 1.8rem; }}
  header a {{
    color: #aaa; text-decoration: none; font-size: 0.9rem;
    border: 1px solid #444; padding: 6px 14px; border-radius: 6px;
  }}
  header a:hover {{ border-color: #e50914; color: #e50914; }}
  .grid {{
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 20px;
  }}
  .card {{ cursor: default; }}
  .poster {{
    width: 100%; aspect-ratio: 2/3;
    background: #1f1f1f; border-radius: 8px; overflow: hidden;
    border: 2px solid transparent; transition: border-color 0.15s;
  }}
  .card:hover .poster {{ border-color: #fff; }}
  .poster img {{
    width: 100%; height: 100%; object-fit: cover; display: block;
  }}
  .title {{
    margin-top: 8px; font-size: 0.85rem; color: #ccc;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }}
  .empty {{ color: #666; font-size: 1rem; }}
  .empty a {{ color: #e50914; }}
</style>
</head>
<body>
<header>
  <h1>Movie Night</h1>
  <a href="/admin">Admin</a>
</header>
<div class="grid">
  {cards_html}
</div>
</body>
</html>"""


@app.get("/admin", response_class=HTMLResponse)
def admin_page():
    conn = get_db()
    movies = [dict(r) for r in conn.execute("SELECT * FROM movies ORDER BY title").fetchall()]
    conn.close()
    return _render_admin(movies)


@app.post("/admin/add")
def admin_add(
    title: str = Form(...),
    thumbnail_url: str = Form(""),
    stream_url: str = Form(...),
):
    title = title.strip()
    stream_url = stream_url.strip()
    thumbnail_url = thumbnail_url.strip()

    conn = get_db()
    conn.execute(
        "INSERT INTO movies (title, thumbnail_url, stream_url) VALUES (?, ?, ?)",
        (title, thumbnail_url, stream_url),
    )
    conn.commit()
    conn.close()
    return RedirectResponse("/admin", status_code=303)


@app.post("/admin/delete/{movie_id}")
def admin_delete(movie_id: int):
    conn = get_db()
    conn.execute("DELETE FROM movies WHERE id = ?", (movie_id,))
    conn.commit()
    conn.close()
    return RedirectResponse("/admin", status_code=303)
