## 2026-02-21 - [Fixing JS Injection in WebView Bridge]
**Learning:** Using single-quote interpolation to pass JSON strings from Java to JavaScript via `evaluateJavascript` is vulnerable to injection if the data contains single quotes.
**Action:** Always use `gson.toJson(String)` to properly escape the JSON string as a safe JavaScript string literal.
