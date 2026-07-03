package com.deepawasthi.URLShortener.controller;

import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminController {

    private final UrlService urlService;

    public AdminController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping(value = "/admin", produces = MediaType.TEXT_HTML_VALUE)
    public String adminPage() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>URL Shortener Admin</title>
                    <style>
                        :root { color-scheme: light dark; font-family: Inter, system-ui, sans-serif; }
                        body { margin: 0; background: #f6f8fb; color: #17202a; }
                        main { max-width: 1200px; margin: 0 auto; padding: 32px 20px; }
                        header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
                        h1 { margin: 0; font-size: clamp(1.6rem, 4vw, 2.4rem); }
                        button, input { border: 1px solid #c8d1dc; border-radius: 6px; font: inherit; min-height: 40px; }
                        input { padding: 0 12px; min-width: min(280px, 100%); }
                        button { background: #1264a3; color: #fff; padding: 0 14px; cursor: pointer; }
                        .toolbar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
                        .status { margin: 12px 0 18px; color: #52616f; }
                        .table-wrap { overflow-x: auto; border: 1px solid #d8e0ea; border-radius: 8px; background: #fff; }
                        table { border-collapse: collapse; width: 100%; min-width: 900px; }
                        th, td { text-align: left; padding: 12px 14px; border-bottom: 1px solid #edf1f5; vertical-align: top; }
                        th { background: #eef4f8; color: #334155; font-size: .82rem; text-transform: uppercase; }
                        td { font-size: .94rem; }
                        a { color: #0b67a3; word-break: break-all; }
                        .empty { padding: 28px; text-align: center; color: #52616f; }
                        .expired { color: #a33d16; font-weight: 700; }
                        .active { color: #146c43; font-weight: 700; }
                        @media (prefers-color-scheme: dark) {
                            body { background: #111827; color: #e5e7eb; }
                            .table-wrap { background: #172033; border-color: #2d3a4f; }
                            th { background: #1f2a3d; color: #d7dee8; }
                            th, td { border-bottom-color: #28364a; }
                            input { background: #111827; color: #e5e7eb; border-color: #44546a; }
                            .status, .empty { color: #aab7c7; }
                            a { color: #77c7ff; }
                        }
                    </style>
                </head>
                <body>
                    <main>
                        <header>
                            <h1>URL Shortener Admin</h1>
                            <div class="toolbar">
                                <input id="token" type="password" autocomplete="current-password" placeholder="Admin token">
                                <button id="saveToken" type="button">Save Token</button>
                                <button id="refresh" type="button">Refresh</button>
                            </div>
                        </header>
                        <p class="status" id="status">Loading URLs...</p>
                        <div class="table-wrap" id="tableWrap"></div>
                    </main>
                    <script>
                        const statusNode = document.querySelector("#status");
                        const tableWrap = document.querySelector("#tableWrap");
                        const tokenInput = document.querySelector("#token");
                        const params = new URLSearchParams(window.location.search);
                        tokenInput.value = params.get("token") || sessionStorage.getItem("adminToken") || "";

                        document.querySelector("#saveToken").addEventListener("click", () => {
                            sessionStorage.setItem("adminToken", tokenInput.value);
                            loadUrls();
                        });
                        document.querySelector("#refresh").addEventListener("click", loadUrls);

                        function formatDate(v) { return v ? new Date(v).toLocaleString() : ""; }
                        function shortUrl(code) { return `${window.location.origin}/${code}`; }
                        function esc(v) {
                            return String(v ?? "").replaceAll("&","&amp;").replaceAll("<","&lt;")
                                .replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#039;");
                        }

                        function renderRows(urls) {
                            if (!urls.length) {
                                tableWrap.innerHTML = '<div class="empty">No URLs yet.</div>';
                                return;
                            }
                            const rows = urls.map(u => {
                                const expired = new Date(u.expirationDate) < new Date();
                                return `<tr>
                                    <td>${esc(u.id)}</td>
                                    <td><a href="${shortUrl(u.shortCode)}" target="_blank">${esc(u.shortCode)}</a></td>
                                    <td><a href="${esc(u.originalUrl)}" target="_blank">${esc(u.originalUrl)}</a></td>
                                    <td>${formatDate(u.creationDate)}</td>
                                    <td>${formatDate(u.expirationDate)}</td>
                                    <td>${u.clickCount}</td>
                                    <td class="${expired?"expired":"active"}">${expired?"Expired":"Active"}</td>
                                </tr>`;
                            }).join("");
                            tableWrap.innerHTML = `<table>
                                <thead><tr><th>ID</th><th>Short Code</th><th>Original URL</th><th>Created</th><th>Expires</th><th>Clicks</th><th>Status</th></tr></thead>
                                <tbody>${rows}</tbody>
                            </table>`;
                        }

                        async function loadUrls() {
                            statusNode.textContent = "Loading...";
                            const token = tokenInput.value.trim();
                            const headers = token ? { "Authorization": `Bearer ${token}` } : {};
                            try {
                                const r = await fetch("/api/urls", { headers });
                                if (r.status === 401) { statusNode.textContent = "Unauthorized. Enter a valid JWT token."; tableWrap.innerHTML = ""; return; }
                                if (!r.ok) { statusNode.textContent = "Unable to load URLs."; return; }
                                const urls = await r.json();
                                statusNode.textContent = `${urls.length} URL${urls.length===1?"":"s"} stored`;
                                renderRows(urls);
                            } catch(e) { statusNode.textContent = "Error loading URLs."; }
                        }
                        loadUrls();
                    </script>
                </body>
                </html>
                """;
    }

    @GetMapping("/admin/urls")
    public ResponseEntity<List<Url>> urls() {
        return ResponseEntity.ok(urlService.getAllUrls());
    }
}
