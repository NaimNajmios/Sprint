package com.najmi.sprint.ui.project

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.repository.ProjectDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val projectDocumentRepository: ProjectDocumentRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val documentId: String = savedStateHandle.get<String>("documentId") ?: ""
    private val projectId: String = savedStateHandle.get<String>("projectId") ?: ""

    private val _htmlContent = MutableStateFlow<String?>(null)
    val htmlContent: StateFlow<String?> = _htmlContent.asStateFlow()

    private val _documentTitle = MutableStateFlow("Loading...")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            // Find the document by ID from the project's list
            // Since we only have observeDocumentsForProject, we'll get the list and find it
            val docs = projectDocumentRepository.observeDocumentsForProject(projectId).firstOrNull()
            val doc = docs?.find { it.id == documentId }
            
            if (doc != null) {
                _documentTitle.value = doc.title
                
                val rawMarkdown = withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(doc.uri)
                        context.contentResolver.openInputStream(uri)?.use { 
                            it.bufferedReader().readText() 
                        } ?: "Error: Could not open document."
                    } catch (e: Exception) {
                        "Error reading file: ${e.message}\n\nMake sure the file still exists and permissions were granted."
                    }
                }
                
                _htmlContent.value = buildHtmlTemplate(rawMarkdown)
            } else {
                _documentTitle.value = "Error"
                _htmlContent.value = buildHtmlTemplate("# Document not found")
            }
        }
    }

    private fun buildHtmlTemplate(markdown: String): String {
        // Escaping backticks, newlines, and quotes for injecting into JS
        val escapedMarkdown = markdown
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
            
            <style>
                :root {
                    --bg-color: #121212;
                    --text-color: #E0E0E0;
                    --link-color: #bb86fc;
                    --border-color: #333333;
                    --code-bg: #1e1e1e;
                    --callout-bg: #1A1A1A;
                    --callout-border: #444;
                }

                @media (prefers-color-scheme: light) {
                    :root {
                        --bg-color: #FFFFFF;
                        --text-color: #212121;
                        --link-color: #6200EE;
                        --border-color: #E0E0E0;
                        --code-bg: #F5F5F5;
                        --callout-bg: #F9F9F9;
                        --callout-border: #E0E0E0;
                    }
                }

                body {
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    background-color: var(--bg-color);
                    color: var(--text-color);
                    line-height: 1.6;
                    padding: 16px;
                    margin: 0;
                    word-wrap: break-word;
                }

                img {
                    max-width: 100%;
                    border-radius: 8px;
                }

                pre {
                    background-color: var(--code-bg);
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 14px;
                }

                code {
                    background-color: var(--code-bg);
                    padding: 2px 4px;
                    border-radius: 4px;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 0.9em;
                }

                pre code {
                    background-color: transparent;
                    padding: 0;
                }

                a {
                    color: var(--link-color);
                    text-decoration: none;
                }

                blockquote {
                    border-left: 4px solid var(--link-color);
                    margin: 0;
                    padding-left: 16px;
                    color: var(--text-color);
                    opacity: 0.8;
                }

                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin-bottom: 16px;
                }

                th, td {
                    border: 1px solid var(--border-color);
                    padding: 8px;
                    text-align: left;
                }

                th {
                    background-color: var(--code-bg);
                }

                /* Callout Styling */
                .callout {
                    border: 1px solid var(--callout-border);
                    border-left: 4px solid var(--link-color);
                    background-color: var(--callout-bg);
                    padding: 12px 16px;
                    margin: 16px 0;
                    border-radius: 4px;
                }
                .callout-title {
                    font-weight: bold;
                    margin-bottom: 4px;
                    display: flex;
                    align-items: center;
                    text-transform: capitalize;
                }
                .callout-content {
                    margin-top: 8px;
                }
                .callout-content > p:first-child {
                    margin-top: 0;
                }
                .callout-content > p:last-child {
                    margin-bottom: 0;
                }
                
                /* Specific Callout Types */
                .callout[data-callout="info"] { border-left-color: #2196F3; }
                .callout[data-callout="note"] { border-left-color: #2196F3; }
                .callout[data-callout="warning"] { border-left-color: #FF9800; }
                .callout[data-callout="danger"] { border-left-color: #F44336; }
                .callout[data-callout="error"] { border-left-color: #F44336; }
                .callout[data-callout="success"] { border-left-color: #4CAF50; }
                .callout[data-callout="tip"] { border-left-color: #00BCD4; }
                .callout[data-callout="abstract"] { border-left-color: #00BCD4; }
            </style>
        </head>
        <body>
            <div id="content"></div>

            <script>
                // 1. Initialize Mermaid
                mermaid.initialize({ startOnLoad: false, theme: (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'default' });

                // 2. Custom Extension for Obsidian Callouts
                const calloutExtension = {
                    name: 'callout',
                    level: 'block',
                    start(src) { return src.match(/^> \[\![a-zA-Z]+\]/)?.index; },
                    tokenizer(src, tokens) {
                        const rule = /^> \[\!([a-zA-Z]+)\](.*?)\n((?:> .*\n?)*)/;
                        const match = rule.exec(src);
                        if (match) {
                            const rawContent = match[3].replace(/^> ?/gm, '');
                            const token = {
                                type: 'callout',
                                raw: match[0],
                                calloutType: match[1].toLowerCase(),
                                title: match[2].trim() || match[1],
                                text: rawContent,
                                tokens: []
                            };
                            this.lexer.blockTokens(token.text, token.tokens);
                            return token;
                        }
                    },
                    renderer(token) {
                        const contentHtml = this.parser.parse(token.tokens);
                        return `
                            <div class="callout" data-callout="${"$"}{token.calloutType}">
                                <div class="callout-title">${"$"}{token.title}</div>
                                <div class="callout-content">${"$"}{contentHtml}</div>
                            </div>
                        `;
                    }
                };

                // Add callout extension to marked
                marked.use({ extensions: [calloutExtension] });

                // 3. Process the Markdown
                const rawMarkdown = `${"$"}{escapedMarkdown}`;
                
                // Pre-process mermaid blocks so marked doesn't format them as standard code
                const renderer = new marked.Renderer();
                const originalCodeRenderer = renderer.code.bind(renderer);
                renderer.code = function(code, language, isEscaped) {
                    if (language === 'mermaid') {
                        return '<div class="mermaid">' + code + '</div>';
                    }
                    return originalCodeRenderer(code, language, isEscaped);
                };
                
                marked.use({ renderer });

                // Parse and inject
                document.getElementById('content').innerHTML = marked.parse(rawMarkdown);

                // 4. Render mermaid diagrams
                setTimeout(() => {
                    mermaid.run({ querySelector: '.mermaid' });
                }, 100);
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
