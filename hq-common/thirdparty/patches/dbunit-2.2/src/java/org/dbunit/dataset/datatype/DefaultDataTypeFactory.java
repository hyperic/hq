Index: src/src/java/org/dbunit/dataset/datatype/DefaultDataTypeFactory.java
===================================================================
--- src/src/java/org/dbunit/dataset/datatype/DefaultDataTypeFactory.java        (revision 546)
+++ src/src/java/org/dbunit/dataset/datatype/DefaultDataTypeFactory.java        (working copy)
@@ -46,12 +46,15 @@
             {
                 return DataType.BLOB;
             }
-
             // CLOB
-            if ("CLOB".equals(sqlTypeName))
+            else if ("CLOB".equals(sqlTypeName))
             {
                 return DataType.CLOB;
             }
+            else
+            {
+                dataType = DataType.forSqlTypeName(sqlTypeName);
+            }
         }
         return dataType;
     }
