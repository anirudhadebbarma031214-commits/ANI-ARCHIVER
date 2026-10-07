package com.anistudio.archiver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class FileItem(val name: String, val uri: Uri, val isDir: Boolean)

class MainActivity : ComponentActivity() {
    private var currentTree: Uri? = null
    private var fileItems by mutableStateOf<List<FileItem>>(emptyList())

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            currentTree = uri
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            loadTree(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AniArchiverApp() }
    }

    private fun loadTree(tree: Uri) {
        val result = mutableListOf<FileItem>()
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        contentResolver.query(children, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ), null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC")?.use { c ->
            val id = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val name = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mime = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (c.moveToNext()) {
                val childId = c.getString(id)
                result.add(FileItem(
                    c.getString(name),
                    DocumentsContract.buildDocumentUriUsingTree(tree, childId),
                    c.getString(mime) == DocumentsContract.Document.MIME_TYPE_DIR
                ))
            }
        }
        fileItems = result
    }

    private fun createZip(item: FileItem) {
        val parent = currentTree ?: return
        val name = item.name.substringBeforeLast('.') + ".zip"
        val target = DocumentsContract.createDocument(contentResolver, parent, "application/zip", name) ?: return
        contentResolver.openOutputStream(target)?.use { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip -> addToZip(item, zip, "") }
        }
        loadTree(parent)
    }

    private fun addToZip(item: FileItem, zip: ZipOutputStream, path: String) {
        if (item.isDir) {
            val docId = DocumentsContract.getDocumentId(item.uri)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(item.uri, docId)
            contentResolver.query(children, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null)?.use { c ->
                val id = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val name = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mime = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(item.uri, c.getString(id))
                    addToZip(FileItem(c.getString(name), uri, c.getString(mime) == DocumentsContract.Document.MIME_TYPE_DIR), zip, path + item.name + "/")
                }
            }
        } else {
            zip.putNextEntry(ZipEntry(path + item.name))
            contentResolver.openInputStream(item.uri)?.use { BufferedInputStream(it).copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun extractZip(item: FileItem) {
        val parent = currentTree ?: return
        val folder = DocumentsContract.createDocumentDirectory(contentResolver, parent, item.name.removeSuffix(".zip") + "_extracted") ?: return
        contentResolver.openInputStream(item.uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val safe = entry.name.replace("\\", "/").trimStart('/')
                    if (!safe.split('/').any { it == ".." }) {
                        val parts = safe.split('/').filter { it.isNotEmpty() }
                        if (parts.isNotEmpty()) {
                            var dir = folder
                            for (part in parts.dropLast(if (entry.isDirectory) 0 else 1)) dir = findOrCreateDir(dir, part)
                            if (entry.isDirectory) {
                                findOrCreateDir(dir, parts.last())
                            } else {
                                val out = DocumentsContract.createDocument(contentResolver, dir, "application/octet-stream", parts.last())
                                if (out != null) contentResolver.openOutputStream(out)?.use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        loadTree(parent)
    }

    private fun findOrCreateDir(parent: Uri, name: String): Uri {
        val id = DocumentsContract.getDocumentId(parent)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, id)
        contentResolver.query(children, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ), null, null, null)?.use { c ->
            val i = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val n = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val m = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (c.moveToNext()) if (c.getString(n) == name && c.getString(m) == DocumentsContract.Document.MIME_TYPE_DIR)
                return DocumentsContract.buildDocumentUriUsingTree(parent, c.getString(i))
        }
        return DocumentsContract.createDocumentDirectory(contentResolver, parent, name)!!
    }

    @Composable
    private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color.White.copy(.12f), Color(0xFF6E8CFF).copy(.07f), Color.White.copy(.035f))), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(24.dp))
                .padding(18.dp), content = content
        )
    }

    @Composable
    private fun AniArchiverApp() {
        var status by remember { mutableStateOf("Choose a folder to begin") }
        MaterialTheme(colorScheme = darkColorScheme(
            primary = Color(0xFF8C9EFF), secondary = Color(0xFFB388FF),
            background = Color(0xFF05060A), surface = Color(0xFF10131C)
        )) {
            Column(Modifier.fillMaxSize().background(Color(0xFF05060A)).padding(16.dp)) {
                Text("ANI ARCHIVER", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("YOUR FILES. YOUR ARCHIVES.", fontSize = 11.sp, color = Color(0xFFAAB4D4))
                Spacer(Modifier.height(16.dp))
                GlassCard {
                    Text("STORAGE", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(status, color = Color(0xFFC2C8DC))
                    Spacer(Modifier.height(11.dp))
                    Button(onClick = { pickFolder.launch(null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Text("OPEN FOLDER")
                    }
                }
                Spacer(Modifier.height(14.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fileItems) { item ->
                        Row(
                            Modifier.fillMaxWidth().background(Color.White.copy(.06f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(16.dp)).padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (item.isDir) "📁" else "📄", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(item.name, color = Color.White, modifier = Modifier.weight(1f))
                            if (!item.isDir && item.name.endsWith(".zip", true)) {
                                TextButton(onClick = { status = "Extracting…"; extractZip(item); status = "Extraction complete" }) { Text("EXTRACT") }
                            } else {
                                TextButton(onClick = { status = "Creating ZIP…"; createZip(item); status = "Archive created" }) { Text("ZIP") }
                            }
                        }
                    }
                }
                Text("Created by ANIRUDDHA DEBBARMA • ANI STUDIO", color = Color(0xFF697188), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
