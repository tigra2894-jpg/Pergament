package ro.pergament

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.pergament.data.Import
import ro.pergament.reader.Cititor

class TextTest {

    @Test
    fun codurileNumericeDevinLitere() {
        assertEquals("ș ț ă â", Cititor.decodeazaEntitati("&#537; &#539; &#x103; &#226;"))
        assertEquals("It’s…", Cititor.decodeazaEntitati("It&#8217;s&#8230;"))
        assertEquals("„Ce-o fi”", Cititor.decodeazaEntitati("&#8222;Ce-o fi&#x201D;"))
    }

    @Test
    fun codurileCuNumeDevinLitere() {
        assertEquals("când — été €", Cititor.decodeazaEntitati("c&acirc;nd &mdash; &eacute;t&eacute; &euro;"))
        assertEquals("Amintiri & povești", Cititor.decodeazaEntitati("Amintiri &amp; povești"))
    }

    @Test
    fun ampersandDubluNuSeDecodeazaDeDouaOri() {
        assertEquals("&#39;", Cititor.decodeazaEntitati("&amp;#39;"))
    }

    @Test
    fun textulObisnuitRamaneNeatins() {
        val t = "Tom & Jerry; 5 &lt 6 &necunoscut; &#;"
        assertEquals(t, Cititor.decodeazaEntitati(t))
    }

    @Test
    fun formateAcceptate() {
        listOf("carte.pdf", "Carte.EPUB", "note.txt", "readme.md", "benzi.cbz", "benzi.zip", "fara_extensie")
            .forEach { assertTrue(it, Import.formatAcceptat(it)) }
        listOf("document.docx", "carte.mobi", "carte.fb2", "poza.jpg", "carte.azw3")
            .forEach { assertFalse(it, Import.formatAcceptat(it)) }
    }
}
