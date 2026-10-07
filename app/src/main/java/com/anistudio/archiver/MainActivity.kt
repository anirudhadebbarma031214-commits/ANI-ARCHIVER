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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

data class FileItem(val name:String,val uri:Uri,val isDir:Boolean)

class MainActivity:ComponentActivity(){
 private var root:Uri?=null
 private var items by mutableStateOf<List<FileItem>>(emptyList())
 private var path by mutableStateOf("Storage")
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){u->
  if(u!=null){root=u;contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION);load(u,"Storage")}
 }
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}
 private fun load(tree:Uri,label:String){
  path=label
  val id=DocumentsContract.getTreeDocumentId(tree)
  val q=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id)
  val out=mutableListOf<FileItem>()
  contentResolver.query(q,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,DocumentsContract.Document.COLUMN_DISPLAY_NAME+" ASC")?.use{c->
   val i=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);val n=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);val m=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
   while(c.moveToNext())out+=FileItem(c.getString(n),DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(i)),c.getString(m)==DocumentsContract.Document.MIME_TYPE_DIR)
  };items=out
 }
 private fun zip(item:FileItem){
  val p=root?:return
  val t=DocumentsContract.createDocument(contentResolver,p,"application/zip",item.name.substringBeforeLast(".")+".zip")?:return
  contentResolver.openOutputStream(t)?.use{ZipOutputStream(BufferedOutputStream(it)).use{z->addZip(item,z,"")}}
  load(p,path)
 }
 private fun addZip(x:FileItem,z:ZipOutputStream,prefix:String){
  if(x.isDir){
   val id=DocumentsContract.getDocumentId(x.uri);val q=DocumentsContract.buildChildDocumentsUriUsingTree(x.uri,id)
   contentResolver.query(q,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use{c->
    val i=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);val n=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);val m=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
    while(c.moveToNext())addZip(FileItem(c.getString(n),DocumentsContract.buildDocumentUriUsingTree(x.uri,c.getString(i)),c.getString(m)==DocumentsContract.Document.MIME_TYPE_DIR),z,prefix+x.name+"/")
   }
  }else{z.putNextEntry(ZipEntry(prefix+x.name));contentResolver.openInputStream(x.uri)?.use{BufferedInputStream(it).copyTo(z)};z.closeEntry()}
 }
 private fun extract(x:FileItem){
  val p=root?:return;val folder=DocumentsContract.createDocumentDirectory(contentResolver,p,x.name.removeSuffix(".zip")+"_extracted")?:return
  contentResolver.openInputStream(x.uri)?.use{ZipInputStream(BufferedInputStream(it)).use{z->
   var e=z.nextEntry
   while(e!=null){val safe=e.name.replace("\\","/").trimStart('/');if(!safe.split('/').any{it==".."}){val a=safe.split('/').filter{it.isNotEmpty()};if(a.isNotEmpty()){var d=folder;for(v in a.dropLast(if(e.isDirectory)0 else 1))d=findDir(d,v);if(e.isDirectory)findDir(d,a.last()) else DocumentsContract.createDocument(contentResolver,d,"application/octet-stream",a.last())?.let{u->contentResolver.openOutputStream(u)?.use{z.copyTo(it)}}}};z.closeEntry();e=z.nextEntry}
  }}
  load(p,path)
 }
 private fun findDir(p:Uri,n:String):Uri{
  val id=DocumentsContract.getDocumentId(p);val q=DocumentsContract.buildChildDocumentsUriUsingTree(p,id)
  contentResolver.query(q,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use{c->
   val i=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);val nn=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);val m=c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
   while(c.moveToNext())if(c.getString(nn)==n&&c.getString(m)==DocumentsContract.Document.MIME_TYPE_DIR)return DocumentsContract.buildDocumentUriUsingTree(p,c.getString(i))
  };return DocumentsContract.createDocumentDirectory(contentResolver,p,n)!!
 }
 @Composable private fun App(){
  var status by remember{mutableStateOf("Select a storage folder to begin")}
  MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF8EA0FF),secondary=Color(0xFFB77CFF),background=Color(0xFF05060A),surface=Color(0xFF111522))){
   Column(Modifier.fillMaxSize().background(Color(0xFF05060A)).padding(16.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.FolderZip,null,tint=Color(0xFF9AA8FF),modifier=Modifier.size(40.dp));Spacer(Modifier.width(10.dp));Column{Text("ANI ARCHIVER",fontSize=28.sp,fontWeight=FontWeight.Black,color=Color.White);Text("YOUR FILES. YOUR SPACE.",fontSize=10.sp,color=Color(0xFFAAB4D4))}}
    Spacer(Modifier.height(16.dp))
    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(.07f)),modifier=Modifier.fillMaxWidth().border(1.dp,Color.White.copy(.14f),RoundedCornerShape(24.dp))){
     Column(Modifier.padding(18.dp)){Text(path,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(status,color=Color(0xFFB9C0D6),fontSize=12.sp);Spacer(Modifier.height(10.dp));Button(onClick={picker.launch(null)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Icon(Icons.Default.FolderOpen,null);Spacer(Modifier.width(8.dp));Text("OPEN STORAGE")}}
    }
    Spacer(Modifier.height(12.dp))
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
     items(items){x->Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(.055f)),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(x.isDir)Icons.Default.Folder else Icons.Default.InsertDriveFile,null,tint=if(x.isDir)Color(0xFF9AA8FF) else Color(0xFFB8BFDA),modifier=Modifier.size(28.dp));Spacer(Modifier.width(10.dp));Text(x.name,color=Color.White,modifier=Modifier.weight(1f),maxLines=1);if(!x.isDir&&x.name.endsWith(".zip",true))TextButton(onClick={status="Extracting…";extract(x);status="Extraction complete"}){Text("EXTRACT")}else TextButton(onClick={status="Creating ZIP…";zip(x);status="ZIP created"}){Text("ZIP")}}}}
    }
    Text("ANI ARCHIVER  •  Created by ANIRUDDHA DEBBARMA  •  ANI STUDIO",fontSize=9.sp,color=Color(0xFF687087),modifier=Modifier.align(Alignment.CenterHorizontally))
   }
  }
 }
}