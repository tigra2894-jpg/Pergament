# Numele claselor raman neschimbate: rapoartele de eroare din EcranEroare
# arata numele exceptiei, iar urmele din Play Console raman citibile.
-dontobfuscate

# PDFBox isi incarca fonturile si filtrele dupa nume; il pastram intreg.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
