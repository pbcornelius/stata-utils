* Based on:
*! excelcol 1.0.0 19jul2014
*! by Sergiy Radyakin
*! 0c1941df-aa73-48d8-a39e-0e02f55445df
* Replaces the custom Mata function ExcelColumn() with Stata's built-in numtobase26(). ExcelColumn() didn't seem to run on Unix-based systems, but numtobase26() does.

program define excelcol, rclass
   version 15
   syntax anything
   confirm number `anything'
   
   mata st_local("result", numtobase26(`anything'))
   return local column `"`result'"'
end
